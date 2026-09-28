package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.model.getDefaultPermissionsForRole
import com.example.security.DisabledAccountException
import com.example.security.SecureSessionManager
import com.example.security.SecurityUtils
import com.example.security.SessionExpiredException
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// =============================================================================
// AUTHENTICATION STATES & SESSION DATA CLASSES
// =============================================================================

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Authenticating : AuthState()
    data class Authenticated(val session: UserSession) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserSession(
    val userId: String,
    val institutionId: String,
    val email: String,
    val displayName: String,
    val role: UserRole,
    val sessionToken: String,
    val claims: SecurityClaims,
    val authProvider: String = "EMAIL_PASSWORD",
    val issuedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L // 24 hours
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt
}

// =============================================================================
// MULTI-TENANT AUTH REPOSITORY INTERFACE
// =============================================================================

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val currentSession: StateFlow<UserSession?>

    /**
     * Signs in using email/username and password under a specific institution tenant.
     */
    suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Signs in using Google ID Token via Android Credential Manager.
     */
    suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Registers a new tenant user with cryptographic salted password storage.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Terminate the active session and clears the tenant security context.
     */
    suspend fun signOut()

    /**
     * Dispatches a password recovery request for the given email address.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Refreshes the active session token and extends validity.
     */
    suspend fun refreshToken(): Result<UserSession>

    /**
     * Validates whether an active session exists and is unexpired for the tenant.
     */
    fun isSessionValid(): Boolean

    /**
     * Switches the active tenant if authorized for multi-school access.
     */
    suspend fun switchTenant(targetInstitutionId: String): Result<UserSession>
}

