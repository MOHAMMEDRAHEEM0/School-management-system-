package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.remote.sync.ConflictResolutionPolicy
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.remote.sync.SyncBatchResult
import com.example.security.SecureSessionManager
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

class DuplicatePaymentException(message: String) : IllegalStateException(message)
class DuplicateReceiptException(message: String) : IllegalStateException(message)

data class ManualFeePaymentRequest(
    val institutionId: String? = null,
    val studentId: String,
    val feeRecordId: String,
    val amountPaise: Long,
    val paymentMode: String,
    val transactionReference: String = "",
    val remarks: String = "",
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val customReceiptNumber: String? = null
)

data class CreateFeeAssessmentRequest(
    val institutionId: String? = null,
    val studentId: String,
    val term: String,
    val feeType: String,
    val totalAmountPaise: Long
)

data class FeeBalanceSummary(
    val feeRecord: FeeRecordEntity,
    val totalAmountPaise: Long,
    val confirmedPaidPaise: Long,
    val outstandingBalancePaise: Long,
    val confirmedTransactionCount: Int,
    val transactions: List<FeePaymentTransactionEntity>
)

data class FeePaymentReceiptResult(
    val transaction: FeePaymentTransactionEntity,
    val updatedFeeRecord: FeeRecordEntity,
    val outboxId: String,
    val outstandingBalancePaise: Long
)

/**
 * Pure domain validator for Phase D Fee Receipts and Payment integrity.
 */
object FeePaymentValidator {
    val AUTHORIZED_FINANCIAL_ROLES = setOf(
        UserRole.ACCOUNTANT,
        UserRole.PRINCIPAL,
        UserRole.ADMIN
    )

    val ALLOWED_MANUAL_PAYMENT_MODES = setOf(
        "CASH",
        "CHEQUE",
        "BANK_TRANSFER",
        "UPI_MANUAL",
        "DD"
    )

    fun validateFinancialActor(claims: SecurityClaims?) {
        if (claims == null) {
            throw SecurityException("Unauthorized: Active authenticated session is required for financial operations.")
        }
        val role = claims.role
        val hasRole = role in AUTHORIZED_FINANCIAL_ROLES
        val explicitlyPermitted = role == UserRole.ACCOUNTANT ||
            role == UserRole.PRINCIPAL ||
            role == UserRole.ADMIN
        if (!hasRole && !explicitlyPermitted && !claims.hasPermission(SchoolPermissions.MANAGE_FEE_STRUCTURE)) {
            throw SecurityException(
                "Access Denied: Role '${role.name}' is not authorized to record fee assessments or payments. Only Accountant, Principal, or Admin may perform this operation."
            )
        }
        // Explicitly deny TEACHER / INCHARGE / STUDENT / PARENT even if extra flags are present
        if (role !in AUTHORIZED_FINANCIAL_ROLES) {
            throw SecurityException(
                "Access Denied: Role '${role.name}' cannot modify institutional financial ledgers."
            )
        }
    }

    fun validateManualPaymentInput(request: ManualFeePaymentRequest) {
        if (request.studentId.isBlank()) {
            throw IllegalArgumentException("Student selection is required.")
        }
        if (request.feeRecordId.isBlank()) {
            throw IllegalArgumentException("Fee account selection is required.")
        }
        if (request.amountPaise <= 0L) {
            throw IllegalArgumentException("Payment amount must be greater than ₹0.")
        }
        if (request.idempotencyKey.isBlank()) {
            throw IllegalArgumentException("Operation idempotency key cannot be blank.")
        }
        val normalizedMode = request.paymentMode.trim().uppercase(Locale.US)
        if (normalizedMode !in ALLOWED_MANUAL_PAYMENT_MODES) {
            throw IllegalArgumentException(
                "Unsupported manual payment mode '${request.paymentMode}'. Accepted counter modes: ${ALLOWED_MANUAL_PAYMENT_MODES.joinToString(", ")}. Gateway payments require live server verification."
            )
        }
        if (normalizedMode != "CASH" && request.transactionReference.isBlank()) {
            throw IllegalArgumentException(
                "Reference number (Cheque/UTR/Transaction Ref) is required for $normalizedMode payments."
            )
        }
    }

