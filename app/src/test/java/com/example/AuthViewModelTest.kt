package com.example

import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.UserSession
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

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialUiState() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertNull(state.userSession)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertEquals("INST-SXA-2026", state.institutionId)
    }

    @Test
    fun testInputUpdates() = runTest(testDispatcher) {
        viewModel.updateIdentifier("teacher@school.org")
        assertEquals("teacher@school.org", viewModel.uiState.value.identifierInput)

        viewModel.updatePassword("SecretPass123!")
        assertEquals("SecretPass123!", viewModel.uiState.value.passwordInput)

        viewModel.updateDisplayName("Dr. Anita Roy")
        assertEquals("Dr. Anita Roy", viewModel.uiState.value.displayNameInput)

        viewModel.updateInstitutionId("INST-TEST-999")
        assertEquals("INST-TEST-999", viewModel.uiState.value.institutionId)

        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun testSignInValidation_emptyInputs() = runTest(testDispatcher) {
        viewModel.updateIdentifier("")
        viewModel.updatePassword("")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("username"))

        viewModel.updateIdentifier("validuser")
        viewModel.updatePassword("")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("password"))
    }

    @Test
    fun testSignInSuccess() = runTest(testDispatcher) {
        viewModel.updateIdentifier("teacher@sxa.edu")
        viewModel.updatePassword("ValidPass123")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAuthenticated)
        assertNotNull(state.userSession)
        assertEquals("teacher@sxa.edu", state.userSession?.email)
        assertNull(state.errorMessage)
        assertNotNull(state.successMessage)
    }

    @Test
    fun testSignInFailure_invalidCredentials() = runTest(testDispatcher) {
        viewModel.updateIdentifier("unknown@sxa.edu")
        viewModel.updatePassword("wrongpassword")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isAuthenticated)
        assertNull(state.userSession)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testRegisterUserValidation() = runTest(testDispatcher) {
        viewModel.registerUser(
            email = "invalid-email",
            password = "123",
            displayName = ""
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("email"))

        viewModel.registerUser(
            email = "user@school.org",
            password = "123",
            displayName = ""
        )
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("6 characters"))
    }

    @Test
    fun testRegisterUserSuccess_SafeDefaultStudentRole() = runTest(testDispatcher) {
        viewModel.registerUser(
            email = "newstudent@sxa.edu",
            password = "SecurePassword@123",
            displayName = "Student Mehta",
            tenantId = "INST-SXA-2026"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.isAuthenticated)
        assertNotNull(state.userSession)
        assertEquals("Student Mehta", state.userSession?.displayName)
        assertEquals(UserRole.STUDENT, state.userSession?.role)
    }

    @Test
    fun testRegisterPrivilegeEscalation_Rejected() = runTest(testDispatcher) {
        // Attempting to register as a privileged role (PRINCIPAL/ADMIN) directly through repository fails closed
        val principalResult = fakeAuthRepository.registerUser(
            email = "attacker@sxa.edu",
            password = "Password123!",
            displayName = "Attacker",
            role = UserRole.PRINCIPAL,
            institutionId = "INST-SXA-2026"
        )
        assertTrue(principalResult.isFailure)
        assertTrue(principalResult.exceptionOrNull() is SecurityException)

        val adminResult = fakeAuthRepository.registerUser(
            email = "attacker2@sxa.edu",
            password = "Password123!",
            displayName = "Attacker Admin",
            role = UserRole.ADMIN,
            institutionId = "INST-SXA-2026"
        )
        assertTrue(adminResult.isFailure)
        assertTrue(adminResult.exceptionOrNull() is SecurityException)
    }

    @Test
    fun testLoginFailure_FailsClosed() = runTest(testDispatcher) {
        viewModel.updateIdentifier("unknown@sxa.edu")
        viewModel.updatePassword("WrongPassword123")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.userSession)
        assertNotNull(state.errorMessage)
    }

    @Test
    fun testTokenExpiration_InvalidatesSession() = runTest(testDispatcher) {
        viewModel.updateIdentifier("teacher@sxa.edu")
        viewModel.updatePassword("ValidPass123")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAuthenticated)

        // Simulate expired token in repository
        val expiredSession = fakeAuthRepository.currentSession.value!!.copy(
            expiresAt = System.currentTimeMillis() - 1000L
        )
        fakeAuthRepository.setSession(expiredSession)
        assertTrue(expiredSession.isExpired)

        // Session validation fails closed
        val restoreResult = fakeAuthRepository.restoreSessionIfValid()
        assertTrue(restoreResult.isFailure)
        assertNull(fakeAuthRepository.currentSession.value)
    }

    @Test
    fun testSignOut() = runTest(testDispatcher) {
        // First authenticate
        viewModel.updateIdentifier("teacher@sxa.edu")
        viewModel.updatePassword("ValidPass123")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAuthenticated)

        // Sign out
        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticated)
        assertNull(state.userSession)
    }

    @Test
    fun testSendPasswordReset() = runTest(testDispatcher) {
        viewModel.sendPasswordReset("teacher@sxa.edu")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.successMessage)
        assertTrue(state.successMessage!!.contains("Password reset instructions sent"))
    }

    @Test
    fun testClearMessages() = runTest(testDispatcher) {
        viewModel.updateIdentifier("")
        viewModel.signInWithEmailPassword()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }
}

