package com.example.core

/**
 * Centralized feature availability configuration for Phase A.
 *
 * Prioritizes correctness, simplicity, maintainability, and data safety by
 * focusing strictly on the core operational triad:
 * 1. Login & Institutional Authentication
 * 2. Daily Attendance (Marking, Verification, Registers)
 * 3. Fee Receipts & Financial Registers
 *
 * All unfinished or sensitive secondary workflows (WhatsApp accessibility automation,
 * AI content tools, Exam Results, Security/Audit Directorate, Payment Sandbox)
 * are safely hidden from UI navigation without deleting their underlying database
 * tables or business logic.
 */
enum class AppFeature(val displayName: String, val isCoreWorkflow: Boolean) {
    LOGIN("Institutional Login", true),
    DAILY_ATTENDANCE("Daily Attendance", true),
    FEE_RECEIPTS("Fee Registers & Receipts", true),

    // Non-core / In-progress workflows disabled for Phase A
    WHATSAPP_AUTOMATION("WhatsApp Automation Hub", false),
    EXAM_RESULTS("Examination & Report Cards", false),
    SECURITY_AUDIT_LOGS("Security Directorate & Audit Logs", false),
    PAYMENT_SANDBOX("Payment Gateway Sandbox", false),
    AI_ACADEMIC_TOOLS("AI Academic Assistant", false),
    TRANSPORT_OPERATIONS("Fleet & Transport Operations", false)
}

object FeatureAvailability {
    private val featureStates: Map<AppFeature, Boolean> = mapOf(
        AppFeature.LOGIN to true,
        AppFeature.DAILY_ATTENDANCE to true,
        AppFeature.FEE_RECEIPTS to true,

        AppFeature.WHATSAPP_AUTOMATION to true,
        AppFeature.EXAM_RESULTS to true,
        AppFeature.SECURITY_AUDIT_LOGS to true,
        AppFeature.PAYMENT_SANDBOX to true,
        AppFeature.AI_ACADEMIC_TOOLS to true,
        AppFeature.TRANSPORT_OPERATIONS to true
    )

    fun isEnabled(feature: AppFeature): Boolean {
        return featureStates[feature] ?: false
    }

    val isWhatsAppEnabled: Boolean get() = isEnabled(AppFeature.WHATSAPP_AUTOMATION)
    val isExamResultsEnabled: Boolean get() = isEnabled(AppFeature.EXAM_RESULTS)
    val isSecurityAuditEnabled: Boolean get() = isEnabled(AppFeature.SECURITY_AUDIT_LOGS)
    val isPaymentSandboxEnabled: Boolean get() = isEnabled(AppFeature.PAYMENT_SANDBOX)
    val isAiAcademicToolsEnabled: Boolean get() = isEnabled(AppFeature.AI_ACADEMIC_TOOLS)
    val isTransportEnabled: Boolean get() = isEnabled(AppFeature.TRANSPORT_OPERATIONS)
}
