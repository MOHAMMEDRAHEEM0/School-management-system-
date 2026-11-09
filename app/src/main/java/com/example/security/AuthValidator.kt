package com.example.security

import android.util.Patterns

/**
 * Reusable, encapsulated validation for authentication inputs.
 * Adheres to single-responsibility principle and supports client-side safety checks
 * before dispatching network requests to Firebase Auth.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()

    val isValid: Boolean get() = this is Valid
    val errorMessage: String? get() = (this as? Invalid)?.reason
}

object AuthValidator {

    private const val MIN_PASSWORD_LENGTH = 6
    private const val MAX_EMAIL_LENGTH = 100
    private const val MIN_DISPLAY_NAME_LENGTH = 2
    private const val MAX_DISPLAY_NAME_LENGTH = 60

    private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    fun validateEmail(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult.Invalid("Email address cannot be empty")
        }
        val trimmed = email.trim()
        if (trimmed.length > MAX_EMAIL_LENGTH) {
            return ValidationResult.Invalid("Email address is too long")
        }
        if (!EMAIL_REGEX.matches(trimmed)) {
            return ValidationResult.Invalid("Please enter a valid email address (e.g. name@school.edu)")
        }
        return ValidationResult.Valid
    }

    fun validatePassword(password: String?): ValidationResult {
        if (password.isNullOrBlank()) {
            return ValidationResult.Invalid("Password cannot be empty")
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return ValidationResult.Invalid("Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        return ValidationResult.Valid
    }

    fun validateDisplayName(name: String?): ValidationResult {
        if (name.isNullOrBlank()) {
            return ValidationResult.Invalid("Full name cannot be empty")
        }
        val trimmed = name.trim()
        if (trimmed.length < MIN_DISPLAY_NAME_LENGTH) {
            return ValidationResult.Invalid("Full name must be at least $MIN_DISPLAY_NAME_LENGTH characters")
        }
        if (trimmed.length > MAX_DISPLAY_NAME_LENGTH) {
            return ValidationResult.Invalid("Full name cannot exceed $MAX_DISPLAY_NAME_LENGTH characters")
        }
        return ValidationResult.Valid
    }

    fun validateInstitutionalIdentifier(identifier: String?): ValidationResult {
        if (identifier.isNullOrBlank()) {
            return ValidationResult.Invalid("Identifier / Admission Number cannot be empty")
        }
        val sanitized = identifier.trim()
        if (sanitized.length < 3) {
            return ValidationResult.Invalid("Identifier must be at least 3 characters")
        }
        return ValidationResult.Valid
    }
}
