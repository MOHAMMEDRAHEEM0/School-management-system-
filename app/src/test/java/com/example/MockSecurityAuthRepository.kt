package com.example

import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.UserSession
import com.example.security.DisabledAccountException
import com.example.security.SecureSessionManager
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Strict security test double for AuthRepository enforcing:
 * - Authoritative Firebase token semantics (FIREBASE-ID-TOKEN-...)
 * - Strict prohibition of client-side privilege escalation (safe default STUDENT only)
 * - Fail-closed error handling (no fallback credentials)
 * - Complete session lifecycle synchronization with SecureSessionManager and TenantSecurityContext
 * - Disabled account enforcement
 * - Tenant boundary isolation
 */
class MockSecurityAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    var disabledAccountEmail: String? = null
    var simulateOffline: Boolean = false

    fun setSession(session: UserSession?) {
        _currentSession.value = session
        if (session != null) {
            SecureSessionManager.saveSession(session)
            TenantSecurityContext.setContext(session.claims, session.institutionId)
            _authState.value = AuthState.Authenticated(session)
        } else {
            SecureSessionManager.clearSession()
            TenantSecurityContext.clearContext()
            _authState.value = AuthState.Unauthenticated
        }
    }

    override suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String
    ): Result<UserSession> {
        val cleanId = identifier.trim()
        val cleanPass = password.trim()

        if (cleanId == disabledAccountEmail) {
            _currentSession.value = null
            SecureSessionManager.clearSession()
            TenantSecurityContext.clearContext()
            _authState.value = AuthState.Error("Access Denied: Account is disabled.")
            return Result.failure(DisabledAccountException("Access Denied: Account is disabled for tenant $institutionId."))
        }

        if (institutionId.startsWith("INST-DIFFERENT") || institutionId.startsWith("INST-ROGUE")) {
            _authState.value = AuthState.Error("Cross-tenant access violation.")
            return Result.failure(TenantSecurityException("Cross-tenant access violation: user does not belong to tenant $institutionId"))
        }

        if (simulateOffline) {
            val offlineMsg = "Network unavailable. Fresh login requires internet connection to verify credentials with Firebase."
            _authState.value = AuthState.Error(offlineMsg)
            return Result.failure(com.example.security.OfflineAuthenticationException(offlineMsg))
        }

        if (cleanPass == "WrongPassword!" || cleanPass == "WrongPassword999!" || cleanPass == "WrongPassword123!") {
            _currentSession.value = null
            SecureSessionManager.clearSession()
            TenantSecurityContext.clearContext()
            _authState.value = AuthState.Error("Invalid Firebase credentials.")
            return Result.failure(com.example.security.InvalidCredentialsException("Invalid Firebase credentials."))
        }

        val role = when {
            cleanId.contains("teacher") -> UserRole.TEACHER
            cleanId.contains("principal") -> UserRole.PRINCIPAL
            cleanId.contains("admin") -> UserRole.ADMIN
            else -> UserRole.STUDENT
        }

        val claims = SecurityClaims(
            userId = "USR-FB-${System.currentTimeMillis()}",
            role = role,
            roleClaim = role.roleClaim,
            displayName = if (role == UserRole.TEACHER) "Anita Roy" else cleanId.substringBefore("@"),
            permissions = setOf("VIEW_STUDENTS", "MANAGE_ATTENDANCE")
        )

        val session = UserSession(
            userId = claims.userId,
            institutionId = institutionId,
            email = cleanId,
            displayName = claims.displayName,
            role = role,
            sessionToken = "FIREBASE-ID-TOKEN-${System.currentTimeMillis()}",
            claims = claims,
            authProvider = "FIREBASE_EMAIL_PASSWORD"
        )

        SecureSessionManager.saveSession(session)
        TenantSecurityContext.setContext(claims, institutionId)
        _currentSession.value = session
        _authState.value = AuthState.Authenticated(session)
        return Result.success(session)
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String
    ): Result<UserSession> {
        if (idToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Google ID token is required."))
        }
        val claims = SecurityClaims(
            userId = "USR-FB-GOOGLE-01",
            role = UserRole.STUDENT,
            roleClaim = UserRole.STUDENT.roleClaim,
            displayName = "Google User",
            permissions = setOf("VIEW_HOMEWORK")
        )
        val session = UserSession(
            userId = "USR-FB-GOOGLE-01",
            institutionId = institutionId,
            email = "user@google.com",
            displayName = "Google User",
            role = UserRole.STUDENT,
            sessionToken = "FIREBASE-ID-TOKEN-GOOGLE",
            claims = claims,
            authProvider = "FIREBASE_GOOGLE"
        )
        SecureSessionManager.saveSession(session)
        TenantSecurityContext.setContext(claims, institutionId)
        _currentSession.value = session
        _authState.value = AuthState.Authenticated(session)
        return Result.success(session)
    }

    override suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
        institutionId: String
    ): Result<UserSession> {
        // Enforce privilege escalation prevention
        if (role != UserRole.STUDENT) {
            val error = "Privilege escalation rejected: Registration cannot choose privileged role '${role.name}'. All new registrations strictly receive the safe default STUDENT role."
            _authState.value = AuthState.Unauthenticated
            return Result.failure(SecurityException(error))
        }

        val claims = SecurityClaims(
            userId = "USR-FB-NEW-${System.currentTimeMillis()}",
            role = UserRole.STUDENT,
            roleClaim = UserRole.STUDENT.roleClaim,
            displayName = displayName,
            permissions = setOf("VIEW_HOMEWORK")
        )
        val session = UserSession(
            userId = claims.userId,
            institutionId = institutionId,
            email = email,
            displayName = displayName,
            role = UserRole.STUDENT,
            sessionToken = "FIREBASE-ID-TOKEN-REG-${System.currentTimeMillis()}",
            claims = claims,
            authProvider = "FIREBASE_EMAIL_PASSWORD"
        )
        SecureSessionManager.saveSession(session)
        TenantSecurityContext.setContext(claims, institutionId)
        _currentSession.value = session
        _authState.value = AuthState.Authenticated(session)
        return Result.success(session)
    }

    override suspend fun restoreSessionIfValid(): Result<UserSession> {
        val cur = _currentSession.value
        return if (cur != null && !cur.isExpired) {
            _authState.value = AuthState.Authenticated(cur)
            Result.success(cur)
        } else {
            _currentSession.value = null
            SecureSessionManager.clearSession()
            TenantSecurityContext.clearContext()
            _authState.value = AuthState.Unauthenticated
            Result.failure(IllegalStateException("No valid Firebase session"))
        }
    }

    override suspend fun signOut() {
        _currentSession.value = null
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isSessionValid(): Boolean {
        val s = _currentSession.value ?: return false
        return !s.isExpired
    }

    override suspend fun refreshToken(): Result<UserSession> {
        val cur = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        val refreshed = cur.copy(
            sessionToken = "FIREBASE-ID-TOKEN-REFRESHED-${System.currentTimeMillis()}",
            expiresAt = System.currentTimeMillis() + 3600000L
        )
        SecureSessionManager.saveSession(refreshed)
        _currentSession.value = refreshed
        return Result.success(refreshed)
    }

    override suspend fun switchTenant(targetInstitutionId: String): Result<UserSession> {
        val cur = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        if (targetInstitutionId != cur.institutionId) {
            return Result.failure(TenantSecurityException("Tenant violation: User does not belong to $targetInstitutionId"))
        }
        val switched = cur.copy(institutionId = targetInstitutionId)
        _currentSession.value = switched
        return Result.success(switched)
    }
}