/**
 * Lightweight fake repository for AuthViewModel testing.
 */
class FakeAuthRepository : AuthRepository {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    fun setSession(session: UserSession?) {
        _currentSession.value = session
        _authState.value = if (session != null) AuthState.Authenticated(session) else AuthState.Unauthenticated
    }

    override suspend fun signInWithEmailPassword(
        identifier: String,
        password: String,
        institutionId: String
    ): Result<UserSession> {
        if (identifier == "teacher@sxa.edu" && password == "ValidPass123") {
            val session = UserSession(
                userId = "USR-001",
                institutionId = institutionId,
                email = identifier,
                displayName = "Anita Roy",
                role = UserRole.TEACHER,
                sessionToken = "FIREBASE-TOKEN-ABC-123",
                claims = SecurityClaims(
                    userId = "USR-001",
                    role = UserRole.TEACHER,
                    roleClaim = UserRole.TEACHER.roleClaim,
                    displayName = "Anita Roy",
                    permissions = setOf("VIEW_STUDENTS", "MANAGE_ATTENDANCE")
                )
            )
            _currentSession.value = session
            _authState.value = AuthState.Authenticated(session)
            return Result.success(session)
        } else {
            val error = "Invalid credentials for institution: $institutionId"
            _currentSession.value = null
            _authState.value = AuthState.Error(error)
            return Result.failure(IllegalArgumentException(error))
        }
    }

    override suspend fun signInWithGoogleCredential(
        idToken: String,
        institutionId: String
    ): Result<UserSession> {
        val session = UserSession(
            userId = "USR-GOOGLE-001",
            institutionId = institutionId,
            email = "googleuser@sxa.edu",
            displayName = "Google User",
            role = UserRole.TEACHER,
            sessionToken = "GOOGLE-FIREBASE-TOKEN-123",
            claims = SecurityClaims(
                userId = "USR-GOOGLE-001",
                role = UserRole.TEACHER,
                roleClaim = UserRole.TEACHER.roleClaim,
                displayName = "Google User",
                permissions = setOf("VIEW_STUDENTS")
            ),
            authProvider = "GOOGLE"
        )
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
        // Enforce privilege escalation guard
        if (role != UserRole.STUDENT) {
            val error = "Privilege escalation rejected: Registration cannot choose privileged role '${role.name}'. All new registrations strictly receive the safe default STUDENT role."
            _authState.value = AuthState.Error(error)
            return Result.failure(SecurityException(error))
        }

        val session = UserSession(
            userId = "USR-NEW-001",
            institutionId = institutionId,
            email = email,
            displayName = displayName,
            role = UserRole.STUDENT,
            sessionToken = "FIREBASE-TOKEN-NEW-123",
            claims = SecurityClaims(
                userId = "USR-NEW-001",
                role = UserRole.STUDENT,
                roleClaim = UserRole.STUDENT.roleClaim,
                displayName = displayName,
                permissions = setOf("VIEW_HOMEWORK")
            )
        )
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
            _authState.value = AuthState.Unauthenticated
            Result.failure(IllegalStateException("No valid Firebase session"))
        }
    }

    override suspend fun signOut() {
        _currentSession.value = null
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun isSessionValid(): Boolean {
        val session = _currentSession.value ?: return false
        return !session.isExpired
    }

    override suspend fun refreshToken(): Result<UserSession> {
        val cur = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        val refreshed = cur.copy(
            sessionToken = "REFRESHED-FIREBASE-TOKEN-${System.currentTimeMillis()}",
            expiresAt = System.currentTimeMillis() + 3600000L
        )
        _currentSession.value = refreshed
        return Result.success(refreshed)
    }

    override suspend fun switchTenant(targetInstitutionId: String): Result<UserSession> {
        val cur = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        val switched = cur.copy(institutionId = targetInstitutionId)
        _currentSession.value = switched
        _authState.value = AuthState.Authenticated(switched)
        return Result.success(switched)
    }
}
