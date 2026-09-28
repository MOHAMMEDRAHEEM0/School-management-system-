package com.example.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Enterprise WhatsApp Business API service with consent verification,
 * rate limiting, server-template rendering, and manual intent fallback.
 */
class WhatsAppService(
    private val maxMessagesPerMinutePerTenant: Int = 30
) {
    // Registered opt-in consent by (tenantId + ":" + phone)
    private val consentRegistry = ConcurrentHashMap<String, WhatsAppConsentRecord>()

    // Sliding-window rate limiting counters by (tenantId + ":" + minuteBucket)
    private val rateLimitCounters = ConcurrentHashMap<String, AtomicInteger>()

    // Track dispatched messages
    private val dispatchLog = ConcurrentHashMap<String, WhatsAppDispatchResult>()

    /**
     * Registers explicit opt-in consent for a parent or staff member.
     */
    fun registerConsent(record: WhatsAppConsentRecord) {
        val key = "${record.tenantId}:${record.recipientPhone}"
        consentRegistry[key] = record
    }

    /**
     * Revokes consent upon recipient opt-out.
     */
    fun revokeConsent(tenantId: String, recipientPhone: String) {
        val key = "$tenantId:$recipientPhone"
        val existing = consentRegistry[key]
        if (existing != null) {
            consentRegistry[key] = existing.copy(hasOptedIn = false)
        }
    }

    /**
     * Checks whether valid opt-in consent exists for this phone number.
     */
    fun hasConsent(tenantId: String, recipientPhone: String): Boolean {
        val key = "$tenantId:$recipientPhone"
        return consentRegistry[key]?.hasOptedIn ?: false
    }

    /**
     * Renders standard approved template text from parameter map.
     */
    fun renderTemplate(
        templateType: WhatsAppTemplateType,
        recipientName: String,
        parameters: Map<String, String>,
        schoolName: String = "St. Xavier's International Academy"
    ): String {
        return when (templateType) {
            WhatsAppTemplateType.ATTENDANCE_ABSENT_ALERT -> {
                val student = parameters["studentName"] ?: "your ward"
                val date = parameters["date"] ?: "today"
                "Dear Parent ($recipientName), this is an official alert from $schoolName: $student was marked ABSENT for Morning Assembly on $date. If this absence is an excused medical leave, please update the school office portal."
            }
            WhatsAppTemplateType.FEE_REMINDER_DUE -> {
                val term = parameters["term"] ?: "Current Term"
                val amount = parameters["amount"] ?: ""
                val dueDate = parameters["dueDate"] ?: ""
                "Dear Parent ($recipientName), fee invoice for $term amounting to INR $amount is due on $dueDate at $schoolName. Please use the secure institutional cloud portal to remit dues online."
            }
            WhatsAppTemplateType.EXAM_REPORT_PUBLISHED -> {
                val exam = parameters["examName"] ?: "Term Exam"
                val grade = parameters["grade"] ?: ""
                "Dear Parent ($recipientName), official academic report cards for $exam have been certified and published by $schoolName. Clearance Level: $grade."
            }
            WhatsAppTemplateType.EMERGENCY_CIRCULAR -> {
                val title = parameters["title"] ?: "Emergency Notice"
                val desc = parameters["description"] ?: ""
                "CRITICAL INSTITUTIONAL NOTICE from $schoolName: $title - $desc. Please check the institutional app."
            }
            WhatsAppTemplateType.ADMISSION_CONFIRMATION -> {
                val appNo = parameters["applicationNumber"] ?: ""
                "Greetings from $schoolName: Application $appNo has been officially received and queued for document verification."
            }
        }
    }

    /**
     * Dispatches message via official WhatsApp Business API or falls back to manual intent.
     */
    suspend fun dispatchMessage(
        request: WhatsAppMessageRequest,
        schoolName: String = "St. Xavier's International Academy",
        apiKeyConfigured: Boolean = true
    ): WhatsAppDispatchResult {
        val rendered = renderTemplate(
            templateType = request.templateType,
            recipientName = request.recipientName,
            parameters = request.templateParameters,
            schoolName = schoolName
        )

        // If official API preferred, verify consent and rate limit
        if (request.preferredMode == WhatsAppDeliveryMode.OFFICIAL_BUSINESS_API && apiKeyConfigured) {
            // 1. Consent verification
            if (!hasConsent(request.tenantId, request.recipientPhone)) {
                val result = WhatsAppDispatchResult(
                    isSuccess = false,
                    messageId = request.messageId,
                    deliveryMode = WhatsAppDeliveryMode.OFFICIAL_BUSINESS_API,
                    status = WhatsAppDeliveryStatus.CONSENT_REQUIRED,
                    renderedText = rendered,
                    errorReason = "Recipient '${request.recipientPhone}' has not opted in to official WhatsApp notifications"
                )
                dispatchLog[request.messageId] = result
                return result
            }

            // 2. Rate limiting check
            val currentMinute = System.currentTimeMillis() / 60000L
            val rateKey = "${request.tenantId}:$currentMinute"
            val counter = rateLimitCounters.computeIfAbsent(rateKey) { AtomicInteger(0) }
            if (counter.incrementAndGet() > maxMessagesPerMinutePerTenant) {
                val result = WhatsAppDispatchResult(
                    isSuccess = false,
                    messageId = request.messageId,
                    deliveryMode = WhatsAppDeliveryMode.OFFICIAL_BUSINESS_API,
                    status = WhatsAppDeliveryStatus.RATE_LIMITED,
                    renderedText = rendered,
                    errorReason = "Rate limit exceeded ($maxMessagesPerMinutePerTenant msgs/min for tenant ${request.tenantId})"
                )
                dispatchLog[request.messageId] = result
                return result
            }

            // 3. Simulate official server API dispatch
            kotlinx.coroutines.delay(100)
            val result = WhatsAppDispatchResult(
                isSuccess = true,
                messageId = request.messageId,
                deliveryMode = WhatsAppDeliveryMode.OFFICIAL_BUSINESS_API,
                status = WhatsAppDeliveryStatus.DELIVERED,
                renderedText = rendered,
                externalMessageId = "wamid.HBgM${System.currentTimeMillis()}"
            )
            dispatchLog[request.messageId] = result
            return result
        } else {
            // Fallback manual sharing mode
            val result = WhatsAppDispatchResult(
                isSuccess = true,
                messageId = request.messageId,
                deliveryMode = WhatsAppDeliveryMode.MANUAL_INTENT_FALLBACK,
                status = WhatsAppDeliveryStatus.SENT,
                renderedText = rendered
            )
            dispatchLog[request.messageId] = result
            return result
        }
    }

    /**
     * Builds standard Android ACTION_VIEW intent for manual WhatsApp share fallback.
     */
    fun buildManualIntent(phone: String, messageText: String): Intent {
        val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
        val encodedMessage = URLEncoder.encode(messageText, "UTF-8")
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
        return Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getDispatchResult(messageId: String): WhatsAppDispatchResult? = dispatchLog[messageId]
}
