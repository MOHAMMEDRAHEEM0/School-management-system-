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
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
    val sessionToken: String, // Authoritative Firebase ID token
    val claims: SecurityClaims,
    val authProvider: String = "FIREBASE_EMAIL_PASSWORD",
    val issuedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 3600000L // 1 hour Firebase token default
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
     * Signs in using email/username and password authoritative through Firebase Authentication.
     * Fails closed if Firebase authentication fails.
     */
    suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Signs in using Google ID Token authoritative via Firebase GoogleAuthProvider.
     * Fails closed on invalid credential; no mock provisioning is permitted.
     */
    suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Registers a new tenant user through Firebase Authentication.
     * Newly registered accounts strictly receive the safe default STUDENT role.
     * Any attempt by the client to register as a privileged role (ADMIN, PRINCIPAL, TEACHER, etc.)
     * is rejected as a privilege-escalation violation.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        displayName: String,
        role: UserRole = UserRole.STUDENT,
        institutionId: String = "INST-SXA-2026"
    ): Result<UserSession>

    /**
     * Terminates the active Firebase session and clears all security contexts.
     */
    suspend fun signOut()

    /**
     * Dispatches a password recovery request through Firebase Authentication.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * Refreshes the authoritative Firebase ID token and updates active session.
     */
    suspend fun refreshToken(): Result<UserSession>

    /**
     * Validates whether an active Firebase session exists and is unexpired.
     */
    fun isSessionValid(): Boolean

    /**
     * Restores active Firebase session on app launch if credentials remain valid.
     */
    suspend fun restoreSessionIfValid(): Result<UserSession>

    /**
     * Switches the active tenant if authorized by Firebase custom claims.
     */
    suspend fun switchTenant(targetInstitutionId: String): Result<UserSession>
}