    fun validateBalanceAndAmount(
        totalAmountPaise: Long,
        confirmedPaidPaise: Long,
        requestedPaymentPaise: Long
    ): Long {
        if (totalAmountPaise <= 0L) {
            throw IllegalStateException("Fee assessment total must be positive.")
        }
        val outstandingPaise = (totalAmountPaise - confirmedPaidPaise).coerceAtLeast(0L)
        if (outstandingPaise <= 0L) {
            throw IllegalStateException("This fee account is already fully settled (₹0 outstanding).")
        }
        if (requestedPaymentPaise > outstandingPaise) {
            throw IllegalArgumentException(
                "Payment amount (₹${requestedPaymentPaise / 100L}) exceeds persisted outstanding balance (₹${outstandingPaise / 100L})."
            )
        }
        return (outstandingPaise - requestedPaymentPaise).coerceAtLeast(0L)
    }
}

interface FeeRepository {
    fun observeFeeRecordsForTenant(institutionId: String): Flow<List<FeeRecordEntity>>
    fun observeFeeRecordsForStudent(studentId: String, institutionId: String): Flow<List<FeeRecordEntity>>
    fun observePaymentTransactionsForTenant(institutionId: String): Flow<List<FeePaymentTransactionEntity>>
    fun observePaymentTransactionsForStudent(studentId: String, institutionId: String): Flow<List<FeePaymentTransactionEntity>>
    fun observeStudentsForTenant(institutionId: String): Flow<List<StudentEntity>>
    fun searchStudentsForTenant(institutionId: String, query: String): Flow<List<StudentEntity>>

    suspend fun getFeeBalanceSummary(feeRecordId: String): Result<FeeBalanceSummary>
    suspend fun getStudentFeeSummaries(studentId: String): Result<List<FeeBalanceSummary>>
    suspend fun createFeeAssessment(request: CreateFeeAssessmentRequest): Result<FeeRecordEntity>
    suspend fun recordManualPayment(request: ManualFeePaymentRequest): Result<FeePaymentReceiptResult>
    suspend fun syncPendingFinancialOutbox(forceAll: Boolean = false): Result<SyncBatchResult>
}

