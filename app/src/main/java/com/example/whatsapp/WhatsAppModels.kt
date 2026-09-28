package com.example.whatsapp

import java.util.UUID

// =============================================================================
// OFFICIAL WHATSAPP BUSINESS API & ENTERPRISE MESSAGING MODELS
// =============================================================================

enum class WhatsAppDeliveryMode {
    OFFICIAL_BUSINESS_API,
    MANUAL_INTENT_FALLBACK
}

enum class WhatsAppTemplateType(val templateName: String, val category: String) {
    ATTENDANCE_ABSENT_ALERT("school_attendance_absent_v1", "UTILITY"),
    FEE_REMINDER_DUE("school_fee_reminder_v1", "UTILITY"),
    EXAM_REPORT_PUBLISHED("school_exam_report_v1", "UTILITY"),
    EMERGENCY_CIRCULAR("school_emergency_notice_v1", "AUTHENTICATION"),
    ADMISSION_CONFIRMATION("school_admission_ack_v1", "MARKETING")
}

enum class WhatsAppDeliveryStatus {
    CONSENT_REQUIRED,
    RATE_LIMITED,
    QUEUED,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

data class WhatsAppConsentRecord(
    val recipientPhone: String,
    val tenantId: String,
    val recipientRole: String, // "PARENT", "STUDENT", "STAFF"
    val hasOptedIn: Boolean = true,
    val optedInTimestamp: Long = System.currentTimeMillis(),
    val consentMethod: String = "SCHOOL_ENROLLMENT_FORM"
)

data class WhatsAppMessageRequest(
    val messageId: String = "WHT-MSG-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4).uppercase()}",
    val tenantId: String,
    val recipientPhone: String,
    val recipientName: String,
    val templateType: WhatsAppTemplateType,
    val templateParameters: Map<String, String>,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val preferredMode: WhatsAppDeliveryMode = WhatsAppDeliveryMode.OFFICIAL_BUSINESS_API,
    val requestedAt: Long = System.currentTimeMillis()
)

data class WhatsAppDispatchResult(
    val isSuccess: Boolean,
    val messageId: String,
    val deliveryMode: WhatsAppDeliveryMode,
    val status: WhatsAppDeliveryStatus,
    val renderedText: String,
    val errorReason: String? = null,
    val externalMessageId: String? = null,
    val dispatchedAt: Long = System.currentTimeMillis()
)
