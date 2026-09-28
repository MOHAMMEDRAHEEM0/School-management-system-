package com.example.payment

import java.util.UUID

// =============================================================================
// PRODUCTION MULTI-GATEWAY PAYMENT SUBSYSTEM MODELS
// =============================================================================

enum class PaymentGatewayProvider(val displayName: String) {
    RAZORPAY("Razorpay Payment Gateway"),
    CASHFREE("Cashfree Payments"),
    STRIPE("Stripe Connect"),
    PAYU("PayU Payments")
}

enum class PaymentEnvironment {
    TEST_SANDBOX,
    PRODUCTION
}

enum class PaymentStatus {
    CREATED,
    PENDING,
    AUTHORIZED,
    CAPTURED_SUCCESS,
    FAILED,
    PARTIALLY_REFUNDED,
    REFUNDED
}

/**
 * Domain separation between school fee collections and SaaS subscription billing.
 */
enum class OrderDomain {
    SCHOOL_FEE,
    SAAS_SUBSCRIPTION
}

/**
 * Institutional SaaS Subscription Plans.
 */
enum class SaaSPlan(
    val planId: String,
    val planName: String,
    val pricePaise: Long,
    val billingCycleDays: Int
) {
    STARTER("PLAN-STARTER-ANNUAL", "Starter Campus (up to 500 scholars)", 4999900L, 365),
    GROWTH("PLAN-GROWTH-ANNUAL", "Growth Campus (up to 2,000 scholars)", 9999900L, 365),
    ENTERPRISE("PLAN-ENTERPRISE-ANNUAL", "Enterprise Multi-Campus Autonomous Board", 19999900L, 365)
}

/**
 * Universal UPI Payment Intent Descriptor.
 */
data class UpiPaymentIntent(
    val payeeVpa: String, // e.g. "fees.stxaviers@icici" or "billing@saasplatform"
    val payeeName: String,
    val transactionRefId: String, // internal order ID
    val transactionNote: String,
    val amountRupees: Double,
    val currency: String = "INR",
    val deepLinkUri: String
) {
    companion object {
        fun buildUpiUri(
            payeeVpa: String,
            payeeName: String,
            orderId: String,
            amountRupees: Double,
            note: String
        ): String {
            val encodedName = java.net.URLEncoder.encode(payeeName, "UTF-8")
            val encodedNote = java.net.URLEncoder.encode(note, "UTF-8")
            val formattedAmount = String.format(java.util.Locale.US, "%.2f", amountRupees)
            return "upi://pay?pa=$payeeVpa&pn=$encodedName&mc=8211&tid=$orderId&tr=$orderId&tn=$encodedNote&am=$formattedAmount&cu=INR"
        }
    }
}

/**
 * Request to create an authoritative school fee payment order.
 */
data class CreateFeeOrderRequest(
    val tenantId: String,
    val studentId: String,
    val studentName: String,
    val feeRecordId: String,
    val term: String,
    val amountPaise: Long,
    val payerUserId: String,
    val payerName: String,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val provider: PaymentGatewayProvider = PaymentGatewayProvider.RAZORPAY
)

/**
 * Request to create an institutional SaaS subscription payment order.
 */
data class CreateSaaSOrderRequest(
    val tenantId: String,
    val institutionName: String,
    val plan: SaaSPlan,
    val payerUserId: String,
    val payerName: String,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val provider: PaymentGatewayProvider = PaymentGatewayProvider.RAZORPAY
)

/**
 * Double-Entry Accounting Ledger Entry.
 */
enum class LedgerAccountType {
    CASH_AT_BANK,
    FEES_RECEIVABLE,
    ESCROW_CLEARING,
    PLATFORM_REVENUE,
    REFUND_CLEARING
}

data class DoubleEntryRecord(
    val debitAccount: LedgerAccountType,
    val creditAccount: LedgerAccountType,
    val amountPaise: Long,
    val domain: OrderDomain,
    val orderId: String,
    val invoiceId: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Server-authoritative fee payment order.
 * Immutable invoice and order records scoped strictly to a single institution tenant.
 */
data class FeePaymentOrder(
    val internalOrderId: String = "ORD-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6).uppercase()}",
    val internalInvoiceId: String,
    val tenantId: String,
    val studentId: String,
    val studentName: String,
    val feeRecordId: String,
    val term: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val receiptNumber: String = "RCP-${System.currentTimeMillis().toString().takeLast(6)}",
    val gatewayProvider: PaymentGatewayProvider = PaymentGatewayProvider.RAZORPAY,
    val gatewayOrderId: String? = null,
    val idempotencyKey: String,
    val status: PaymentStatus = PaymentStatus.CREATED,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000L), // 30 minutes
    val completedAt: Long? = null,
    val gatewayPaymentId: String? = null,
    val failureReason: String? = null,
    val refundedAmountPaise: Long = 0L
) {
    val amountRupees: Double get() = amountPaise / 100.0
    val isExpired: Boolean get() = System.currentTimeMillis() > expiresAt && (status == PaymentStatus.CREATED || status == PaymentStatus.PENDING)
}

/**
 * Verification request submitted to backend after client checkout callback.
 * Never trust client callback alone without backend verification!
 */
data class PaymentVerificationRequest(
    val internalOrderId: String,
    val gatewayOrderId: String,
    val gatewayPaymentId: String,
    val gatewaySignature: String,
    val tenantId: String
)

data class PaymentVerificationResult(
    val isVerified: Boolean,
    val internalOrderId: String,
    val gatewayPaymentId: String,
    val amountPaise: Long,
    val status: PaymentStatus,
    val receiptNumber: String? = null,
    val failureMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Inbound webhook event payload from payment aggregator.
 */
data class GatewayWebhookEvent(
    val eventId: String,
    val eventType: String, // e.g. "payment.captured", "payment.failed", "refund.processed"
    val gatewayProvider: PaymentGatewayProvider,
    val payloadJson: String,
    val signatureHeader: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class WebhookProcessingResult(
    val isSuccess: Boolean,
    val isDuplicate: Boolean = false,
    val eventId: String,
    val internalOrderId: String?,
    val resultingStatus: PaymentStatus?,
    val message: String
)

data class RefundRequest(
    val internalOrderId: String,
    val refundAmountPaise: Long,
    val reason: String,
    val requestedByUserId: String,
    val idempotencyKey: String = UUID.randomUUID().toString()
)

data class GatewayTransactionRecord(
    val gatewayPaymentId: String,
    val gatewayOrderId: String,
    val amountPaise: Long,
    val status: String,
    val currency: String = "INR",
    val settledAt: Long = System.currentTimeMillis()
)

data class ReconciliationRecord(
    val internalOrderId: String,
    val tenantId: String,
    val feeRecordId: String,
    val expectedAmountPaise: Long,
    val capturedAmountPaise: Long,
    val gatewayStatus: PaymentStatus,
    val schoolLedgerStatus: String,
    val isMismatch: Boolean,
    val mismatchNotes: String = ""
)

class PaymentSecurityException(message: String) : SecurityException(message)
class PaymentDuplicateException(message: String) : IllegalStateException(message)