// =============================================================================
// PRODUCTION IMPLEMENTATION (Firebase Auth + Room Multi-Tenant Cache)
// =============================================================================

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val schoolDao: SchoolDao,
    private val firebaseAuth: FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()
) : AuthRepository {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    override suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating

        try {
            // 1. Fetch user by username, email or phone under target institution tenant
            val user = schoolDao.getUserByCredentials(identifier.trim(), institutionId)
                ?: schoolDao.getUserByUsernameOrPhone(identifier.trim())

            if (user == null) {
                val errorMsg = "Invalid credentials for institution tenant: $institutionId"
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalArgumentException(errorMsg))
            }

            // Tenant isolation verification
            if (user.institutionId != institutionId) {
                val errorMsg = "Cross-tenant access violation: user does not belong to $institutionId"
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(TenantSecurityException(errorMsg))
            }

            // Disabled account verification
            if (user.isAccountDisabled || !user.isActive) {
                val errorMsg = "Access Denied: Account is disabled for institution tenant $institutionId."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(DisabledAccountException(errorMsg))
            }

            // 2. Cryptographic password verification (Salted SHA-256 with legacy fallback)
            val isPasswordValid = SecurityUtils.verifyPassword(
                rawPassword = password,
                salt = user.passwordSalt,
                storedHash = user.passwordHash,
                fallbackPlain = user.password
            )

            if (!isPasswordValid) {
                val errorMsg = "Invalid password. Authentication failed."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalArgumentException(errorMsg))
            }

            // 3. Attempt Firebase Auth sign-in if email format and Firebase is active
            if (firebaseAuth != null && user.email.contains("@")) {
                runCatching {
                    firebaseAuth.signInWithEmailAndPassword(user.email, password).await()
                }
            }

            // 4. Construct production UserSession and SecurityClaims
            val userRole = runCatching { UserRole.valueOf(user.role.uppercase()) }.getOrDefault(UserRole.TEACHER)
            val permissions = getDefaultPermissionsForRole(userRole)
            val sessionToken = SecurityUtils.generateSessionToken(user.id, institutionId)

            val claims = SecurityClaims(
                userId = user.id,
                role = userRole,
                roleClaim = userRole.roleClaim,
                displayName = user.displayName,
                phone = user.phoneNumber,
                email = user.email,
                permissions = permissions,
                sessionToken = sessionToken
            )

            // Enforce tenant context
            TenantSecurityContext.setContext(claims, institutionId)

            val session = UserSession(
                userId = user.id,
                institutionId = institutionId,
                email = user.email,
                displayName = user.displayName,
                role = userRole,
                sessionToken = sessionToken,
                claims = claims,
                authProvider = "EMAIL_PASSWORD"
            )

            // Audit log the successful authentication
            schoolDao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    action = "USER_SIGN_IN",
                    entityType = "UserSession",
                    entityId = user.id,
                    actorUserId = user.id,
                    actorName = user.displayName,
                    actorRole = userRole.roleClaim,
                    previousState = null,
                    newState = "AUTHENTICATED",
                    remarks = "Authenticated successfully via email/password credentials."
                )
            )

            SecureSessionManager.saveSession(session)
            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Authentication failed due to an unexpected error."
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating

        try {
            var firebaseEmail = ""
            var firebaseDisplayName = ""
            var firebaseUid = ""

            // 1. Authenticate with Firebase using Google ID Token if Firebase Auth is present
            if (firebaseAuth != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    firebaseEmail = firebaseUser.email ?: ""
                    firebaseDisplayName = firebaseUser.displayName ?: "Google User"
                    firebaseUid = firebaseUser.uid
                }
            } else {
                firebaseEmail = "google.user@example.com"
                firebaseDisplayName = "Google User"
                firebaseUid = "GOOG-${idToken.take(8)}"
            }

            // 2. Lookup or link local user in tenant database
            var localUser = schoolDao.getUserByCredentials(firebaseEmail, institutionId)
            if (localUser == null) {
                // Auto-provision student/parent role if invited or default to STUDENT
                val newUserId = "USR-GGL-${UUID.randomUUID().toString().take(8).uppercase()}"
                localUser = UserEntity(
                    id = newUserId,
                    institutionId = institutionId,
                    username = firebaseEmail.substringBefore("@"),
                    email = firebaseEmail,
                    displayName = firebaseDisplayName,
                    role = "STUDENT",
                    password = "",
                    passwordSalt = "sxa_salt_2026",
                    passwordHash = SecurityUtils.hashPassword(UUID.randomUUID().toString(), "sxa_salt_2026")
                )
                schoolDao.insertUser(localUser)
            }

            // Disabled account verification
            if (localUser.isAccountDisabled || !localUser.isActive) {
                val errorMsg = "Access Denied: Account is disabled for institution tenant $institutionId."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(DisabledAccountException(errorMsg))
            }

            val userRole = runCatching { UserRole.valueOf(localUser.role.uppercase()) }.getOrDefault(UserRole.STUDENT)
            val sessionToken = SecurityUtils.generateSessionToken(localUser.id, institutionId)
            val claims = SecurityClaims(
                userId = localUser.id,
                role = userRole,
                roleClaim = userRole.roleClaim,
                displayName = localUser.displayName,
                phone = localUser.phoneNumber,
                email = localUser.email,
                permissions = getDefaultPermissionsForRole(userRole),
                sessionToken = sessionToken
            )

            TenantSecurityContext.setContext(claims, institutionId)

            val session = UserSession(
                userId = localUser.id,
                institutionId = institutionId,
                email = localUser.email,
                displayName = localUser.displayName,
                role = userRole,
                sessionToken = sessionToken,
                claims = claims,
                authProvider = "GOOGLE"
            )

            SecureSessionManager.saveSession(session)
            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Google Sign-In failed."
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    override suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole,
        institutionId: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating

        try {
            // Check if email already registered in this tenant
            val existing = schoolDao.getUserByCredentials(email.trim(), institutionId)
            if (existing != null) {
                val errorMsg = "An account with email $email already exists for tenant $institutionId."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            // Optional Firebase Auth user creation
            if (firebaseAuth != null) {
                runCatching {
                    firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
                }
            }

            // Salted cryptographic hash
            val salt = "sxa_salt_${System.currentTimeMillis() % 10000}"
            val hash = SecurityUtils.hashPassword(password, salt)
            val userId = "USR-${role.badge}-${UUID.randomUUID().toString().take(6).uppercase()}"

            val newUser = UserEntity(
                id = userId,
                institutionId = institutionId,
                username = email.substringBefore("@"),
                email = email.trim(),
                displayName = displayName.trim(),
                role = role.name,
                password = "", // Plaintext is never stored in production
                passwordSalt = salt,
                passwordHash = hash
            )
            schoolDao.insertUser(newUser)

            val sessionToken = SecurityUtils.generateSessionToken(userId, institutionId)
            val claims = SecurityClaims(
                userId = userId,
                role = role,
                roleClaim = role.roleClaim,
                displayName = displayName,
                email = email,
                permissions = getDefaultPermissionsForRole(role),
                sessionToken = sessionToken
            )

            TenantSecurityContext.setContext(claims, institutionId)

            val session = UserSession(
                userId = userId,
                institutionId = institutionId,
                email = email,
                displayName = displayName,
                role = role,
                sessionToken = sessionToken,
                claims = claims,
                authProvider = "EMAIL_PASSWORD"
            )

            SecureSessionManager.saveSession(session)
            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "User registration failed."
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    override suspend fun signOut() = withContext(Dispatchers.IO) {
        val session = _currentSession.value
        if (session != null) {
            schoolDao.insertAuditLog(
                AuditLogEntity(
                    institutionId = session.institutionId,
                    action = "USER_SIGN_OUT",
                    entityType = "UserSession",
                    entityId = session.userId,
                    actorUserId = session.userId,
                    actorName = session.displayName,
                    actorRole = session.role.roleClaim,
                    previousState = "AUTHENTICATED",
                    newState = "UNAUTHENTICATED",
                    remarks = "Signed out of session safely."
                )
            )
        }
        if (firebaseAuth != null) {
            runCatching { firebaseAuth.signOut() }
        }
        SecureSessionManager.clearSession()
        TenantSecurityContext.setContext(null, "INST-SXA-2026")
        _currentSession.value = null
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (firebaseAuth != null) {
                firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<UserSession> = withContext(Dispatchers.IO) {
        val refreshed = SecureSessionManager.refreshSessionToken()
        if (refreshed != null) {
            _currentSession.value = refreshed
            _authState.value = AuthState.Authenticated(refreshed)
            Result.success(refreshed)
        } else {
            _authState.value = AuthState.Unauthenticated
            Result.failure(SessionExpiredException("Failed to refresh session token. Please re-authenticate."))
        }
    }

    override fun isSessionValid(): Boolean {
        val session = SecureSessionManager.getActiveSession() ?: _currentSession.value ?: return false
        return !session.isExpired
    }

    override suspend fun switchTenant(targetInstitutionId: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val current = _currentSession.value
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated session."))

        // Verify user profile exists in target tenant or is multi-school admin
        val userInTarget = schoolDao.getUserByCredentials(current.email, targetInstitutionId)
        if (userInTarget == null && current.role != UserRole.ADMIN) {
            return@withContext Result.failure(TenantSecurityException("User is not authorized for tenant $targetInstitutionId."))
        }

        val updatedRole = userInTarget?.let {
            runCatching { UserRole.valueOf(it.role.uppercase()) }.getOrDefault(current.role)
        } ?: current.role

        val newSessionToken = SecurityUtils.generateSessionToken(current.userId, targetInstitutionId)
        val newClaims = current.claims.copy(
            sessionToken = newSessionToken,
            role = updatedRole,
            roleClaim = updatedRole.roleClaim,
            permissions = getDefaultPermissionsForRole(updatedRole)
        )

        TenantSecurityContext.setContext(newClaims, targetInstitutionId)

        val newSession = current.copy(
            institutionId = targetInstitutionId,
            role = updatedRole,
            sessionToken = newSessionToken,
            claims = newClaims,
            issuedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + 86400000L
        )

        SecureSessionManager.saveSession(newSession)
        _currentSession.value = newSession
        _authState.value = AuthState.Authenticated(newSession)
        Result.success(newSession)
    }
}
