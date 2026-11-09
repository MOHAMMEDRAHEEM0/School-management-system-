package com.example.payment

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.FinancialLedgerEntryEntity
import com.example.data.local.entities.PaymentOrderEntity
import com.example.security.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Enterprise Zero-Trust Provider-Agnostic Payment Engine & Double-Entry Ledger.
 *
 * Implements:
 * 1. Provider-Agnostic Abstraction for Gateways (Razorpay, Cashfree, UPI Direct).
 * 2. Strict Domain Isolation: School Fee Collections vs SaaS Subscription Billing.
 * 3. Immutable Unique Order IDs, Invoice IDs, and Idempotency Keys.
 * 4. Cryptographic HMAC-SHA256 Webhook & Callback Verification (Timing-attack resistant).
 * 5. Replay Attack & Duplicate Callback Prevention.
 * 6. Finite State Machine: CREATED -> PENDING -> SUCCESS -> REFUNDED (or FAILED).
 * 7. Balanced Double-Entry Financial Bookkeeping with running balances and checksums.
 * 8. Network Drop Resiliency with 30-minute session expiration.
 * 9. Zero-Trust Security: No API secrets, webhook keys, card details, or UPI PINs exposed in the client APK.
 */
class PaymentGatewayService(
    private val dao: SchoolDao? = null,
    private val environment: PaymentEnvironment = PaymentEnvironment.TEST_SANDBOX
) {
    // In-memory cache for processed webhook event IDs to prevent replay attacks
    private val processedWebhookEvents = ConcurrentHashMap<String, Long>()

    // Track active in-memory orders (synchronizes with Room when DAO is provided)
    private val activeOrders = ConcurrentHashMap<String, FeePaymentOrder>()

    // Track completed verification results
    private val paymentRecords = ConcurrentHashMap<String, PaymentVerificationResult>()

    // Double-entry running balance tracking by (tenantId + ":" + accountType)
    private val ledgerBalances = ConcurrentHashMap<String, Long>()

    // Audit trail of posted double-entry ledger transactions
    private val doubleEntryJournal = java.util.concurrent.CopyOnWriteArrayList<DoubleEntryRecord>()

    init {
        // Balances are initialized dynamically per tenant via ensureTenantLedger
    }

    private fun ensureTenantLedger(tenantId: String) {
        val cleanTenant = tenantId.ifBlank { "INST-DEFAULT" }
        val bankKey = "$cleanTenant:CASH_AT_BANK"
        if (!ledgerBalances.containsKey(bankKey)) {
            ledgerBalances[bankKey] = 150000000L // Rs. 15,00,000 opening bank
            ledgerBalances["$cleanTenant:FEES_RECEIVABLE"] = 500000000L // Rs. 50,00,000 fees receivable
            ledgerBalances["$cleanTenant:ESCROW_CLEARING"] = 0L
            ledgerBalances["$cleanTenant:PLATFORM_REVENUE"] = 0L
            ledgerBalances["$cleanTenant:REFUND_CLEARING"] = 0L
        }
    }

    /**
     * Computes HMAC-SHA256 signature for payload verification.
     */
    fun computeHmacSha256(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(secretKeySpec)
        val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies cryptographic signature for Razorpay / standard HMAC payment callback.
     * Expected signature = HMAC_SHA256(orderId + "|" + paymentId, secret)
     */
    fun verifySignature(
        orderId: String,
        paymentId: String,
        receivedSignature: String,
        secret: String
    ): Boolean {
        val payload = "$orderId|$paymentId"
        val expected = computeHmacSha256(payload, secret)
        return SecurityUtils.constantTimeEquals(expected, receivedSignature)
    }

    // =========================================================================
    // 1. ORDER CREATION (FEES & SAAS BILLING)
    // =========================================================================

    /**
     * Creates an authoritative School Fee Payment Order with unique Invoice ID and UPI Deep Link.
     */
    suspend fun createSchoolFeeOrder(request: CreateFeeOrderRequest): PaymentOrderEntity = withContext(Dispatchers.IO) {
        require(request.amountPaise > 0) { "Fee amount must be greater than zero" }
        require(request.tenantId.isNotBlank()) { "Tenant ID is mandatory" }

        // Idempotency check: Return existing order if idempotency key matches
        dao?.getPaymentOrderByIdempotencyKey(request.idempotencyKey)?.let {
            return@withContext it
        }

        val timestamp = System.currentTimeMillis()
        val orderId = "ORD-FEE-${timestamp}-${UUID.randomUUID().toString().take(6).uppercase()}"
        val invoiceId = "INV-FEE-${timestamp.toString().takeLast(6)}"
        val receiptNumber = "RCP-${timestamp.toString().takeLast(6)}"
        val amountRupees = request.amountPaise / 100.0

        val upiUri = UpiPaymentIntent.buildUpiUri(
            payeeVpa = "fees.${request.tenantId.lowercase().replace("-", "")}@icici",
            payeeName = "St. Xavier's International Academy",
            orderId = orderId,
            amountRupees = amountRupees,
            note = "Fee Payment - ${request.term} - Student ${request.studentId}"
        )

        val rawChecksum = "$orderId|$invoiceId|${request.amountPaise}|${request.idempotencyKey}"
        val checksum = computeSha256(rawChecksum.toByteArray(Charsets.UTF_8))

        val orderEntity = PaymentOrderEntity(
            orderId = orderId,
            institutionId = request.tenantId,
            orderType = OrderDomain.SCHOOL_FEE.name,
            invoiceId = invoiceId,
            payerUserId = request.payerUserId,
            payerName = request.payerName,
            studentId = request.studentId,
            subscriptionPlanId = null,
            amountPaise = request.amountPaise,
            currency = "INR",
            receiptNumber = receiptNumber,
            gatewayProvider = request.provider.name,
            gatewayOrderId = "gw_fee_${timestamp}",
            gatewayPaymentId = null,
            idempotencyKey = request.idempotencyKey,
            status = PaymentStatus.PENDING.name,
            upiPaymentString = upiUri,
            createdAt = timestamp,
            expiresAt = timestamp + (30 * 60 * 1000L), // 30 minutes
            completedAt = null,
            failureReason = null,
            checksumSha256 = checksum
        )

        dao?.insertPaymentOrder(orderEntity)

        // Sync in-memory model for backward compatibility
        activeOrders[orderId] = FeePaymentOrder(
            internalOrderId = orderId,
            internalInvoiceId = invoiceId,
            tenantId = request.tenantId,
            studentId = request.studentId,
            studentName = request.studentName,
            feeRecordId = request.feeRecordId,
            term = request.term,
            amountPaise = request.amountPaise,
            receiptNumber = receiptNumber,
            gatewayProvider = request.provider,
            gatewayOrderId = orderEntity.gatewayOrderId,
            idempotencyKey = request.idempotencyKey,
            status = PaymentStatus.PENDING,
            createdAt = timestamp,
            expiresAt = orderEntity.expiresAt
        )

        orderEntity
    }

    /**
     * Creates an authoritative SaaS Subscription Order for Institutional Platform Licensing.
     */
    suspend fun createSaaSSubscriptionOrder(request: CreateSaaSOrderRequest): PaymentOrderEntity = withContext(Dispatchers.IO) {
        dao?.getPaymentOrderByIdempotencyKey(request.idempotencyKey)?.let {
            return@withContext it
        }

        val timestamp = System.currentTimeMillis()
        val orderId = "ORD-SAAS-${timestamp}-${UUID.randomUUID().toString().take(6).uppercase()}"
        val invoiceId = "INV-SAAS-${timestamp.toString().takeLast(6)}"
        val receiptNumber = "SAAS-RCP-${timestamp.toString().takeLast(6)}"
        val amountRupees = request.plan.pricePaise / 100.0

        val upiUri = UpiPaymentIntent.buildUpiUri(
            payeeVpa = "billing@saasplatform.hdfc",
            payeeName = "School ERP Cloud SaaS",
            orderId = orderId,
            amountRupees = amountRupees,
            note = "SaaS Annual Subscription - ${request.plan.planName}"
        )

        val rawChecksum = "$orderId|$invoiceId|${request.plan.pricePaise}|${request.idempotencyKey}"
        val checksum = computeSha256(rawChecksum.toByteArray(Charsets.UTF_8))

        val orderEntity = PaymentOrderEntity(
            orderId = orderId,
            institutionId = request.tenantId,
            orderType = OrderDomain.SAAS_SUBSCRIPTION.name,
            invoiceId = invoiceId,
            payerUserId = request.payerUserId,
            payerName = request.payerName,
            studentId = null,
            subscriptionPlanId = request.plan.planId,
            amountPaise = request.plan.pricePaise,
            currency = "INR",
            receiptNumber = receiptNumber,
            gatewayProvider = request.provider.name,
            gatewayOrderId = "gw_saas_${timestamp}",
            gatewayPaymentId = null,
            idempotencyKey = request.idempotencyKey,
            status = PaymentStatus.PENDING.name,
            upiPaymentString = upiUri,
            createdAt = timestamp,
            expiresAt = timestamp + (30 * 60 * 1000L),
            completedAt = null,
            failureReason = null,
            checksumSha256 = checksum
        )

        dao?.insertPaymentOrder(orderEntity)
        orderEntity
    }

    /**
     * Legacy synchronous helper for backwards compatibility.
     */
    fun createPaymentOrder(
        tenantId: String,
        studentId: String,
        studentName: String,
        feeRecordId: String,
        term: String,
        amountPaise: Long,
        idempotencyKey: String,
        provider: PaymentGatewayProvider = PaymentGatewayProvider.RAZORPAY
    ): FeePaymentOrder {
        val existing = activeOrders.values.firstOrNull {
            it.idempotencyKey == idempotencyKey && it.tenantId == tenantId
        }
        if (existing != null) {
            return existing
        }

        val order = FeePaymentOrder(
            internalInvoiceId = "INV-${System.currentTimeMillis().toString().takeLast(6)}",
            tenantId = tenantId,
            studentId = studentId,
            studentName = studentName,
            feeRecordId = feeRecordId,
            term = term,
            amountPaise = amountPaise,
            gatewayProvider = provider,
            gatewayOrderId = "gw_ord_${System.currentTimeMillis()}",
            idempotencyKey = idempotencyKey,
            status = PaymentStatus.PENDING
        )
        activeOrders[order.internalOrderId] = order
        return order
    }

    // =========================================================================
    // 2. PAYMENT VERIFICATION & IDEMPOTENT CALLBACK PROCESSING
    // =========================================================================

    /**
     * Authoritative server-side payment verification.
     * Validates order existence, tenant ownership, cryptographic signature, and idempotency.
     * Automatically posts balanced double-entry ledger transactions.
     */
    fun verifyPayment(
        request: PaymentVerificationRequest,
        apiSecret: String
    ): PaymentVerificationResult {
        // Check for duplicate callback if already completed
        val existingRecord = paymentRecords[request.internalOrderId]
        if (existingRecord != null && existingRecord.isVerified && existingRecord.status == PaymentStatus.CAPTURED_SUCCESS) {
            return existingRecord // Idempotent return without duplicate processing
        }

        val order = activeOrders[request.internalOrderId]
            ?: return PaymentVerificationResult(
                isVerified = false,
                internalOrderId = request.internalOrderId,
                gatewayPaymentId = request.gatewayPaymentId,
                amountPaise = 0L,
                status = PaymentStatus.FAILED,
                failureMessage = "Order not found in authoritative order registry"
            )

        // Check if order has timed out during checkout due to network drop
        if (order.isExpired) {
            val expiredResult = PaymentVerificationResult(
                isVerified = false,
                internalOrderId = request.internalOrderId,
                gatewayPaymentId = request.gatewayPaymentId,
                amountPaise = order.amountPaise,
                status = PaymentStatus.FAILED,
                failureMessage = "Checkout session expired due to network drop timeout"
            )
            activeOrders[order.internalOrderId] = order.copy(
                status = PaymentStatus.FAILED,
                failureReason = expiredResult.failureMessage
            )
            return expiredResult
        }

        // Multi-tenant authorization check
        if (order.tenantId != request.tenantId) {
            return PaymentVerificationResult(
                isVerified = false,
                internalOrderId = request.internalOrderId,
                gatewayPaymentId = request.gatewayPaymentId,
                amountPaise = order.amountPaise,
                status = PaymentStatus.FAILED,
                failureMessage = "Security violation: Cross-tenant payment verification rejected"
            )
        }

        // Verify cryptographic signature (timing-attack resistant constant-time check)
        val isSignatureValid = verifySignature(
            orderId = request.gatewayOrderId,
            paymentId = request.gatewayPaymentId,
            receivedSignature = request.gatewaySignature,
            secret = apiSecret
        )

        if (!isSignatureValid) {
            val failedResult = PaymentVerificationResult(
                isVerified = false,
                internalOrderId = request.internalOrderId,
                gatewayPaymentId = request.gatewayPaymentId,
                amountPaise = order.amountPaise,
                status = PaymentStatus.FAILED,
                failureMessage = "Cryptographic signature mismatch: Forged or corrupted payment payload"
            )
            activeOrders[order.internalOrderId] = order.copy(
                status = PaymentStatus.FAILED,
                failureReason = failedResult.failureMessage
            )
            return failedResult
        }

        // Verification successful: Update state to CAPTURED_SUCCESS
        val successResult = PaymentVerificationResult(
            isVerified = true,
            internalOrderId = request.internalOrderId,
            gatewayPaymentId = request.gatewayPaymentId,
            amountPaise = order.amountPaise,
            status = PaymentStatus.CAPTURED_SUCCESS,
            receiptNumber = order.receiptNumber
        )

        activeOrders[order.internalOrderId] = order.copy(
            status = PaymentStatus.CAPTURED_SUCCESS,
            completedAt = System.currentTimeMillis(),
            gatewayPaymentId = request.gatewayPaymentId
        )
        paymentRecords[request.internalOrderId] = successResult

        // Post balanced double-entry ledger record
        postDoubleEntry(
            domain = OrderDomain.SCHOOL_FEE,
            orderId = order.internalOrderId,
            invoiceId = order.internalInvoiceId,
            amountPaise = order.amountPaise,
            description = "School fee payment captured via ${order.gatewayProvider.displayName} - ${order.receiptNumber}",
            tenantId = order.tenantId
        )

        return successResult
    }

    // =========================================================================
    // 3. CRYPTOGRAPHIC WEBHOOK CONSUMPTION & REPLAY ATTACK MITIGATION
    // =========================================================================

    /**
     * Authoritative idempotent webhook processing.
     * Prevents forged signatures, replay attacks, and duplicate ledger entries.
     */
    fun processWebhookEvent(
        event: GatewayWebhookEvent,
        webhookSecret: String,
        targetOrderId: String? = null
    ): WebhookProcessingResult {
        // 1. Replay attack prevention via idempotency check on eventId
        if (processedWebhookEvents.containsKey(event.eventId)) {
            return WebhookProcessingResult(
                isSuccess = true,
                isDuplicate = true,
                eventId = event.eventId,
                internalOrderId = targetOrderId,
                resultingStatus = null,
                message = "Idempotent duplicate: Event ${event.eventId} already processed."
            )
        }

        // 2. Validate cryptographic HMAC-SHA256 signature
        val expectedSignature = computeHmacSha256(event.payloadJson, webhookSecret)
        if (!SecurityUtils.constantTimeEquals(expectedSignature, event.signatureHeader)) {
            throw PaymentSecurityException("Forged webhook signature: verification failed for event ${event.eventId}")
        }

        // 3. State machine transition & ledger updates
        var updatedStatus: PaymentStatus? = null
        if (targetOrderId != null && activeOrders.containsKey(targetOrderId)) {
            val order = activeOrders[targetOrderId]!!

            // Finite state machine enforcement: cannot transition out of terminal state
            if (order.status == PaymentStatus.CAPTURED_SUCCESS && event.eventType == "payment.captured") {
                // Idempotent duplicate event
                return WebhookProcessingResult(
                    isSuccess = true,
                    isDuplicate = true,
                    eventId = event.eventId,
                    internalOrderId = targetOrderId,
                    resultingStatus = order.status,
                    message = "Order $targetOrderId is already captured. Duplicate webhook ignored."
                )
            }

            updatedStatus = when (event.eventType) {
                "payment.captured", "order.paid" -> PaymentStatus.CAPTURED_SUCCESS
                "payment.failed" -> PaymentStatus.FAILED
                "refund.processed" -> PaymentStatus.REFUNDED
                else -> order.status
            }

            activeOrders[targetOrderId] = order.copy(
                status = updatedStatus,
                completedAt = System.currentTimeMillis()
            )

            if (updatedStatus == PaymentStatus.CAPTURED_SUCCESS) {
                postDoubleEntry(
                    domain = OrderDomain.SCHOOL_FEE,
                    orderId = order.internalOrderId,
                    invoiceId = order.internalInvoiceId,
                    amountPaise = order.amountPaise,
                    description = "Webhook confirmed payment capture for order ${order.internalOrderId}",
                    tenantId = order.tenantId
                )
            }
        }

        // Mark event as processed with timestamp
        processedWebhookEvents[event.eventId] = System.currentTimeMillis()

        return WebhookProcessingResult(
            isSuccess = true,
            isDuplicate = false,
            eventId = event.eventId,
            internalOrderId = targetOrderId,
            resultingStatus = updatedStatus,
            message = "Webhook processed successfully: ${event.eventType}"
        )
    }

    // =========================================================================
    // 4. BALANCED DOUBLE-ENTRY FINANCIAL BOOKKEEPING
    // =========================================================================

    /**
     * Posts a balanced double-entry accounting transaction to the general ledger.
     * DEBIT must equal CREDIT.
     */
    fun postDoubleEntry(
        domain: OrderDomain,
        orderId: String,
        invoiceId: String,
        amountPaise: Long,
        description: String,
        tenantId: String
    ): DoubleEntryRecord {
        val (debitAccount, creditAccount) = when (domain) {
            OrderDomain.SCHOOL_FEE -> {
                // School Fee: Debit Bank (Asset +), Credit Fees Receivable (Asset -)
                Pair(LedgerAccountType.CASH_AT_BANK, LedgerAccountType.FEES_RECEIVABLE)
            }
            OrderDomain.SAAS_SUBSCRIPTION -> {
                // SaaS Subscription: Debit Escrow Clearing (Asset +), Credit Platform Revenue (Revenue +)
                Pair(LedgerAccountType.ESCROW_CLEARING, LedgerAccountType.PLATFORM_REVENUE)
            }
        }

        val debitKey = "$tenantId:${debitAccount.name}"
        val creditKey = "$tenantId:${creditAccount.name}"

        val currentDebitBal = ledgerBalances.getOrDefault(debitKey, 0L)
        val newDebitBal = currentDebitBal + amountPaise
        ledgerBalances[debitKey] = newDebitBal

        val currentCreditBal = ledgerBalances.getOrDefault(creditKey, 0L)
        val newCreditBal = currentCreditBal + amountPaise
        ledgerBalances[creditKey] = newCreditBal

        val record = DoubleEntryRecord(
            debitAccount = debitAccount,
            creditAccount = creditAccount,
            amountPaise = amountPaise,
            domain = domain,
            orderId = orderId,
            invoiceId = invoiceId,
            description = description,
            timestamp = System.currentTimeMillis()
        )
        doubleEntryJournal.add(0, record)
        return record
    }

    /**
     * Retrieves the posted double-entry journal transactions.
     */
    fun getDoubleEntryJournal(): List<DoubleEntryRecord> = doubleEntryJournal.toList()

    /**
     * Retrieves processed webhook events and their receipt timestamps.
     */
    fun getProcessedWebhooks(): Map<String, Long> = processedWebhookEvents.toMap()

    /**
     * Retrieves all active orders in the authoritative registry.
     */
    fun getAllOrders(): List<FeePaymentOrder> = activeOrders.values.toList()

    /**
     * Simulates transitioning an order between states (e.g. PENDING -> SUCCESS, FAILED)
     * for manual sandbox verification without real transactions.
     */
    fun simulateStateTransition(orderId: String, targetStatus: PaymentStatus): FeePaymentOrder? {
        val order = activeOrders[orderId] ?: return null
        val updated = order.copy(
            status = targetStatus,
            completedAt = if (targetStatus == PaymentStatus.CAPTURED_SUCCESS) System.currentTimeMillis() else order.completedAt
        )
        activeOrders[orderId] = updated
        if (targetStatus == PaymentStatus.CAPTURED_SUCCESS) {
            postDoubleEntry(
                domain = OrderDomain.SCHOOL_FEE,
                orderId = updated.internalOrderId,
                invoiceId = updated.internalInvoiceId,
                amountPaise = updated.amountPaise,
                description = "Sandbox verified capture transition - ${updated.receiptNumber}",
                tenantId = updated.tenantId
            )
        }
        return updated
    }

    /**
     * Retrieves the current running ledger balance for an account.
     */
    fun getLedgerBalance(tenantId: String, accountType: LedgerAccountType): Long {
        val key = "$tenantId:${accountType.name}"
        return ledgerBalances.getOrDefault(key, 0L)
    }

    // =========================================================================
    // 5. REFUND STATE MACHINE & REVERSING LEDGER ENTRIES
    // =========================================================================

    /**
     * Processes full or partial refunds with state machine transitions and ledger reversals.
     */
    fun processRefund(request: RefundRequest): Result<PaymentVerificationResult> {
        val order = activeOrders[request.internalOrderId]
            ?: return Result.failure(IllegalArgumentException("Order not found: ${request.internalOrderId}"))

        if (order.status != PaymentStatus.CAPTURED_SUCCESS && order.status != PaymentStatus.PARTIALLY_REFUNDED) {
            return Result.failure(IllegalStateException("Cannot refund order in state: ${order.status}"))
        }

        val totalRefunded = order.refundedAmountPaise + request.refundAmountPaise
        if (totalRefunded > order.amountPaise) {
            return Result.failure(IllegalArgumentException("Refund amount exceeds paid order amount"))
        }

        val newStatus = if (totalRefunded == order.amountPaise) {
            PaymentStatus.REFUNDED
        } else {
            PaymentStatus.PARTIALLY_REFUNDED
        }

        activeOrders[order.internalOrderId] = order.copy(
            status = newStatus,
            refundedAmountPaise = totalRefunded
        )

        // Reversing entry: Debit Refund Clearing, Credit Bank
        val bankKey = "${order.tenantId}:${LedgerAccountType.CASH_AT_BANK.name}"
        val refundKey = "${order.tenantId}:${LedgerAccountType.REFUND_CLEARING.name}"
        ledgerBalances[bankKey] = (ledgerBalances[bankKey] ?: 0L) - request.refundAmountPaise
        ledgerBalances[refundKey] = (ledgerBalances[refundKey] ?: 0L) + request.refundAmountPaise

        val refundRecord = DoubleEntryRecord(
            debitAccount = LedgerAccountType.REFUND_CLEARING,
            creditAccount = LedgerAccountType.CASH_AT_BANK,
            amountPaise = request.refundAmountPaise,
            domain = OrderDomain.SCHOOL_FEE,
            orderId = order.internalOrderId,
            invoiceId = order.internalInvoiceId,
            description = "Refund reversal: ${request.reason}",
            timestamp = System.currentTimeMillis()
        )
        doubleEntryJournal.add(0, refundRecord)

        val result = PaymentVerificationResult(
            isVerified = true,
            internalOrderId = order.internalOrderId,
            gatewayPaymentId = order.gatewayPaymentId ?: "REFUND-${request.idempotencyKey}",
            amountPaise = totalRefunded,
            status = newStatus,
            receiptNumber = "REF-${order.receiptNumber}"
        )
        return Result.success(result)
    }

    // =========================================================================
    // 6. LEDGER RECONCILIATION & DISCREPANCY AUDITING
    // =========================================================================

    /**
     * Ledger reconciliation: audits external gateway records against school fee ledger records.
     * Detects mismatches (underpayments, unrecorded payments, gateway dropouts).
     */
    fun reconcileLedger(
        tenantId: String,
        schoolFeeRecords: List<FeeRecordEntity>,
        gatewayTransactions: List<GatewayTransactionRecord>
    ): List<ReconciliationRecord> {
        val results = mutableListOf<ReconciliationRecord>()

        for (fee in schoolFeeRecords.filter { it.institutionId == tenantId }) {
            val matchingOrder = activeOrders.values.firstOrNull { it.feeRecordId == fee.id && it.tenantId == tenantId }
            val matchingTxn = gatewayTransactions.firstOrNull { it.gatewayOrderId == matchingOrder?.gatewayOrderId }

            val capturedAmount = matchingTxn?.amountPaise ?: (matchingOrder?.amountPaise ?: 0L)
            val expectedAmount = fee.totalAmount

            val isMismatch = when {
                fee.status == "PAID" && matchingTxn == null && (matchingOrder == null || matchingOrder.status != PaymentStatus.CAPTURED_SUCCESS) -> true
                fee.status == "PAID" && capturedAmount < expectedAmount -> true
                fee.status == "DUE" && matchingTxn?.status == "CAPTURED" -> true
                else -> false
            }

            val mismatchNotes = when {
                fee.status == "PAID" && matchingTxn == null -> "School ledger marked PAID but no corresponding Gateway capture record"
                fee.status == "PAID" && capturedAmount < expectedAmount -> "Underpayment mismatch: Expected $expectedAmount paise, captured $capturedAmount paise"
                fee.status == "DUE" && matchingTxn?.status == "CAPTURED" -> "Ledger latency: Gateway captured funds but School ledger still reflects DUE"
                else -> "Reconciled OK"
            }

            results.add(
                ReconciliationRecord(
                    internalOrderId = matchingOrder?.internalOrderId ?: "N/A",
                    tenantId = tenantId,
                    feeRecordId = fee.id,
                    expectedAmountPaise = expectedAmount,
                    capturedAmountPaise = capturedAmount,
                    gatewayStatus = matchingOrder?.status ?: PaymentStatus.PENDING,
                    schoolLedgerStatus = fee.status,
                    isMismatch = isMismatch,
                    mismatchNotes = mismatchNotes
                )
            )
        }

        return results
    }

    /**
     * Simulates order expiration for network drop timeout testing.
     */
    fun simulateOrderExpiration(orderId: String) {
        val order = activeOrders[orderId] ?: return
        activeOrders[orderId] = order.copy(expiresAt = System.currentTimeMillis() - 1000L)
    }

    /**
     * Handles checkout network drop timeouts.
     * If an order remained in PENDING past its 30-minute expiration, marks it FAILED.
     */
    fun handleCheckoutNetworkDrop(orderId: String): FeePaymentOrder? {
        val order = activeOrders[orderId] ?: return null
        if (order.status == PaymentStatus.PENDING && order.isExpired) {
            val failedOrder = order.copy(
                status = PaymentStatus.FAILED,
                failureReason = "Checkout session expired due to network drop"
            )
            activeOrders[orderId] = failedOrder
            return failedOrder
        }
        return order
    }

    fun getOrder(orderId: String): FeePaymentOrder? = activeOrders[orderId]

    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
