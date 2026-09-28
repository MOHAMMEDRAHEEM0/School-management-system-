package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthState
import com.example.data.repository.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// =============================================================================
// UI STATE FOR AUTHENTICATION FLOWS
// =============================================================================

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val userSession: UserSession? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val institutionId: String = "INST-SXA-2026",
    val identifierInput: String = "",
    val passwordInput: String = "",
    val displayNameInput: String = "",
    val isPasswordVisible: Boolean = false,
    val selectedRole: UserRole = UserRole.TEACHER
)

// =============================================================================
// PRODUCTION HILT AUTH VIEW MODEL
// =============================================================================

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeRepositoryAuthState()
    }

    private fun observeRepositoryAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticating -> {
                        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                    }
                    is AuthState.Authenticated -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = true,
                                userSession = state.session,
                                errorMessage = null,
                                successMessage = "Authenticated as ${state.session.displayName}"
                            )
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isAuthenticated = false,
                                userSession = null,
                                passwordInput = ""
                            )
                        }
                    }
                    is AuthState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = state.message
                            )
                        }
                    }
                }
            }
        }
    }

    // Input handlers
    fun updateIdentifier(value: String) {
        _uiState.update { it.copy(identifierInput = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(passwordInput = value, errorMessage = null) }
    }

    fun updateDisplayName(value: String) {
        _uiState.update { it.copy(displayNameInput = value, errorMessage = null) }
    }

    fun updateInstitutionId(value: String) {
        _uiState.update { it.copy(institutionId = value.trim(), errorMessage = null) }
    }

    fun setSelectedRole(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    /**
     * Signs in using email/username credentials for current institution tenant.
     */
    fun signInWithEmailPassword(
        overrideIdentifier: String? = null,
        overridePassword: String? = null,
        overrideTenant: String? = null
    ): Job {
        val identifier = overrideIdentifier ?: _uiState.value.identifierInput.trim()
        val password = overridePassword ?: _uiState.value.passwordInput
        val tenant = overrideTenant ?: _uiState.value.institutionId.trim()

        if (identifier.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter username, email, or mobile number.") }
            return Job().apply { complete() }
        }
        if (password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithEmailPassword(
                identifier = identifier,
                password = password,
                institutionId = tenant
            )
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userSession = session,
                        errorMessage = null,
                        successMessage = "Authenticated as ${session.displayName}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Invalid credentials or unauthorized institution."
                    )
                }
            }
        }
    }

    /**
     * Signs in using Google ID token via Android Credential Manager.
     */
    fun signInWithGoogleCredential(idToken: String, tenantId: String? = null): Job {
        val tenant = tenantId ?: _uiState.value.institutionId.trim()
        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.signInWithGoogleCredential(
                idToken = idToken,
                institutionId = tenant
            )
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userSession = session,
                        errorMessage = null,
                        successMessage = "Authenticated as ${session.displayName}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Google Sign-In failed."
                    )
                }
            }
        }
    }

    /**
     * Registers a new tenant user with salted hash credential storage.
     */
    fun registerUser(
        email: String? = null,
        password: String? = null,
        displayName: String? = null,
        role: UserRole? = null,
        tenantId: String? = null
    ): Job {
        val targetEmail = email ?: _uiState.value.identifierInput.trim()
        val targetPassword = password ?: _uiState.value.passwordInput
        val targetName = displayName ?: _uiState.value.displayNameInput.trim()
        val targetRole = role ?: _uiState.value.selectedRole
        val targetTenant = tenantId ?: _uiState.value.institutionId.trim()

        if (targetEmail.isBlank() || !targetEmail.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return Job().apply { complete() }
        }
        if (targetPassword.length < 6) {
            _uiState.update { it.copy(errorMessage = "Password must be at least 6 characters long.") }
            return Job().apply { complete() }
        }
        if (targetName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your full name.") }
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerUser(
                email = targetEmail,
                password = targetPassword,
                displayName = targetName,
                role = targetRole,
                institutionId = targetTenant
            )
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        userSession = session,
                        errorMessage = null,
                        successMessage = "Account created successfully for ${session.displayName}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Registration failed."
                    )
                }
            }
        }
    }

    /**
     * Dispatches password recovery email.
     */
    fun sendPasswordReset(email: String? = null): Job {
        val targetEmail = email ?: _uiState.value.identifierInput.trim()
        if (targetEmail.isBlank() || !targetEmail.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address to reset password.") }
            return Job().apply { complete() }
        }

        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.sendPasswordResetEmail(targetEmail)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Password reset instructions sent to $targetEmail"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to send reset email."
                    )
                }
            }
        }
    }

    /**
     * Safely signs out the current session and clears security context.
     */
    fun signOut(): Job {
        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.signOut()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userSession = null,
                    passwordInput = "",
                    successMessage = "Signed out successfully."
                )
            }
        }
    }

    /**
     * Switches tenant context for multi-institution administrative profiles.
     */
    fun switchTenant(targetInstitutionId: String): Job {
        return viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.switchTenant(targetInstitutionId)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        institutionId = targetInstitutionId,
                        userSession = session,
                        successMessage = "Switched to tenant $targetInstitutionId"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Tenant switch unauthorized."
                    )
                }
            }
        }
    }
}