@Singleton
class FeeRepositoryImpl @Inject constructor(
    private val dao: SchoolDao,
    private val outboxManager: DurableOutboxManager
) : FeeRepository {

    constructor(dao: SchoolDao) : this(
        dao = dao,
        outboxManager = DurableOutboxManager(dao)
    )

    override fun observeFeeRecordsForTenant(institutionId: String): Flow<List<FeeRecordEntity>> =
        dao.getFeeRecordsForInstitution(institutionId)

    override fun observeFeeRecordsForStudent(studentId: String, institutionId: String): Flow<List<FeeRecordEntity>> =
        dao.getFeeRecordsForStudentInTenant(studentId, institutionId)

    override fun observePaymentTransactionsForTenant(institutionId: String): Flow<List<FeePaymentTransactionEntity>> =
        dao.getPaymentTransactions(institutionId)

    override fun observePaymentTransactionsForStudent(studentId: String, institutionId: String): Flow<List<FeePaymentTransactionEntity>> =
        dao.getPaymentTransactionsForStudentInTenant(studentId, institutionId)

    override fun observeStudentsForTenant(institutionId: String): Flow<List<StudentEntity>> =
        dao.getStudentsForInstitution(institutionId)

    override fun searchStudentsForTenant(institutionId: String, query: String): Flow<List<StudentEntity>> =
        dao.searchStudentsByTenant(institutionId, query.trim())

    private fun resolveActiveClaims(): SecurityClaims? {
        return TenantSecurityContext.getCurrentClaims()
            ?: SecureSessionManager.getActiveSession()?.claims
    }

    private fun resolveAuthenticatedTenant(preferredTenantId: String? = null): String {
        val sessionTenant = TenantSecurityContext.getActiveTenantIdOrNull()
            ?: SecureSessionManager.getActiveSession()?.institutionId
            ?: throw TenantSecurityException(
                "Unauthorized operation: Verified authenticated tenant context is required."
            )

        if (!preferredTenantId.isNullOrBlank() && preferredTenantId.trim() != sessionTenant) {
            throw TenantSecurityException(
                "Tenant isolation violation: Active session belongs to '$sessionTenant', cannot access or mutate financial records for '$preferredTenantId'."
            )
        }
        return sessionTenant
    }

    override suspend fun getFeeBalanceSummary(feeRecordId: String): Result<FeeBalanceSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val activeClaims = resolveActiveClaims()
                ?: throw SecurityException("Unauthorized: Active authenticated session is required.")
            val resolvedTenant = resolveAuthenticatedTenant()

            val feeRecord = dao.getFeeRecordById(feeRecordId.trim())
                ?: throw IllegalArgumentException("Fee record '$feeRecordId' not found.")

            if (feeRecord.institutionId != resolvedTenant) {
                throw TenantSecurityException(
                    "Cross-tenant access denied: Fee record '$feeRecordId' belongs to another institution."
                )
            }

            enforceReadAccess(activeClaims, feeRecord.studentId, resolvedTenant)

            val confirmedTxns = dao.getConfirmedTransactionsForFeeRecord(feeRecord.id, resolvedTenant)
            val paidFromTxnsPaise = confirmedTxns.sumOf { it.amountPaidPaise }
            val outstandingPaise = (feeRecord.totalAmount - paidFromTxnsPaise).coerceAtLeast(0L)

            FeeBalanceSummary(
                feeRecord = feeRecord.copy(
                    paidAmount = paidFromTxnsPaise,
                    dueAmount = outstandingPaise,
                    status = computeStatus(feeRecord.totalAmount, paidFromTxnsPaise, outstandingPaise)
                ),
                totalAmountPaise = feeRecord.totalAmount,
                confirmedPaidPaise = paidFromTxnsPaise,
                outstandingBalancePaise = outstandingPaise,
                confirmedTransactionCount = confirmedTxns.size,
                transactions = confirmedTxns.sortedByDescending { it.timestamp }
            )
        }
    }

    override suspend fun getStudentFeeSummaries(studentId: String): Result<List<FeeBalanceSummary>> = withContext(Dispatchers.IO) {
        runCatching {
            val activeClaims = resolveActiveClaims()
                ?: throw SecurityException("Unauthorized: Active authenticated session is required.")
            val resolvedTenant = resolveAuthenticatedTenant()

            val student = dao.getStudentById(studentId.trim())
                ?: dao.getStudentByAdmissionNumber(studentId.trim())
                ?: throw IllegalArgumentException("Student '$studentId' not found in institution '$resolvedTenant'.")

            if (student.institutionId != resolvedTenant) {
                throw TenantSecurityException(
                    "Cross-tenant access denied: Student '${student.id}' does not belong to '$resolvedTenant'."
                )
            }

            enforceReadAccess(activeClaims, student.id, resolvedTenant)

            val records = dao.getFeeRecordsForStudentList(student.id, resolvedTenant)
            records.map { record ->
                val confirmedTxns = dao.getConfirmedTransactionsForFeeRecord(record.id, resolvedTenant)
                val paidFromTxnsPaise = confirmedTxns.sumOf { it.amountPaidPaise }
                val outstandingPaise = (record.totalAmount - paidFromTxnsPaise).coerceAtLeast(0L)
                FeeBalanceSummary(
                    feeRecord = record.copy(
                        paidAmount = paidFromTxnsPaise,
                        dueAmount = outstandingPaise,
                        status = computeStatus(record.totalAmount, paidFromTxnsPaise, outstandingPaise)
                    ),
                    totalAmountPaise = record.totalAmount,
                    confirmedPaidPaise = paidFromTxnsPaise,
                    outstandingBalancePaise = outstandingPaise,
                    confirmedTransactionCount = confirmedTxns.size,
                    transactions = confirmedTxns.sortedByDescending { it.timestamp }
                )
            }
        }
    }

    override suspend fun createFeeAssessment(request: CreateFeeAssessmentRequest): Result<FeeRecordEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val activeClaims = resolveActiveClaims()
            FeePaymentValidator.validateFinancialActor(activeClaims)
            val resolvedTenant = resolveAuthenticatedTenant(request.institutionId)

            if (request.totalAmountPaise <= 0L) {
                throw IllegalArgumentException("Fee assessment amount must be greater than ₹0.")
            }
            if (request.term.isBlank() || request.feeType.isBlank()) {
                throw IllegalArgumentException("Academic term and fee category are required.")
            }

            val student = dao.getStudentById(request.studentId.trim())
                ?: dao.getStudentByAdmissionNumber(request.studentId.trim())
                ?: throw IllegalArgumentException("Student '${request.studentId}' not found.")

            if (student.institutionId != resolvedTenant) {
                throw TenantSecurityException(
                    "Cross-tenant access denied: Student '${student.id}' belongs to '${student.institutionId}', not '$resolvedTenant'."
                )
            }

            val feeId = "FEE-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}-${UUID.randomUUID().toString().take(6).uppercase(Locale.US)}"
            val newFee = FeeRecordEntity(
                id = feeId,
                institutionId = resolvedTenant,
                studentId = student.id,
                admissionNumber = student.admissionNumber,
                studentName = student.name,
                gradeSection = "Grade ${student.grade}-${student.section}",
                term = request.term.trim(),
                feeType = request.feeType.trim(),
                totalAmount = request.totalAmountPaise,
                paidAmount = 0L,
                dueAmount = request.totalAmountPaise,
                status = "DUE",
                receiptNumber = null,
                paidDate = null,
                paymentMode = "UNPAID",
                verifiedByIncharge = false
            )

            val payload = JSONObject().apply {
                put("feeRecordId", newFee.id)
                put("institutionId", resolvedTenant)
                put("studentId", student.id)
                put("admissionNumber", student.admissionNumber)
                put("studentName", student.name)
                put("gradeSection", newFee.gradeSection)
                put("term", newFee.term)
                put("feeType", newFee.feeType)
                put("totalAmountPaise", newFee.totalAmount)
                put("paidAmountPaise", 0L)
                put("dueAmountPaise", newFee.totalAmount)
                put("status", "DUE")
                put("createdByUserId", activeClaims!!.userId)
                put("createdByRole", activeClaims.role.name)
            }.toString()

            val outboxItem = outboxManager.createOutboxEntity(
                entityType = "FEE_RECORD",
                entityId = newFee.id,
                operation = "CREATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = "FEE-ASSESS-$resolvedTenant-${newFee.id}"
            )

            val auditLog = AuditLogEntity(
                institutionId = resolvedTenant,
                action = "FEE_ASSESSMENT_CREATED",
                entityType = "FeeRecord",
                entityId = newFee.id,
                actorUserId = activeClaims!!.userId,
                actorName = activeClaims.displayName,
                actorRole = activeClaims.role.name,
                previousState = null,
                newState = "DUE (₹${newFee.totalRupees})",
                remarks = "Created fee assessment '${newFee.feeType}' (${newFee.term}) of ₹${newFee.totalRupees} for ${student.name}."
            )

            val existingRecords = dao.getFeeRecordsForStudentList(student.id, resolvedTenant) + newFee
            val totalPaidRupees = existingRecords.sumOf { record ->
                dao.getConfirmedTransactionsForFeeRecord(record.id, resolvedTenant).sumOf { it.amountPaidPaise }
            } / 100.0
            val totalDueRupees = (existingRecords.sumOf { it.totalAmount } / 100.0 - totalPaidRupees).coerceAtLeast(0.0)

            dao.createFeeAssessmentWithOutbox(
                fee = newFee,
                auditLog = auditLog,
                outboxItem = outboxItem,
                studentPaidRupees = totalPaidRupees,
                studentDueRupees = totalDueRupees
            )

            newFee
        }
    }

    override suspend fun recordManualPayment(request: ManualFeePaymentRequest): Result<FeePaymentReceiptResult> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Role Authorization Check
            val activeClaims = resolveActiveClaims()
            FeePaymentValidator.validateFinancialActor(activeClaims)

            // 2. Tenant Isolation Check
            val resolvedTenant = resolveAuthenticatedTenant(request.institutionId)

            // 3. Input Validation
            FeePaymentValidator.validateManualPaymentInput(request)
            val normalizedMode = request.paymentMode.trim().uppercase(Locale.US)
            val cleanIdempotencyKey = request.idempotencyKey.trim()

            // 4. Duplicate Payment Prevention (Idempotency Key & Outbox check)
            val existingTxnByKey = dao.getPaymentTransactionByIdempotencyKey(cleanIdempotencyKey)
            if (existingTxnByKey != null) {
                throw DuplicatePaymentException(
                    "Duplicate payment prevented: Operation key '$cleanIdempotencyKey' was already recorded under receipt '${existingTxnByKey.receiptNumber}'."
                )
            }
            val existingOutboxByKey = dao.getOutboxByIdempotencyKey(cleanIdempotencyKey)
            if (existingOutboxByKey != null) {
                throw DuplicatePaymentException(
                    "Duplicate payment prevented: Outbox submission '$cleanIdempotencyKey' is already queued."
                )
            }

            // 5. Verify Student and Fee Record Existence & Tenant Ownership
            val student = dao.getStudentById(request.studentId.trim())
                ?: dao.getStudentByAdmissionNumber(request.studentId.trim())
                ?: throw IllegalArgumentException("Student '${request.studentId}' not found in this institution.")

            if (student.institutionId != resolvedTenant) {
                throw TenantSecurityException(
                    "Cross-tenant access denied: Student '${student.id}' belongs to '${student.institutionId}', not '$resolvedTenant'."
                )
            }

            val feeRecord = dao.getFeeRecordById(request.feeRecordId.trim())
                ?: throw IllegalArgumentException("Fee record '${request.feeRecordId}' not found.")

            if (feeRecord.institutionId != resolvedTenant) {
                throw TenantSecurityException(
                    "Cross-tenant access denied: Fee record '${feeRecord.id}' belongs to '${feeRecord.institutionId}', not '$resolvedTenant'."
                )
            }

            if (feeRecord.studentId != student.id && feeRecord.admissionNumber != student.admissionNumber) {
                throw IllegalArgumentException(
                    "Integrity error: Fee record '${feeRecord.id}' belongs to '${feeRecord.studentName}', not '${student.name}'."
                )
            }

            // 6. Compute Authoritative Balance strictly from Persisted Transactions
            val confirmedTxns = dao.getConfirmedTransactionsForFeeRecord(feeRecord.id, resolvedTenant)
            val confirmedPaidPaise = confirmedTxns.sumOf { it.amountPaidPaise }
            val newDuePaise = FeePaymentValidator.validateBalanceAndAmount(
                totalAmountPaise = feeRecord.totalAmount,
                confirmedPaidPaise = confirmedPaidPaise,
                requestedPaymentPaise = request.amountPaise
            )
            val newPaidPaise = confirmedPaidPaise + request.amountPaise
            val newStatus = computeStatus(feeRecord.totalAmount, newPaidPaise, newDuePaise)

            // Guard against accidental rapid duplicate non-cash reference submission on same fee record
            val cleanRef = request.transactionReference.trim()
            if (normalizedMode != "CASH" && cleanRef.isNotBlank()) {
                val duplicateRefTxn = confirmedTxns.firstOrNull {
                    it.transactionReference.equals(cleanRef, ignoreCase = true) &&
                        it.paymentMode.equals(normalizedMode, ignoreCase = true)
                }
                if (duplicateRefTxn != null) {
                    throw DuplicatePaymentException(
                        "Duplicate payment reference '$cleanRef' already used on receipt '${duplicateRefTxn.receiptNumber}'."
                    )
                }
            }

            // 7. Generate / Validate Unique Receipt Number
            val receiptNumber = resolveUniqueReceiptNumber(
                institutionId = resolvedTenant,
                requestedReceiptNumber = request.customReceiptNumber
            )

            val now = System.currentTimeMillis()
            val transactionId = "TXN-$resolvedTenant-$now-${UUID.randomUUID().toString().take(6).uppercase(Locale.US)}"
            val effectiveRef = cleanRef.ifBlank { "COUNTER-CASH-${now.toString().takeLast(6)}" }

            val txn = FeePaymentTransactionEntity(
                transactionId = transactionId,
                institutionId = resolvedTenant,
                feeRecordId = feeRecord.id,
                studentId = student.id,
                studentName = student.name,
                amountPaidPaise = request.amountPaise,
                paymentMode = normalizedMode,
                transactionReference = effectiveRef,
                receiptNumber = receiptNumber,
                timestamp = now,
                cashierName = activeClaims!!.displayName,
                status = FeePaymentTransactionEntity.STATUS_RECORDED_MANUAL,
                recordedByUserId = activeClaims.userId,
                recordedByRole = activeClaims.role.name,
                verificationSource = FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED,
                idempotencyKey = cleanIdempotencyKey,
                syncStatus = FeePaymentTransactionEntity.SYNC_PENDING,
                isSyncedToCentral = false,
                serverAckToken = null,
                remarks = request.remarks.trim()
            )

            // 8. Build Outbox Payload (never marked synced until genuine server ack!)
            val outboxPayload = JSONObject().apply {
                put("transactionId", txn.transactionId)
                put("institutionId", resolvedTenant)
                put("tenantId", resolvedTenant)
                put("feeRecordId", feeRecord.id)
                put("studentId", student.id)
                put("admissionNumber", student.admissionNumber)
                put("studentName", student.name)
                put("amountPaidPaise", txn.amountPaidPaise)
                put("paymentMode", txn.paymentMode)
                put("transactionReference", txn.transactionReference)
                put("receiptNumber", txn.receiptNumber)
                put("timestamp", txn.timestamp)
                put("cashierName", txn.cashierName)
                put("recordedByUserId", txn.recordedByUserId)
                put("recordedByRole", txn.recordedByRole)
                put("verificationSource", FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED)
                put("status", FeePaymentTransactionEntity.STATUS_RECORDED_MANUAL)
                put("calculatedPaidAfterPaise", newPaidPaise)
                put("calculatedDueAfterPaise", newDuePaise)
                put("idempotencyKey", cleanIdempotencyKey)
                put("remarks", txn.remarks)
            }.toString()

            val outboxItem = outboxManager.createOutboxEntity(
                entityType = "FEE_PAYMENT",
                entityId = txn.transactionId,
                operation = "CREATE",
                payloadJson = outboxPayload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = cleanIdempotencyKey
            )

            // 9. Immutable Audit Trail Entry
            val auditLog = AuditLogEntity(
                institutionId = resolvedTenant,
                action = "MANUAL_FEE_PAYMENT_RECORDED",
                entityType = "FeePaymentTransaction",
                entityId = txn.transactionId,
                actorUserId = activeClaims.userId,
                actorName = activeClaims.displayName,
                actorRole = activeClaims.role.name,
                previousState = "${feeRecord.status} (Due: ₹${feeRecord.dueRupees})",
                newState = "$newStatus (Due: ₹${newDuePaise / 100L})",
                remarks = "Manual counter payment of ₹${txn.amountRupees} via ${txn.paymentMode} for ${student.name}. Receipt: $receiptNumber. Verification: MANUAL_RECORDED."
            )

            // 10. Compute Student Aggregate Financial Summary from Persisted Records + This New Payment
            val studentFeeRecords = dao.getFeeRecordsForStudentList(student.id, resolvedTenant)
            var aggregatePaidPaise = request.amountPaise
            var aggregateTotalPaise = 0L
            for (rec in studentFeeRecords) {
                aggregateTotalPaise += rec.totalAmount
                aggregatePaidPaise += dao.getConfirmedTransactionsForFeeRecord(rec.id, resolvedTenant).sumOf { it.amountPaidPaise }
            }
            val aggregateDuePaise = (aggregateTotalPaise - aggregatePaidPaise).coerceAtLeast(0L)

            // 11. Single Atomic SQLite Transaction
            dao.processFeePaymentAtomicWithOutbox(
                feeId = feeRecord.id,
                transaction = txn,
                paidPaise = newPaidPaise,
                duePaise = newDuePaise,
                status = newStatus,
                auditLog = auditLog,
                outboxItem = outboxItem,
                studentTotalPaidRupees = aggregatePaidPaise / 100.0,
                studentTotalDueRupees = aggregateDuePaise / 100.0
            )

            val paidDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
            FeePaymentReceiptResult(
                transaction = txn,
                updatedFeeRecord = feeRecord.copy(
                    paidAmount = newPaidPaise,
                    dueAmount = newDuePaise,
                    status = newStatus,
                    receiptNumber = receiptNumber,
                    paidDate = paidDate,
                    paymentMode = normalizedMode
                ),
                outboxId = outboxItem.outboxId,
                outstandingBalancePaise = newDuePaise
            )
        }
    }

    override suspend fun syncPendingFinancialOutbox(forceAll: Boolean): Result<SyncBatchResult> =
        withContext(Dispatchers.IO) {
            val resolvedTenant = resolveAuthenticatedTenant()
            outboxManager.flushQueue(institutionId = resolvedTenant, forceAll = forceAll)
        }

    private suspend fun enforceReadAccess(claims: SecurityClaims, studentId: String, tenantId: String) {
        when (claims.role) {
            UserRole.ACCOUNTANT, UserRole.PRINCIPAL, UserRole.ADMIN -> return
            UserRole.STUDENT -> {
                if (claims.userId != studentId) {
                    throw SecurityException("Access Denied: Students can only view their own fee records and receipts.")
                }
            }
            UserRole.PARENT -> {
                val linkedWards = dao.getWardsForParentList(claims.userId, tenantId)
                if (linkedWards.none { it.studentId == studentId }) {
                    throw SecurityException("Access Denied: Parent is not linked to student '$studentId'.")
                }
            }
            else -> {
                throw SecurityException(
                    "Access Denied: Role '${claims.role.name}' does not have permission to view institutional fee records."
                )
            }
        }
    }

    private suspend fun resolveUniqueReceiptNumber(
        institutionId: String,
        requestedReceiptNumber: String?
    ): String {
        val cleanCustom = requestedReceiptNumber?.trim()?.takeIf { it.isNotBlank() }
        if (cleanCustom != null) {
            if (dao.getPaymentTransactionByReceiptNumber(cleanCustom) != null) {
                throw DuplicateReceiptException("Receipt number '$cleanCustom' is already assigned to another transaction.")
            }
            return cleanCustom
        }

        val tenantPrefix = institutionId.replace("INST-", "").take(4).uppercase(Locale.US).ifBlank { "SCH" }
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        repeat(8) {
            val suffix = UUID.randomUUID().toString().replace("-", "").take(6).uppercase(Locale.US)
            val candidate = "RCP-$tenantPrefix-$dateCode-$suffix"
            if (dao.getPaymentTransactionByReceiptNumber(candidate) == null) {
                return candidate
            }
        }
        throw DuplicateReceiptException("Unable to allocate a unique receipt number after multiple attempts.")
    }

    private fun computeStatus(totalPaise: Long, paidPaise: Long, duePaise: Long): String {
        return when {
            duePaise <= 0L && paidPaise >= totalPaise -> "PAID"
            paidPaise > 0L -> "PARTIAL"
            else -> "DUE"
        }
    }
}
