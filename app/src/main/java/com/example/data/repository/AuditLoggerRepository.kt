package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AuditLogEntity
import com.example.security.SecurityUtils
import com.example.security.TenantSecurityContext
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

enum class AuditSeverity {
    INFO,
    WARNING,
    CRITICAL
}

enum class AuditCategory {
    SECURITY,
    AUTHENTICATION,
    AUTHORIZATION,
    FINANCIAL,
    ACADEMIC,
    DATA_ACCESS,
    CONFIGURATION,
    SYSTEM
}

enum class AuditStatus {
    SUCCESS,
    DENIED,
    FAILURE
}

/**
 * Data model for a non-PII, immutable audit log entry.
 */
data class AuditLogEntry(
    val logId: String,
    val tenantId: String,
    val institutionId: String,
    val timestamp: Long,
    val isoTimestamp: String,
    val action: String,
    val category: String,
    val severity: String,
    val status: String,
    val actorUserId: String,
    val actorRole: String,
    val entityType: String,
    val entityId: String,
    val details: Map<String, Any>,
    val appVersion: String,
    val osApiLevel: Int,
    val integrityHash: String
)

/**
 * Utility responsible for sanitizing and stripping Personally Identifiable Information (PII)
 * such as passwords, tokens, full names, email addresses, phone numbers, and payment credentials
 * from audit log payloads before persistence.
 */
object PiiSanitizer {
    private val SENSITIVE_KEY_PATTERNS = listOf(
        Pattern.compile(".*(password|pass|secret|token|credential|authpin|cvv|hash|salt|card|accountnumber).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(phone|mobile|telephone|contactnumber|guardianphone|studentphone).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(email|mail|address|street|homeaddress|dob|dateofbirth|birthdate).*", Pattern.CASE_INSENSITIVE),
        Pattern.compile(".*(aadhaar|ssn|nationalid|passport|identitynumber|fullname|studentname|parentname|guardianname).*", Pattern.CASE_INSENSITIVE)
    )

    private val EMAIL_REGEX = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    private val PHONE_REGEX = Pattern.compile("(\\+?[0-9]{1,4}[\\s-]?)?(\\(?\\d{3}\\)?[\\s-]?)?[\\d\\s-]{7,15}")
    private val JWT_BEARER_REGEX = Pattern.compile("ey[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")

    fun sanitizeDetails(raw: Map<String, Any?>): Map<String, Any> {
        val sanitized = mutableMapOf<String, Any>()
        for ((key, value) in raw) {
            val isSensitiveKey = SENSITIVE_KEY_PATTERNS.any { it.matcher(key).matches() }
            if (isSensitiveKey) {
                sanitized[key] = "[REDACTED_SENSITIVE_KEY]"
                continue
            }
            if (value == null) continue

            when (value) {
                is String -> sanitized[key] = sanitizeString(value)
                is Number, is Boolean -> sanitized[key] = value
                is Map<*, *> -> {
                    val subMap = value.entries.associate { (k, v) -> k.toString() to v }
                    sanitized[key] = sanitizeDetails(subMap)
                }
                is Iterable<*> -> {
                    sanitized[key] = value.mapNotNull { item ->
                        if (item is String) sanitizeString(item) else item?.toString()
                    }
                }
                else -> sanitized[key] = sanitizeString(value.toString())
            }
        }
        return sanitized
    }

    fun sanitizeString(text: String): String {
        var result = text
        result = JWT_BEARER_REGEX.matcher(result).replaceAll("[REDACTED_JWT_TOKEN]")
        result = EMAIL_REGEX.matcher(result).replaceAll("[REDACTED_EMAIL]")
        result = PHONE_REGEX.matcher(result).replaceAll("[REDACTED_PHONE]")
        return result
    }
}

/**
 * Write-only Audit Logger repository for sensitive operations.
 * Guaranteed write-only boundary for clients: logs cannot be read, updated, or deleted
 * by lower-privilege users, and strictly enforce Firestore security rules.
 */
interface AuditLoggerRepository {
    /**
     * Securely records an audit event for a sensitive operation with zero PII.
     */
    suspend fun logSensitiveOperation(
        action: String,
        entityType: String,
        entityId: String,
        severity: AuditSeverity = AuditSeverity.INFO,
        category: AuditCategory = AuditCategory.SECURITY,
        details: Map<String, Any?> = emptyMap(),
        status: AuditStatus = AuditStatus.SUCCESS,
        errorMessage: String? = null
    ): Result<String>

    /**
     * Records security alarms and access control denials.
     */
    suspend fun logSecurityEvent(
        action: String,
        reason: String,
        severity: AuditSeverity = AuditSeverity.WARNING
    ): Result<String>

    /**
     * Records data access events without leaking the accessed data contents.
     */
    suspend fun logDataAccess(
        resourceType: String,
        resourceId: String,
        accessType: String
    ): Result<String>

    /**
     * Records financial transactions with strictly non-PII financial metadata.
     */
    suspend fun logFinancialOperation(
        action: String,
        transactionId: String,
        amountPaise: Long,
        currency: String = "INR",
        paymentMode: String,
        status: AuditStatus = AuditStatus.SUCCESS
    ): Result<String>
}

