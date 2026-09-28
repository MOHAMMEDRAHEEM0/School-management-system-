package com.example.cloud

/**
 * Enterprise Environment Architecture & Feature Flags.
 * Strictly separates DEVELOPMENT, STAGING, and PRODUCTION environments with distinct
 * endpoints, Firebase configurations, and security policies.
 */
enum class AppEnvironment {
    DEV,
    STAGING,
    PRODUCTION
}

data class EnvironmentConfig(
    val environment: AppEnvironment,
    val firebaseProjectId: String,
    val storageBucket: String,
    val apiBaseUrl: String,
    val authDomain: String,
    val enableDebugTokens: Boolean,
    val enableAppCheck: Boolean,
    val logRetentionDays: Int,
    val paymentGatewayMode: com.example.payment.PaymentEnvironment,
    val featureFlags: Map<String, Boolean>
) {
    fun isFeatureEnabled(flagName: String): Boolean = featureFlags[flagName] ?: false

    companion object {
        const val FLAG_OCR_REGISTER_SCAN = "FLAG_OCR_REGISTER_SCAN"
        const val FLAG_OFFICIAL_WHATSAPP_API = "FLAG_OFFICIAL_WHATSAPP_API"
        const val FLAG_ONLINE_PAYMENTS_GATEWAY = "FLAG_ONLINE_PAYMENTS_GATEWAY"
        const val FLAG_AI_ASSISTANT_STUDY = "FLAG_AI_ASSISTANT_STUDY"
        const val FLAG_BIOMETRIC_AUTH = "FLAG_BIOMETRIC_AUTH"
        const val FLAG_MULTI_CAMPUS = "FLAG_MULTI_CAMPUS"

        fun getDevConfig(): EnvironmentConfig = EnvironmentConfig(
            environment = AppEnvironment.DEV,
            firebaseProjectId = "school-erp-dev-2026",
            storageBucket = "school-erp-dev-2026.appspot.com",
            apiBaseUrl = "https://api-dev.schoolerp.internal/v1",
            authDomain = "school-erp-dev-2026.firebaseapp.com",
            enableDebugTokens = true,
            enableAppCheck = false,
            logRetentionDays = 7,
            paymentGatewayMode = com.example.payment.PaymentEnvironment.TEST_SANDBOX,
            featureFlags = mapOf(
                FLAG_OCR_REGISTER_SCAN to true,
                FLAG_OFFICIAL_WHATSAPP_API to true,
                FLAG_ONLINE_PAYMENTS_GATEWAY to true,
                FLAG_AI_ASSISTANT_STUDY to true,
                FLAG_BIOMETRIC_AUTH to true,
                FLAG_MULTI_CAMPUS to true
            )
        )

        fun getStagingConfig(): EnvironmentConfig = EnvironmentConfig(
            environment = AppEnvironment.STAGING,
            firebaseProjectId = "school-erp-staging-2026",
            storageBucket = "school-erp-staging-2026.appspot.com",
            apiBaseUrl = "https://api-staging.schoolerp.cloud/v1",
            authDomain = "school-erp-staging-2026.firebaseapp.com",
            enableDebugTokens = true,
            enableAppCheck = true,
            logRetentionDays = 30,
            paymentGatewayMode = com.example.payment.PaymentEnvironment.TEST_SANDBOX,
            featureFlags = mapOf(
                FLAG_OCR_REGISTER_SCAN to true,
                FLAG_OFFICIAL_WHATSAPP_API to true,
                FLAG_ONLINE_PAYMENTS_GATEWAY to true,
                FLAG_AI_ASSISTANT_STUDY to true,
                FLAG_BIOMETRIC_AUTH to true,
                FLAG_MULTI_CAMPUS to true
            )
        )

        fun getProductionConfig(): EnvironmentConfig = EnvironmentConfig(
            environment = AppEnvironment.PRODUCTION,
            firebaseProjectId = "school-erp-prod-2026",
            storageBucket = "school-erp-prod-2026.appspot.com",
            apiBaseUrl = "https://api.schoolerp.cloud/v1",
            authDomain = "auth.schoolerp.cloud",
            enableDebugTokens = false,
            enableAppCheck = true,
            logRetentionDays = 365,
            paymentGatewayMode = com.example.payment.PaymentEnvironment.PRODUCTION,
            featureFlags = mapOf(
                FLAG_OCR_REGISTER_SCAN to true,
                FLAG_OFFICIAL_WHATSAPP_API to true,
                FLAG_ONLINE_PAYMENTS_GATEWAY to true,
                FLAG_AI_ASSISTANT_STUDY to true,
                FLAG_BIOMETRIC_AUTH to true,
                FLAG_MULTI_CAMPUS to false
            )
        )
    }
}
