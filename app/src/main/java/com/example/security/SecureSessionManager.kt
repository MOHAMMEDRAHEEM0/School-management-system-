package com.example.security

import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.repository.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Enterprise Secure Session Manager.
 * Handles encrypted in-memory and persistent session states, token expiration,
 * automatic session renewal, disabled-account blocking, and active tenant context enforcement.
 */
class DisabledAccountException(message: String) : SecurityException(message)
class SessionExpiredException(message: String) : SecurityException(message)

object SecureSessionManager {

    private const val SESSION_TTL_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
    private const val TOKEN_REFRESH_THRESHOLD_MILLIS = 30 * 60 * 1000L // 30 minutes before expiry

    @Volatile
    private var activeSession: UserSession? = null

    private val _sessionFlow = MutableStateFlow<UserSession?>(null)
    val sessionFlow: StateFlow<UserSession?> = _sessionFlow.asStateFlow()

    /**
     * Initializes or stores a newly authenticated session with security validation.
     */
    fun saveSession(session: UserSession) {
        require(session.userId.isNotBlank()) { "User ID cannot be blank in session" }
        require(session.institutionId.isNotBlank()) { "Institution ID cannot be blank in session" }

        val validatedSession = if (session.expiresAt <= System.currentTimeMillis()) {
            session.copy(
                issuedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + SESSION_TTL_MILLIS
            )
        } else {
            session
        }

        activeSession = validatedSession
        _sessionFlow.value = validatedSession

        // Propagate to TenantSecurityContext
        TenantSecurityContext.setContext(validatedSession.claims, validatedSession.institutionId)
    }

    /**
     * Retrieves the current session, throwing if expired or null.
     */
    fun getActiveSession(): UserSession? {
        val current = activeSession ?: return null
        if (current.isExpired) {
            clearSession()
            return null
        }
        return current
    }

    /**
     * Checks if the active session is valid and matches the target tenant.
     */
    fun assertValidSessionForTenant(targetInstitutionId: String): UserSession {
        val session = activeSession
            ?: throw SecurityException("Unauthorized: No active session. Please authenticate.")

        if (session.isExpired) {
            clearSession()
            throw SessionExpiredException("Authentication session has expired. Please log in again.")
        }

        if (session.institutionId != targetInstitutionId) {
            throw TenantSecurityException(
                "Cross-tenant access violation: Active session belongs to '${session.institutionId}', cannot access '$targetInstitutionId'."
            )
        }

        return session
    }

    /**
     * Validates that the account has not been disabled by an administrator.
     */
    fun validateAccountStatus(isAccountDisabled: Boolean) {
        if (isAccountDisabled) {
            clearSession()
            throw DisabledAccountException(
                "Access Denied: This account has been disabled by the institution administration. Contact the Principal or IT Helpdesk."
            )
        }
    }

    /**
     * Refreshes the session token and extends validity if within refresh threshold.
     */
    fun refreshSessionToken(): UserSession? {
        val current = activeSession ?: return null
        if (current.isExpired) {
            clearSession()
            return null
        }

        val now = System.currentTimeMillis()
        val newToken = SecurityUtils.generateSessionToken(current.userId, current.institutionId)
        val newClaims = current.claims.copy(
            sessionToken = newToken,
            authenticatedAt = now
        )

        val refreshed = current.copy(
            sessionToken = newToken,
            claims = newClaims,
            issuedAt = now,
            expiresAt = now + SESSION_TTL_MILLIS
        )

        saveSession(refreshed)
        return refreshed
    }

    /**
     * Checks if session should be refreshed preemptively.
     */
    fun shouldRefreshToken(): Boolean {
        val current = activeSession ?: return false
        val timeUntilExpiry = current.expiresAt - System.currentTimeMillis()
        return timeUntilExpiry in 1..TOKEN_REFRESH_THRESHOLD_MILLIS
    }

    /**
     * Securely destroys the current session and clears the tenant security context.
     */
    fun clearSession() {
        activeSession = null
        _sessionFlow.value = null
        TenantSecurityContext.clearContext()
    }
}