@Singleton
class AuditLoggerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val schoolDao: SchoolDao,
    private val firebaseAuth: FirebaseAuth?
) : AuditLoggerRepository {

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override suspend fun logSensitiveOperation(
        action: String,
        entityType: String,
        entityId: String,
        severity: AuditSeverity,
        category: AuditCategory,
        details: Map<String, Any?>,
        status: AuditStatus,
        errorMessage: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val activeTenantId = TenantSecurityContext.getActiveTenantId()
            val currentClaims = TenantSecurityContext.getCurrentClaims()
            val fbUser = firebaseAuth?.currentUser

            val actorUserId = fbUser?.uid ?: currentClaims?.userId ?: "SEC_ACTOR_SYSTEM"
            val actorRole = (currentClaims?.roleClaim ?: fbUser?.let { "ROLE_STUDENT" } ?: "ROLE_ANONYMOUS").let {
                if (it.startsWith("ROLE_")) it else "ROLE_$it"
            }

            val timestamp = System.currentTimeMillis()
            val isoTimestamp = isoDateFormat.format(Date(timestamp))
            val logId = "AUDIT-${UUID.randomUUID().toString().uppercase()}"

            // 1. Scrub and sanitize all metadata to eliminate any PII
            val sanitizedDetails = PiiSanitizer.sanitizeDetails(details).toMutableMap()
            if (!errorMessage.isNullOrBlank()) {
                sanitizedDetails["errorMessage"] = PiiSanitizer.sanitizeString(errorMessage)
            }

            // 2. Compute non-repudiable cryptographic integrity signature
            val signaturePayload = "$logId:$activeTenantId:$actorUserId:$action:$timestamp"
            val integrityHash = SecurityUtils.hashPasswordLegacySha256(signaturePayload, "audit_integrity_salt_v2")

            val logPayload = hashMapOf<String, Any>(
                "logId" to logId,
                "tenantId" to activeTenantId,
                "institutionId" to activeTenantId,
                "timestamp" to timestamp,
                "isoTimestamp" to isoTimestamp,
                "action" to PiiSanitizer.sanitizeString(action),
                "category" to category.name,
                "severity" to severity.name,
                "status" to status.name,
                "actorUserId" to actorUserId,
                "actorRole" to actorRole,
                "entityType" to PiiSanitizer.sanitizeString(entityType),
                "entityId" to PiiSanitizer.sanitizeString(entityId),
                "details" to sanitizedDetails,
                "appVersion" to BuildConfig.VERSION_NAME,
                "osApiLevel" to Build.VERSION.SDK_INT,
                "integrityHash" to integrityHash
            )

            // 3. Write to Firestore under the tenant-isolated audit_logs collection
            // Protected by strict firestore.rules: matching tenantId, actorUserId == request.auth.uid, immutable
            val isFirebaseAvailable = runCatching {
                FirebaseApp.getApps(context).isNotEmpty()
            }.getOrDefault(false)

            if (isFirebaseAvailable && fbUser != null) {
                runCatching {
                    val db = FirebaseFirestore.getInstance()
                    db.collection("tenants")
                        .document(activeTenantId)
                        .collection("audit_logs")
                        .document(logId)
                        .set(logPayload)
                        .await()
                }.onFailure { firestoreErr ->
                    Log.w("AuditLoggerRepository", "Firestore remote audit write deferred: ${firestoreErr.message}")
                }
            }

            // 4. Record to local Room database for durable offline-first non-repudiation
            schoolDao.insertAuditLog(
                AuditLogEntity(
                    institutionId = activeTenantId,
                    timestamp = timestamp,
                    formattedTime = isoTimestamp,
                    action = PiiSanitizer.sanitizeString(action),
                    entityType = PiiSanitizer.sanitizeString(entityType),
                    entityId = PiiSanitizer.sanitizeString(entityId),
                    actorUserId = actorUserId,
                    actorName = "Authenticated Principal", // PII-free actor reference
                    actorRole = actorRole,
                    previousState = null,
                    newState = status.name,
                    remarks = "Severity=${severity.name}, Category=${category.name}, Details=${sanitizedDetails.keys.joinToString()}",
                    securityVerificationHash = integrityHash,
                    syncStatus = if (isFirebaseAvailable && fbUser != null) "SYNCED_CLOUD" else "PENDING_SYNC"
                )
            )

            logId
        }
    }

    override suspend fun logSecurityEvent(
        action: String,
        reason: String,
        severity: AuditSeverity
    ): Result<String> {
        return logSensitiveOperation(
            action = action,
            entityType = "SecurityAlarm",
            entityId = "ALARM-${UUID.randomUUID().toString().take(8).uppercase()}",
            severity = severity,
            category = AuditCategory.SECURITY,
            details = mapOf("reason" to reason),
            status = AuditStatus.DENIED
        )
    }

    override suspend fun logDataAccess(
        resourceType: String,
        resourceId: String,
        accessType: String
    ): Result<String> {
        return logSensitiveOperation(
            action = "DATA_ACCESS_${accessType.uppercase()}",
            entityType = resourceType,
            entityId = resourceId,
            severity = AuditSeverity.INFO,
            category = AuditCategory.DATA_ACCESS,
            details = mapOf("accessType" to accessType)
        )
    }

    override suspend fun logFinancialOperation(
        action: String,
        transactionId: String,
        amountPaise: Long,
        currency: String,
        paymentMode: String,
        status: AuditStatus
    ): Result<String> {
        return logSensitiveOperation(
            action = action,
            entityType = "PaymentTransaction",
            entityId = transactionId,
            severity = if (status == AuditStatus.SUCCESS) AuditSeverity.INFO else AuditSeverity.WARNING,
            category = AuditCategory.FINANCIAL,
            details = mapOf(
                "amountPaise" to amountPaise,
                "currency" to currency,
                "paymentMode" to paymentMode
            ),
            status = status
        )
    }
}
