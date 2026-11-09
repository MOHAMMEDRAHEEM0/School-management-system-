package com.example.security

import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import java.security.MessageDigest
import java.util.UUID

// =============================================================================
// PRODUCTION SECURITY & CRYPTOGRAPHIC UTILITIES
// =============================================================================

class TenantSecurityException(message: String) : SecurityException(message)
class RoleAuthorizationException(message: String, val requiredRole: String? = null) : SecurityException(message)
class OfflineAuthenticationException(message: String) : java.io.IOException(message)
class InvalidCredentialsException(message: String) : SecurityException(message)
class FirebaseConfigurationException(message: String) : IllegalStateException(message)

object SecurityUtils {

    private const val DEFAULT_SALT_PREFIX = "sxa_sec_v2_"
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 65536
    private const val PBKDF2_KEY_LENGTH = 256 // in bits

    /**
     * Generates a cryptographically strong pseudo-random salt using SecureRandom.
     */
    fun generateSecureSalt(): String {
        val random = java.security.SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Computes a NIST-compliant PBKDF2WithHmacSHA256 key derivation hash with 65,536 iterations
     * and cryptographic salt to prevent dictionary and rainbow table attacks.
     */
    fun hashPasswordPBKDF2(password: String, salt: String = generateSecureSalt()): String {
        val saltBytes = "$DEFAULT_SALT_PREFIX$salt".toByteArray(Charsets.UTF_8)
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
        val hash = factory.generateSecret(spec).encoded
        return "pbkdf2:$PBKDF2_ITERATIONS:${hash.joinToString("") { "%02x".format(it) }}"
    }

    /**
     * Primary secure password hashing function using PBKDF2 key derivation.
     */
    fun hashPassword(password: String, salt: String = generateSecureSalt()): String {
        return hashPasswordPBKDF2(password, salt)
    }

    /**
     * Legacy SHA-256 helper retained strictly for transparent migration of existing accounts.
     */
    fun hashPasswordLegacySha256(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = "$DEFAULT_SALT_PREFIX$salt:$password:$salt"
        val hashBytes = digest.digest(combined.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies raw password against stored cryptographic hash using constant-time comparison to prevent timing attacks.
     * Supports both modern PBKDF2 format and legacy salted SHA-256 for backward compatibility.
     * Plaintext password fallbacks are strictly prohibited and will never authenticate.
     */
    fun verifyPassword(
        rawPassword: String,
        salt: String,
        storedHash: String
    ): Boolean {
        if (storedHash.isBlank()) {
            return false
        }
        return if (storedHash.startsWith("pbkdf2:")) {
            val computed = hashPasswordPBKDF2(rawPassword, salt)
            constantTimeEquals(computed, storedHash)
        } else {
            // Legacy SHA-256 verification
            val computed = hashPasswordLegacySha256(rawPassword, salt)
            constantTimeEquals(computed, storedHash)
        }
    }

    /**
     * Constant-time string equality check to mitigate timing side-channel attacks.
     */
    fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    /**
     * Validates and sanitizes tenant ID format to prevent path traversal or injection.
     */
    fun sanitizeTenantId(tenantId: String): String {
        val sanitized = tenantId.trim().replace("[^A-Za-z0-9_-]".toRegex(), "")
        require(sanitized.isNotBlank()) { "Invalid tenant ID: must be alphanumeric, hyphen or underscore" }
        return sanitized
    }

    /**
     * Generates a secure session token.
     */
    fun generateSessionToken(userId: String, tenantId: String): String {
        val raw = "$userId:$tenantId:${System.currentTimeMillis()}:${UUID.randomUUID()}"
        val digest = MessageDigest.getInstance("SHA-256")
        val tokenHash = digest.digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return "SXA-TOK-${tokenHash.take(32).uppercase()}"
    }

    /**
     * Generates a tamper-evident signature hash for audit log records.
     */
    fun generateAuditSignature(
        actorId: String,
        institutionId: String,
        action: String,
        entityType: String,
        entityId: String,
        timestamp: Long
    ): String {
        val payload = "$institutionId|$actorId|$action|$entityType|$entityId|$timestamp"
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        return "HASH-${hash.take(16).uppercase()}"
    }
}

/**
 * Thread-safe context holder for currently authenticated user claims and tenant isolation guard.
 */
object TenantSecurityContext {

    @Volatile
    private var currentClaims: SecurityClaims? = null

    @Volatile
    private var currentTenantId: String? = null

    fun setContext(claims: SecurityClaims?, tenantId: String) {
        currentClaims = claims
        currentTenantId = tenantId.trim()
    }

    fun clearContext() {
        currentClaims = null
        currentTenantId = null
    }

    fun getActiveTenantId(): String = currentTenantId
        ?: throw TenantSecurityException("Tenant violation: No active tenant context established.")

    fun getActiveTenantIdOrNull(): String? = currentTenantId

    fun getCurrentClaims(): SecurityClaims? = currentClaims

    /**
     * Asserts that the caller has access to the target institution tenant.
     * Prevents cross-school unauthorized data reads/writes.
     */
    fun assertTenantAccess(targetInstitutionId: String) {
        val activeTenant = currentTenantId
            ?: throw TenantSecurityException("Tenant violation: No active tenant context established.")
        if (activeTenant != targetInstitutionId) {
            throw TenantSecurityException(
                "Tenant violation: Active tenant is '$activeTenant' but attempted operation on '$targetInstitutionId'."
            )
        }
    }

    /**
     * Asserts that the current user has the required granular permission.
     */
    fun assertPermission(requiredPermission: String) {
        val claims = currentClaims
            ?: throw RoleAuthorizationException("Unauthorized: No active authenticated session.")

        if (!claims.hasPermission(requiredPermission)) {
            throw RoleAuthorizationException(
                "Access Denied: User '${claims.displayName}' lacks required permission '$requiredPermission'.",
                requiredRole = claims.role.roleClaim
            )
        }
    }

    /**
     * Asserts that the current user holds one of the specified roles.
     */
    fun assertRole(allowedRoles: Set<UserRole>) {
        val claims = currentClaims
            ?: throw RoleAuthorizationException("Unauthorized: No active authenticated session.")

        if (!allowedRoles.contains(claims.role)) {
            throw RoleAuthorizationException(
                "Access Denied: Role '${claims.role}' is not in authorized list: $allowedRoles",
                requiredRole = allowedRoles.firstOrNull()?.roleClaim
            )
        }
    }
}
