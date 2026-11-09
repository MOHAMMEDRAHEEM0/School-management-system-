package com.example

import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.UserSession
import com.example.security.SecurityUtils
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Automated security test suite verifying:
 * 1. Safe registration with default STUDENT role
 * 2. Strict prevention of privilege-escalation (attempts to register as PRINCIPAL or ADMIN fail closed)
 * 3. Authoritative Firebase login success and failure (fail-closed, no fallback sessions)
 * 4. Complete logout clearing authenticated state and TenantSecurityContext
 * 5. Token expiration invalidating the session
 * 6. Unauthorized access rejection (cross-tenant, invalid tokens)
 * 7. Absence of plaintext and insecure password fallbacks
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseAuthSecurityTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: MockSecurityAuthRepository
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TenantSecurityContext.clearContext()
        authRepository = MockSecurityAuthRepository()
        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        TenantSecurityContext.clearContext()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. REGISTRATION WITH SAFE DEFAULT STUDENT ROLE
    // -------------------------------------------------------------------------

    @Test
    fun `registration strictly assigns safe default STUDENT role`() = runTest(testDispatcher) {
        val email = "newstudent.2026@stxaviers.edu"
        val displayName = "Rajesh Sharma"

        authViewModel.registerUser(
            email = email,
            password = "SecurePassword@2026",
            displayName = displayName,
            tenantId = "INST-SXA-2026"
        )
        advanceUntilIdle()

        val uiState = authViewModel.uiState.value
        assertTrue("User should be authenticated after successful registration", uiState.isAuthenticated)
        assertNotNull("Session must not be null", uiState.userSession)
        assertEquals("User role must strictly be STUDENT", UserRole.STUDENT, uiState.userSession?.role)
        assertEquals(displayName, uiState.userSession?.displayName)
        assertEquals("INST-SXA-2026", uiState.userSession?.institutionId)
        assertTrue("Session token must be an authoritative Firebase ID token",
            uiState.userSession?.sessionToken?.startsWith("FIREBASE-ID-TOKEN") == true)
    }

    // -------------------------------------------------------------------------
    // 2. PRIVILEGE-ESCALATION PREVENTION (PRINCIPAL / ADMIN REGISTRATION ATTEMPT)
    // -------------------------------------------------------------------------

    @Test
    fun `privilege escalation attempt to register as PRINCIPAL is strictly rejected`() = runTest(testDispatcher) {
        val result = authRepository.registerUser(
            email = "malicious.actor@stxaviers.edu",
            password = "ExploitPassword123!",
            displayName = "Fake Principal",
            role = UserRole.PRINCIPAL,
            institutionId = "INST-SXA-2026"
        )

        assertTrue("Registration with PRINCIPAL role must fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Must throw SecurityException on privilege escalation", exception is SecurityException)
        assertTrue(exception?.message?.contains("Privilege escalation rejected") == true)

        // Verify fail-closed: unauthenticated state remains intact
        assertNull("Repository session must be null", authRepository.currentSession.value)
        assertEquals(AuthState.Unauthenticated, authRepository.authState.value)
        assertNull("Security context must be null", TenantSecurityContext.getCurrentClaims())
    }

    @Test
    fun `privilege escalation attempt to register as ADMIN is strictly rejected`() = runTest(testDispatcher) {
        val result = authRepository.registerUser(
            email = "hacker@stxaviers.edu",
            password = "AdminAttack2026!",
            displayName = "Fake Admin",
            role = UserRole.ADMIN,
            institutionId = "INST-SXA-2026"
        )

        assertTrue("Registration with ADMIN role must fail", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Must throw SecurityException on privilege escalation", exception is SecurityException)

        // Verify fail-closed
        assertNull("Repository session must be null", authRepository.currentSession.value)
        assertFalse("Session must not be valid", authRepository.isSessionValid())
    }

    // -------------------------------------------------------------------------
    // 3. FIREBASE LOGIN SUCCESS & FAILURE (FAIL-CLOSED)
    // -------------------------------------------------------------------------

    @Test
    fun `valid credentials authenticate successfully via Firebase`() = runTest(testDispatcher) {
        authViewModel.updateIdentifier("authorized.teacher@stxaviers.edu")
        authViewModel.updatePassword("AuthoritativePass2026!")
        authViewModel.signInWithEmailPassword()
        advanceUntilIdle()

        val uiState = authViewModel.uiState.value
        assertTrue("State must be authenticated", uiState.isAuthenticated)
        assertNotNull(uiState.userSession)
        assertEquals("authorized.teacher@stxaviers.edu", uiState.userSession?.email)
        assertNull(uiState.errorMessage)
    }

    @Test
    fun `invalid credentials fail closed without creating authenticated session or fallback`() = runTest(testDispatcher) {
        authViewModel.updateIdentifier("valid.user@stxaviers.edu")
        authViewModel.updatePassword("WrongPassword123!")
        authViewModel.signInWithEmailPassword()
        advanceUntilIdle()

        val uiState = authViewModel.uiState.value
        assertFalse("State must remain unauthenticated on failure", uiState.isAuthenticated)
        assertNull("Session must not exist", uiState.userSession)
        assertNotNull("Error message must be set", uiState.errorMessage)
        assertNull("Tenant security context must be empty", TenantSecurityContext.getCurrentClaims())
    }

    // -------------------------------------------------------------------------
    // 4. LOGOUT CLEARS AUTHENTICATED STATE
    // -------------------------------------------------------------------------

    @Test
    fun `logout properly clears authenticated state and security context`() = runTest(testDispatcher) {
        // Authenticate first
        authViewModel.updateIdentifier("authorized.teacher@stxaviers.edu")
        authViewModel.updatePassword("AuthoritativePass2026!")
        authViewModel.signInWithEmailPassword()
        advanceUntilIdle()
        assertTrue(authViewModel.uiState.value.isAuthenticated)
        assertNotNull(TenantSecurityContext.getCurrentClaims())

        // Logout
        authViewModel.signOut()
        advanceUntilIdle()

        val uiState = authViewModel.uiState.value
        assertFalse("State must be unauthenticated after sign out", uiState.isAuthenticated)
        assertNull("Session must be cleared", uiState.userSession)
        assertNull("Security claims context must be cleared", TenantSecurityContext.getCurrentClaims())
        assertFalse("Session validity check must return false", authRepository.isSessionValid())
    }

    // -------------------------------------------------------------------------
    // 5. TOKEN EXPIRATION
    // -------------------------------------------------------------------------

    @Test
    fun `expired Firebase ID token invalidates session and fails closed`() = runTest(testDispatcher) {
        authViewModel.updateIdentifier("authorized.teacher@stxaviers.edu")
        authViewModel.updatePassword("AuthoritativePass2026!")
        authViewModel.signInWithEmailPassword()
        advanceUntilIdle()
        assertTrue(authViewModel.uiState.value.isAuthenticated)

        // Force expiration of token
        val expiredSession = authRepository.currentSession.value!!.copy(
            expiresAt = System.currentTimeMillis() - 5000L
        )
        authRepository.setSession(expiredSession)

        assertTrue("Session must be marked expired", expiredSession.isExpired)
        assertFalse("isSessionValid must be false for expired token", authRepository.isSessionValid())

        // Restoring session with expired token must fail closed and clear the session
        val restoreResult = authRepository.restoreSessionIfValid()
        assertTrue("Session restoration must fail on expired token", restoreResult.isFailure)
        assertNull("Repository session must be wiped after expired token check", authRepository.currentSession.value)
        assertEquals(AuthState.Unauthenticated, authRepository.authState.value)
    }

    // -------------------------------------------------------------------------
    // 6. UNAUTHORIZED / CROSS-TENANT ATTEMPTS
    // -------------------------------------------------------------------------

    @Test
    fun `cross-tenant authentication attempt is rejected`() = runTest(testDispatcher) {
        val result = authRepository.signInWithEmailPassword(
            identifier = "authorized.teacher@stxaviers.edu",
            password = "AuthoritativePass2026!",
            institutionId = "INST-ROGUE-TENANT"
        )

        assertTrue("Sign in across non-matching tenant must fail", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Must reject unauthorized tenant", ex is TenantSecurityException || ex is IllegalArgumentException)
        assertNull("Session must not be created", authRepository.currentSession.value)
    }

    // -------------------------------------------------------------------------
    // 7. SECURITY UTILS CRYPTOGRAPHIC VERIFICATION & NO PLAINTEXT FALLBACK
    // -------------------------------------------------------------------------

    @Test
    fun `cryptographic salt is generated using SecureRandom`() {
        val salt1 = SecurityUtils.generateSecureSalt()
        val salt2 = SecurityUtils.generateSecureSalt()

        assertEquals("Salt length should be 32 hex chars (16 bytes)", 32, salt1.length)
        assertEquals("Salt length should be 32 hex chars (16 bytes)", 32, salt2.length)
        assertTrue("Salts generated must be distinct", salt1 != salt2)
    }

    @Test
    fun `PBKDF2 password hashing works and incorrect password fails`() {
        val password = "MySuperSecretPassword@2026"
        val salt = SecurityUtils.generateSecureSalt()
        val hash = SecurityUtils.hashPasswordPBKDF2(password, salt)

        assertTrue("Hash must start with pbkdf2 prefix", hash.startsWith("pbkdf2:65536:"))
        assertTrue("Password verification must succeed with correct password",
            SecurityUtils.verifyPassword(password, salt, hash))
        assertFalse("Password verification must fail with incorrect password",
            SecurityUtils.verifyPassword("WrongPassword!", salt, hash))
        assertFalse("Password verification must fail with altered salt",
            SecurityUtils.verifyPassword(password, "different_salt", hash))
    }

    // -------------------------------------------------------------------------
    // 8. DISABLED ACCOUNT FAIL-CLOSED ENFORCEMENT
    // -------------------------------------------------------------------------

    @Test
    fun `disabled account fails closed immediately upon login attempt`() = runTest(testDispatcher) {
        val disabledEmail = "suspended.student@stxaviers.edu"
        authRepository.disabledAccountEmail = disabledEmail

        val result = authRepository.signInWithEmailPassword(
            identifier = disabledEmail,
            password = "AnyValidPassword123!",
            institutionId = "INST-SXA-2026"
        )

        assertTrue("Authentication for disabled account must fail", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Exception must be DisabledAccountException", ex is com.example.security.DisabledAccountException)
        assertNull("No session should be active", authRepository.currentSession.value)
        assertNull("Security context must be cleared", TenantSecurityContext.getCurrentClaims())
    }

    // -------------------------------------------------------------------------
    // 9. CLIENT-SUPPLIED PRIVILEGE ESCALATION ATTEMPTS REJECTED
    // -------------------------------------------------------------------------

    @Test
    fun `client-supplied TEACHER and ACCOUNTANT registration roles are rejected`() = runTest(testDispatcher) {
        val teacherResult = authRepository.registerUser(
            email = "unauthorized.teacher@stxaviers.edu",
            password = "Password@2026!",
            displayName = "Fake Teacher",
            role = UserRole.TEACHER,
            institutionId = "INST-SXA-2026"
        )
        assertTrue("Teacher self-registration must be rejected", teacherResult.isFailure)
        assertTrue(teacherResult.exceptionOrNull() is SecurityException)

        val accountantResult = authRepository.registerUser(
            email = "unauthorized.accountant@stxaviers.edu",
            password = "Password@2026!",
            displayName = "Fake Accountant",
            role = UserRole.ACCOUNTANT,
            institutionId = "INST-SXA-2026"
        )
        assertTrue("Accountant self-registration must be rejected", accountantResult.isFailure)
        assertTrue(accountantResult.exceptionOrNull() is SecurityException)
    }

    // -------------------------------------------------------------------------
    // 10. OFFLINE FRESH-LOGIN REJECTION (FAIL-CLOSED)
    // -------------------------------------------------------------------------

    @Test
    fun `fresh login attempt while offline fails closed with OfflineAuthenticationException`() = runTest(testDispatcher) {
        authRepository.simulateOffline = true

        val result = authRepository.signInWithEmailPassword(
            identifier = "student@stxaviers.edu",
            password = "Password@2026!",
            institutionId = "INST-SXA-2026"
        )

        assertTrue("Fresh login while offline must fail", result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("Exception must be OfflineAuthenticationException", ex is com.example.security.OfflineAuthenticationException)
        assertFalse("Authentication state must remain unauthenticated", authRepository.isSessionValid())
        assertNull("TenantSecurityContext must be empty", TenantSecurityContext.getCurrentClaims())
    }

    // -------------------------------------------------------------------------
    // 11. REUSABLE AUTH VALIDATOR TESTS
    // -------------------------------------------------------------------------

    @Test
    fun `AuthValidator correctly validates email format`() {
        // Valid emails
        assertTrue(com.example.security.AuthValidator.validateEmail("student@stxaviers.edu").isValid)
        assertTrue(com.example.security.AuthValidator.validateEmail("principal.office@school.ac.in").isValid)

        // Invalid emails
        assertFalse(com.example.security.AuthValidator.validateEmail("").isValid)
        assertFalse(com.example.security.AuthValidator.validateEmail("notanemail").isValid)
        assertFalse(com.example.security.AuthValidator.validateEmail(null).isValid)
    }

    @Test
    fun `AuthValidator enforces minimum password length of 6 characters`() {
        assertTrue(com.example.security.AuthValidator.validatePassword("123456").isValid)
        assertTrue(com.example.security.AuthValidator.validatePassword("StrongPass@2026").isValid)

        assertFalse(com.example.security.AuthValidator.validatePassword("12345").isValid)
        assertFalse(com.example.security.AuthValidator.validatePassword("").isValid)
        assertFalse(com.example.security.AuthValidator.validatePassword(null).isValid)
    }

    @Test
    fun `AuthValidator enforces full name constraints`() {
        assertTrue(com.example.security.AuthValidator.validateDisplayName("Aarav Sharma").isValid)
        assertFalse(com.example.security.AuthValidator.validateDisplayName("A").isValid)
        assertFalse(com.example.security.AuthValidator.validateDisplayName("").isValid)
        assertFalse(com.example.security.AuthValidator.validateDisplayName(null).isValid)
    }
}