// =============================================================================
// PRODUCTION ZERO-TRUST IMPLEMENTATION (Authoritative Firebase Auth)
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

    private fun getAuthInstance(): FirebaseAuth {
        return firebaseAuth ?: FirebaseAuth.getInstance()
    }

    override suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating

        val cleanId = identifier.trim()
        val cleanPass = password.trim()

        if (cleanId.isBlank() || cleanPass.isBlank()) {
            val errorMsg = "Credentials cannot be empty."
            _authState.value = AuthState.Error(errorMsg)
            return@withContext Result.failure(IllegalArgumentException(errorMsg))
        }

        // Resolve target email if user entered a username
        val targetEmail = if (cleanId.contains("@")) {
            cleanId
        } else {
            val local = schoolDao.getUserByUsernameOrPhone(cleanId)
            local?.email ?: "$cleanId@stxaviers.edu"
        }

        try {
            val isLiveFirebase = runCatching {
                val app = com.google.firebase.FirebaseApp.getInstance()
                val key = app.options.apiKey
                key.isNotBlank() && !key.startsWith("AIzaSyMock")
            }.getOrDefault(false)

            if (!isLiveFirebase) {
                val errorMsg = "Firebase Authentication is not yet configured for production (live google-services.json required). Local fallback credentials have been disabled for data safety."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(com.example.security.FirebaseConfigurationException(errorMsg))
            }

            val auth = try {
                getAuthInstance()
            } catch (e: Exception) {
                val errorMsg = "Firebase Authentication service is unavailable: ${e.message}"
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(com.example.security.FirebaseConfigurationException(errorMsg))
            }

            // 1. Authoritative Firebase Authentication (FAIL-CLOSED)
            val authResult = auth.signInWithEmailAndPassword(targetEmail, cleanPass).await()
            val fbUser = authResult.user ?: throw IllegalStateException("Firebase User is null after authentication.")

            // 2. Fetch authoritative Firebase ID Token
            val tokenResult = fbUser.getIdToken(false).await()
            val idToken = tokenResult.token ?: throw IllegalStateException("Failed to obtain authoritative Firebase ID token.")

            // 3. Extract tenant and role claims from Firebase
            val claimsTenant = (tokenResult.claims["tenantId"] as? String) ?: institutionId
            val claimsRoleStr = tokenResult.claims["role"] as? String

            // Tenant isolation verification
            if (claimsTenant != institutionId && claimsRoleStr != "ADMIN" && claimsRoleStr != "ROLE_ADMIN") {
                val errorMsg = "Cross-tenant access violation: user does not belong to tenant $institutionId"
                _authState.value = AuthState.Error(errorMsg)
                auth.signOut()
                return@withContext Result.failure(TenantSecurityException(errorMsg))
            }

            // Disabled account verification
            val isAccountDisabled = (tokenResult.claims["isAccountDisabled"] as? Boolean) == true
            if (isAccountDisabled) {
                val errorMsg = "Access Denied: Account is disabled for tenant $institutionId."
                _authState.value = AuthState.Error(errorMsg)
                auth.signOut()
                return@withContext Result.failure(DisabledAccountException(errorMsg))
            }

            // Resolve authoritative role from Firebase claims, or fallback to safe STUDENT default
            val userRole = if (!claimsRoleStr.isNullOrBlank()) {
                runCatching { UserRole.valueOf(claimsRoleStr.removePrefix("ROLE_").uppercase()) }
                    .getOrDefault(UserRole.STUDENT)
            } else {
                // Check local role cache if established previously, otherwise safe default STUDENT
                val localUser = schoolDao.getUserByCredentials(targetEmail, institutionId)
                localUser?.let { runCatching { UserRole.valueOf(it.role.uppercase()) }.getOrNull() }
                    ?: UserRole.STUDENT
            }

            val permissions = getDefaultPermissionsForRole(userRole)
            val claims = SecurityClaims(
                userId = fbUser.uid,
                role = userRole,
                roleClaim = userRole.roleClaim,
                displayName = fbUser.displayName ?: cleanId.substringBefore("@"),
                phone = fbUser.phoneNumber ?: "",
                email = targetEmail,
                permissions = permissions,
                sessionToken = idToken
            )

            // Cache / sync local user profile for offline metadata display without storing passwords
            var localUser = schoolDao.getUserByCredentials(targetEmail, institutionId)
            if (localUser == null) {
                localUser = UserEntity(
                    id = fbUser.uid,
                    institutionId = institutionId,
                    username = targetEmail.substringBefore("@"),
                    email = targetEmail,
                    displayName = fbUser.displayName ?: targetEmail.substringBefore("@"),
                    role = userRole.name,
                    password = "", // Plaintext is never stored
                    passwordSalt = "",
                    passwordHash = ""
                )
                schoolDao.insertUser(localUser)
            }

            val issuedAt = tokenResult.issuedAtTimestamp * 1000L
            val expiresAt = tokenResult.expirationTimestamp * 1000L

            val session = UserSession(
                userId = fbUser.uid,
                institutionId = institutionId,
                email = targetEmail,
                displayName = fbUser.displayName ?: localUser.displayName,
                role = userRole,
                sessionToken = idToken,
                claims = claims,
                authProvider = "FIREBASE_EMAIL_PASSWORD",
                issuedAt = issuedAt,
                expiresAt = expiresAt
            )

            TenantSecurityContext.setContext(claims, institutionId)
            SecureSessionManager.saveSession(session)

            schoolDao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    action = "USER_SIGN_IN_FIREBASE",
                    entityType = "UserSession",
                    entityId = fbUser.uid,
                    actorUserId = fbUser.uid,
                    actorName = session.displayName,
                    actorRole = userRole.roleClaim,
                    previousState = null,
                    newState = "AUTHENTICATED",
                    remarks = "Authenticated through authoritative Firebase Auth with verified ID token."
                )
            )

            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val isNetworkIssue = e is com.google.firebase.FirebaseNetworkException ||
                e is java.io.IOException ||
                e.message?.contains("network", ignoreCase = true) == true

            if (isNetworkIssue) {
                val offlineMsg = "Network unavailable. Fresh login requires internet connection to verify credentials with Firebase."
                _authState.value = AuthState.Error(offlineMsg)
                return@withContext Result.failure(com.example.security.OfflineAuthenticationException(offlineMsg))
            }

            val isCredentialsIssue = e is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ||
                e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("user", ignoreCase = true) == true

            val errorMsg = if (isCredentialsIssue) {
                "Invalid email or password. Please verify your credentials."
            } else {
                e.localizedMessage ?: "Authentication failed via Firebase Authentication."
            }
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(if (isCredentialsIssue) com.example.security.InvalidCredentialsException(errorMsg) else e)
        }
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String
    ): Result<UserSession> = withContext(Dispatchers.IO) {
        _authState.value = AuthState.Authenticating

        try {
            val auth = try {
                getAuthInstance()
            } catch (e: Exception) {
                val errorMsg = "Firebase Authentication service unavailable for Google Sign-In."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalStateException(errorMsg))
            }

            // 1. Authoritative Firebase sign-in with Google Credential (FAIL-CLOSED)
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user was null after Google authentication.")

            val tokenResult = fbUser.getIdToken(false).await()
            val fbIdToken = tokenResult.token
                ?: throw IllegalStateException("Failed to obtain authoritative Firebase ID token.")

            val claimsTenant = (tokenResult.claims["tenantId"] as? String) ?: institutionId
            val claimsRoleStr = tokenResult.claims["role"] as? String

            if (claimsTenant != institutionId && claimsRoleStr != "ADMIN" && claimsRoleStr != "ROLE_ADMIN") {
                val errorMsg = "Cross-tenant access violation: Google user does not belong to tenant $institutionId"
                _authState.value = AuthState.Error(errorMsg)
                auth.signOut()
                return@withContext Result.failure(TenantSecurityException(errorMsg))
            }

            val userRole = if (!claimsRoleStr.isNullOrBlank()) {
                runCatching { UserRole.valueOf(claimsRoleStr.removePrefix("ROLE_").uppercase()) }
                    .getOrDefault(UserRole.STUDENT)
            } else {
                UserRole.STUDENT
            }

            val email = fbUser.email ?: "google-user@stxaviers.edu"
            val displayName = fbUser.displayName ?: email.substringBefore("@")

            val claims = SecurityClaims(
                userId = fbUser.uid,
                role = userRole,
                roleClaim = userRole.roleClaim,
                displayName = displayName,
                phone = fbUser.phoneNumber ?: "",
                email = email,
                permissions = getDefaultPermissionsForRole(userRole),
                sessionToken = fbIdToken
            )

            var localUser = schoolDao.getUserByCredentials(email, institutionId)
            if (localUser == null) {
                localUser = UserEntity(
                    id = fbUser.uid,
                    institutionId = institutionId,
                    username = email.substringBefore("@"),
                    email = email,
                    displayName = displayName,
                    role = userRole.name,
                    password = "",
                    passwordSalt = "",
                    passwordHash = ""
                )
                schoolDao.insertUser(localUser)
            }

            val session = UserSession(
                userId = fbUser.uid,
                institutionId = institutionId,
                email = email,
                displayName = displayName,
                role = userRole,
                sessionToken = fbIdToken,
                claims = claims,
                authProvider = "GOOGLE_FIREBASE",
                issuedAt = tokenResult.issuedAtTimestamp * 1000L,
                expiresAt = tokenResult.expirationTimestamp * 1000L
            )

            TenantSecurityContext.setContext(claims, institutionId)
            SecureSessionManager.saveSession(session)

            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Google Sign-In failed via Firebase Authentication."
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
            // STRICT PRIVILEGE ESCALATION GUARD:
            // Newly registered accounts must NEVER be allowed to choose a privileged role from the client.
            // Every new registration receives the safe default STUDENT role.
            if (role != UserRole.STUDENT) {
                val errorMsg = "Privilege escalation rejected: Registration cannot choose privileged role '${role.name}'. All new registrations strictly receive the safe default STUDENT role."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(SecurityException(errorMsg))
            }

            val cleanEmail = email.trim()
            val cleanPass = password.trim()
            val cleanName = displayName.trim()

            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                val errorMsg = "Valid email address is required for registration."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalArgumentException(errorMsg))
            }
            if (cleanPass.length < 6) {
                val errorMsg = "Password must be at least 6 characters."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(IllegalArgumentException(errorMsg))
            }

            val isLiveFirebase = runCatching {
                val app = com.google.firebase.FirebaseApp.getInstance()
                val key = app.options.apiKey
                key.isNotBlank() && !key.startsWith("AIzaSyMock")
            }.getOrDefault(false)

            if (!isLiveFirebase) {
                val errorMsg = "Firebase Authentication is not yet configured for production (live google-services.json required). Student self-registration requires live Firebase Auth."
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(com.example.security.FirebaseConfigurationException(errorMsg))
            }

            val auth = try {
                getAuthInstance()
            } catch (e: Exception) {
                val errorMsg = "Firebase Authentication service is unavailable: ${e.message}"
                _authState.value = AuthState.Error(errorMsg)
                return@withContext Result.failure(com.example.security.FirebaseConfigurationException(errorMsg))
            }

            // 1. Authoritative user creation via Firebase Auth (FAIL-CLOSED)
            val authResult = auth.createUserWithEmailAndPassword(cleanEmail, cleanPass).await()
            val fbUser = authResult.user
                ?: throw IllegalStateException("Firebase user was null after user creation.")

            // 2. Set Firebase profile display name
            runCatching {
                val profileUpdate = UserProfileChangeRequest.Builder()
                    .setDisplayName(cleanName)
                    .build()
                fbUser.updateProfile(profileUpdate).await()
            }

            // 3. Obtain authoritative Firebase ID token
            val tokenResult = fbUser.getIdToken(true).await()
            val fbIdToken = tokenResult.token
                ?: throw IllegalStateException("Failed to obtain authoritative Firebase ID token.")

            // Enforce safe default STUDENT role
            val safeRole = UserRole.STUDENT
            val permissions = getDefaultPermissionsForRole(safeRole)

            val claims = SecurityClaims(
                userId = fbUser.uid,
                role = safeRole,
                roleClaim = safeRole.roleClaim,
                displayName = cleanName,
                email = cleanEmail,
                permissions = permissions,
                sessionToken = fbIdToken
            )

            // Cache profile locally in Room without storing passwords
            val newUser = UserEntity(
                id = fbUser.uid,
                institutionId = institutionId,
                username = cleanEmail.substringBefore("@"),
                email = cleanEmail,
                displayName = cleanName,
                role = safeRole.name,
                password = "", // Plaintext passwords are NEVER stored
                passwordSalt = "",
                passwordHash = ""
            )
            schoolDao.insertUser(newUser)

            val session = UserSession(
                userId = fbUser.uid,
                institutionId = institutionId,
                email = cleanEmail,
                displayName = cleanName,
                role = safeRole,
                sessionToken = fbIdToken,
                claims = claims,
                authProvider = "FIREBASE_EMAIL_PASSWORD",
                issuedAt = tokenResult.issuedAtTimestamp * 1000L,
                expiresAt = tokenResult.expirationTimestamp * 1000L
            )

            TenantSecurityContext.setContext(claims, institutionId)
            SecureSessionManager.saveSession(session)

            schoolDao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    action = "USER_REGISTERED_FIREBASE",
                    entityType = "UserEntity",
                    entityId = fbUser.uid,
                    actorUserId = fbUser.uid,
                    actorName = cleanName,
                    actorRole = safeRole.roleClaim,
                    previousState = null,
                    newState = "REGISTERED_STUDENT",
                    remarks = "Registered new account via Firebase Auth with safe default STUDENT role."
                )
            )

            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            val isNetworkIssue = e is com.google.firebase.FirebaseNetworkException ||
                e is java.io.IOException ||
                e.message?.contains("network", ignoreCase = true) == true

            if (isNetworkIssue) {
                val offlineMsg = "Network unavailable. Registration requires internet connection to register with Firebase Auth."
                _authState.value = AuthState.Error(offlineMsg)
                return@withContext Result.failure(com.example.security.OfflineAuthenticationException(offlineMsg))
            }

            val errorMsg = e.localizedMessage ?: "Registration failed via Firebase Authentication."
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
                    action = "USER_SIGN_OUT_FIREBASE",
                    entityType = "UserSession",
                    entityId = session.userId,
                    actorUserId = session.userId,
                    actorName = session.displayName,
                    actorRole = session.role.roleClaim,
                    previousState = "AUTHENTICATED",
                    newState = "UNAUTHENTICATED",
                    remarks = "Signed out safely. Firebase credentials cleared."
                )
            )
        }
        try {
            getAuthInstance().signOut()
        } catch (e: Exception) {
            android.util.Log.w("AuthRepository", "Firebase signOut encountered: ${e.javaClass.simpleName}")
        }

        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        _currentSession.value = null
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val auth = getAuthInstance()
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(): Result<UserSession> = withContext(Dispatchers.IO) {
        val auth = try { getAuthInstance() } catch (e: Exception) { null }
        val currentUser = auth?.currentUser

        if (currentUser == null) {
            signOut()
            return@withContext Result.failure(SessionExpiredException("No active Firebase session to refresh."))
        }

        try {
            val tokenResult = currentUser.getIdToken(true).await()
            val refreshedToken = tokenResult.token
                ?: throw IllegalStateException("Empty refreshed Firebase ID token.")

            val current = _currentSession.value
            if (current == null) {
                signOut()
                return@withContext Result.failure(SessionExpiredException("Session state lost during refresh."))
            }

            val newClaims = current.claims.copy(
                sessionToken = refreshedToken,
                authenticatedAt = System.currentTimeMillis()
            )

            val updatedSession = current.copy(
                sessionToken = refreshedToken,
                claims = newClaims,
                issuedAt = tokenResult.issuedAtTimestamp * 1000L,
                expiresAt = tokenResult.expirationTimestamp * 1000L
            )

            SecureSessionManager.saveSession(updatedSession)
            _currentSession.value = updatedSession
            _authState.value = AuthState.Authenticated(updatedSession)
            Result.success(updatedSession)
        } catch (e: Exception) {
            signOut()
            Result.failure(SessionExpiredException("Firebase token refresh failed: ${e.message}"))
        }
    }

    override fun isSessionValid(): Boolean {
        val auth = runCatching { getAuthInstance() }.getOrNull() ?: return false
        val currentUser = auth.currentUser ?: return false
        val session = SecureSessionManager.getActiveSession() ?: _currentSession.value ?: return false
        return !session.isExpired && session.userId == currentUser.uid
    }

    override suspend fun restoreSessionIfValid(): Result<UserSession> = withContext(Dispatchers.IO) {
        // 1. Check in-memory / persisted active session first for offline capability
        val cachedSession = SecureSessionManager.getActiveSession() ?: _currentSession.value

        if (cachedSession != null) {
            if (cachedSession.isExpired) {
                signOut()
                return@withContext Result.failure(SessionExpiredException("Cached session token has expired. Fresh login required."))
            }

            // If Firebase is available and online, attempt token refresh / verification
            val auth = runCatching { getAuthInstance() }.getOrNull()
            val currentUser = auth?.currentUser

            if (currentUser != null && currentUser.uid == cachedSession.userId) {
                return@withContext try {
                    val tokenResult = currentUser.getIdToken(false).await()
                    val idToken = tokenResult.token ?: cachedSession.sessionToken
                    val institutionId = (tokenResult.claims["tenantId"] as? String) ?: cachedSession.institutionId
                    val roleStr = tokenResult.claims["role"] as? String
                    val role = if (!roleStr.isNullOrBlank()) {
                        runCatching { UserRole.valueOf(roleStr.removePrefix("ROLE_").uppercase()) }.getOrDefault(cachedSession.role)
                    } else {
                        cachedSession.role
                    }

                    val updated = cachedSession.copy(
                        sessionToken = idToken,
                        institutionId = institutionId,
                        role = role,
                        authProvider = "FIREBASE_ONLINE_VERIFIED",
                        expiresAt = tokenResult.expirationTimestamp * 1000L
                    )
                    TenantSecurityContext.setContext(updated.claims, updated.institutionId)
                    SecureSessionManager.saveSession(updated)
                    _currentSession.value = updated
                    _authState.value = AuthState.Authenticated(updated)
                    Result.success(updated)
                } catch (e: Exception) {
                    // If network fails during token refresh but cached token is still unexpired:
                    // Retain offline-first access for previously authorized session
                    TenantSecurityContext.setContext(cachedSession.claims, cachedSession.institutionId)
                    _currentSession.value = cachedSession
                    _authState.value = AuthState.Authenticated(cachedSession)
                    Result.success(cachedSession)
                }
            } else if (currentUser == null && !cachedSession.isExpired) {
                // Offline scenario where Firebase user is cached in local session
                TenantSecurityContext.setContext(cachedSession.claims, cachedSession.institutionId)
                _currentSession.value = cachedSession
                _authState.value = AuthState.Authenticated(cachedSession)
                return@withContext Result.success(cachedSession)
            }
        }

        // No unexpired cached session available - fail closed
        val auth = runCatching { getAuthInstance() }.getOrNull()
        val currentUser = auth?.currentUser

        if (currentUser == null) {
            signOut()
            return@withContext Result.failure(SessionExpiredException("No active session found. Please log in."))
        }

        try {
            val tokenResult = currentUser.getIdToken(false).await()
            val idToken = tokenResult.token ?: throw IllegalStateException("Failed to obtain ID token.")

            val expiry = tokenResult.expirationTimestamp * 1000L
            if (expiry <= System.currentTimeMillis()) {
                return@withContext refreshToken()
            }

            val institutionId = (tokenResult.claims["tenantId"] as? String) ?: "INST-SXA-2026"
            val roleStr = tokenResult.claims["role"] as? String
            val role = if (!roleStr.isNullOrBlank()) {
                runCatching { UserRole.valueOf(roleStr.removePrefix("ROLE_").uppercase()) }.getOrDefault(UserRole.STUDENT)
            } else {
                UserRole.STUDENT
            }

            val email = currentUser.email ?: ""
            val name = currentUser.displayName ?: email.substringBefore("@")
            val permissions = getDefaultPermissionsForRole(role)

            val claims = SecurityClaims(
                userId = currentUser.uid,
                role = role,
                roleClaim = role.roleClaim,
                displayName = name,
                email = email,
                permissions = permissions,
                sessionToken = idToken
            )

            val session = UserSession(
                userId = currentUser.uid,
                institutionId = institutionId,
                email = email,
                displayName = name,
                role = role,
                sessionToken = idToken,
                claims = claims,
                authProvider = "FIREBASE_RESTORED",
                issuedAt = tokenResult.issuedAtTimestamp * 1000L,
                expiresAt = expiry
            )

            TenantSecurityContext.setContext(claims, institutionId)
            SecureSessionManager.saveSession(session)
            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            Result.success(session)
        } catch (e: Exception) {
            signOut()
            Result.failure(e)
        }
    }

    override suspend fun switchTenant(targetInstitutionId: String): Result<UserSession> = withContext(Dispatchers.IO) {
        val current = _currentSession.value
            ?: return@withContext Result.failure(IllegalStateException("No active authenticated session."))

        val auth = runCatching { getAuthInstance() }.getOrNull()
        val currentUser = auth?.currentUser
            ?: return@withContext Result.failure(IllegalStateException("No active Firebase user."))

        val tokenResult = currentUser.getIdToken(false).await()
        val isSuperAdmin = (tokenResult.claims["isSuperAdmin"] as? Boolean) == true || current.role == UserRole.ADMIN

        // Authoritative verification: cross-tenant switching requires Super Admin claims
        if (!isSuperAdmin) {
            return@withContext Result.failure(
                TenantSecurityException("User is not authorized for cross-tenant access to $targetInstitutionId.")
            )
        }

        val updatedSession = current.copy(
            institutionId = targetInstitutionId,
            issuedAt = System.currentTimeMillis()
        )

        SecureSessionManager.saveSession(updatedSession)
        _currentSession.value = updatedSession
        _authState.value = AuthState.Authenticated(updatedSession)
        Result.success(updatedSession)
    }
}
