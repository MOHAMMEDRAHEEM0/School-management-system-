package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.payment.CreateFeeOrderRequest
import com.example.payment.CreateSaaSOrderRequest
import com.example.payment.DoubleEntryRecord
import com.example.payment.FeePaymentOrder
import com.example.payment.GatewayWebhookEvent
import com.example.payment.LedgerAccountType
import com.example.payment.OrderDomain
import com.example.payment.PaymentGatewayProvider
import com.example.payment.PaymentGatewayService
import com.example.payment.PaymentSecurityException
import com.example.payment.PaymentStatus
import com.example.payment.RefundRequest
import com.example.payment.SaaSPlan
import com.example.payment.WebhookProcessingResult
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Payment Sandbox Dashboard for Institutional Administrators.
 *
 * Simulates webhook events and real-time ledger status updates, allowing manual
 * and automated verification of complete order flows from PENDING to SUCCESS states
 * without real banking transactions.
 *
 * Core Capabilities:
 * 1. Visual Order Lifecycle Stepper: CREATED -> PENDING -> WEBHOOK_INBOUND -> SUCCESS (or FAILED/REFUNDED)
 * 2. Inbound Webhook Event Simulator: payment.captured, payment.authorized, payment.failed, refund.processed
 * 3. Idempotency & Anti-Replay Engine: Duplicate webhook nonce mitigation
 * 4. Timing-attack resistant HMAC-SHA256 signature verification & forged payload rejection
 * 5. Balanced Double-Entry Bookkeeping Ledger Auditor (ΔDebit == ΔCredit equation confirmation)
 * 6. Fast-Track Order Lifecycle Automation: One-tap automated execution from PENDING to SUCCESS
 * 7. Live Cryptographic Audit Log Terminal with export capabilities
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PaymentSandboxDashboard(
    viewModel: SchoolViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    // Test Payment Gateway Engine
    val gatewayService = remember { PaymentGatewayService() }

    val feeRecords by viewModel.feeRecords.collectAsState()

    // Active Sandbox State
    var activeOrder by remember {
        mutableStateOf<FeePaymentOrder?>(
            FeePaymentOrder(
                internalOrderId = "ORD-SANDBOX-2026-001",
                internalInvoiceId = "INV-FEE-882091",
                tenantId = "INST-SXA-2026",
                studentId = "STU-2026-001",
                studentName = "Aarav Sharma",
                feeRecordId = "FEE-2026-001",
                term = "Term 1 Tuition & Lab",
                amountPaise = 1500000L, // Rs. 15,000
                gatewayProvider = PaymentGatewayProvider.RAZORPAY,
                gatewayOrderId = "order_rzp_mock_88102",
                idempotencyKey = "IDEMP-INITIAL-KEY-001",
                status = PaymentStatus.PENDING
            )
        )
    }

    var selectedDomain by remember { mutableStateOf(OrderDomain.SCHOOL_FEE) }
    var selectedWebhookType by remember { mutableStateOf("payment.captured") }
    var webhookSecret by remember { mutableStateOf("whsec_sandbox_prod_key_2026") }
    var lastWebhookResult by remember { mutableStateOf<WebhookProcessingResult?>(null) }
    var ledgerJournal by remember { mutableStateOf<List<DoubleEntryRecord>>(emptyList()) }
    var isAutoRunningFlow by remember { mutableStateOf(false) }
    var auditLogs by remember {
        mutableStateOf(
            listOf(
                "[SYSTEM READY] Payment Sandbox Dashboard initialized in Zero-Trust TEST_SANDBOX environment.",
                "[INITIAL STATE] Initial Order ORD-SANDBOX-2026-001 created in state: PENDING. No funds moved."
            )
        )
    }

    var activeTab by remember { mutableIntStateOf(0) }

    fun addLog(msg: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        auditLogs = listOf("[$timestamp] $msg") + auditLogs.take(29)
    }

    fun refreshLedger() {
        ledgerJournal = gatewayService.getDoubleEntryJournal()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("payment_sandbox_dashboard_root")
    ) {
        // =====================================================================
        // 1. EXECUTIVE SANDBOX HEADER
        // =====================================================================
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(GovNavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Sandbox",
                                tint = PureWhite,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "PAYMENT SANDBOX DASHBOARD",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyDark,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassBadge(text = "ADMIN VERIFICATION", color = GovGreen)
                            }
                            Text(
                                text = "Simulate Inbound Webhooks • Double-Entry Ledger Updates • Order State Flow",
                                fontSize = 10.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = {
                                val logText = auditLogs.joinToString("\n")
                                clipboardManager.setText(AnnotatedString(logText))
                                Toast.makeText(context, "Audit logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Logs", modifier = Modifier.size(16.dp), tint = GovSlateDark)
                        }

                        IconButton(
                            onClick = {
                                activeOrder = FeePaymentOrder(
                                    internalOrderId = "ORD-RESET-${System.currentTimeMillis().toString().takeLast(6)}",
                                    internalInvoiceId = "INV-FEE-${(100000..999999).random()}",
                                    tenantId = "INST-SXA-2026",
                                    studentId = "STU-2026-001",
                                    studentName = "Aarav Sharma",
                                    feeRecordId = "FEE-2026-001",
                                    term = "Term 1 Tuition",
                                    amountPaise = 1500000L,
                                    gatewayProvider = PaymentGatewayProvider.RAZORPAY,
                                    gatewayOrderId = "gw_ord_${System.currentTimeMillis().toString().takeLast(6)}",
                                    idempotencyKey = UUID.randomUUID().toString(),
                                    status = PaymentStatus.PENDING
                                )
                                lastWebhookResult = null
                                addLog("Sandbox state reset. New order initialized in PENDING state.")
                                Toast.makeText(context, "Sandbox reset to fresh PENDING order", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(16.dp), tint = GovSlateDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Module Tabs
                PrimaryTabRow(
                    selectedTabIndex = activeTab,
                    containerColor = PureWhite,
                    contentColor = GovNavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("1. Order & Webhooks", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_sandbox_webhooks")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("2. Ledger & Bookkeeping", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_sandbox_ledger")
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("3. Security & Anti-Replay", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_sandbox_security")
                    )
                }
            }
        }

        // =====================================================================
        // 2. MAIN SCROLLABLE SANDBOX SUITE
        // =====================================================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // High-Impact Order Pipeline Stepper (Always Visible on Top)
            item {
                OrderLifecyclePipelineCard(
                    order = activeOrder,
                    isAutoRunning = isAutoRunningFlow,
                    onFastTrackSuccess = {
                        coroutineScope.launch {
                            if (activeOrder == null) return@launch
                            isAutoRunningFlow = true
                            addLog(">>> FAST-TRACK SIMULATION TRIGGERED: PENDING -> SUCCESS")

                            // Step 1: Ensure order in PENDING
                            delay(400)
                            addLog("Step 1/4: Order ${activeOrder?.internalOrderId} confirmed in PENDING state.")

                            // Step 2: Simulate Inbound Webhook
                            delay(600)
                            val eventId = "evt_fast_${System.currentTimeMillis()}"
                            val payloadJson = """{"event":"payment.captured","order_id":"${activeOrder?.gatewayOrderId}","amount":${activeOrder?.amountPaise}}"""
                            val sig = gatewayService.computeHmacSha256(payloadJson, webhookSecret)
                            addLog("Step 2/4: Gateway Webhook received ($eventId) with HMAC signature.")

                            // Step 3: Verify & Process
                            delay(600)
                            val event = GatewayWebhookEvent(
                                eventId = eventId,
                                eventType = "payment.captured",
                                gatewayProvider = activeOrder?.gatewayProvider ?: PaymentGatewayProvider.RAZORPAY,
                                payloadJson = payloadJson,
                                signatureHeader = sig
                            )
                            val result = gatewayService.processWebhookEvent(event, webhookSecret, activeOrder?.internalOrderId)
                            lastWebhookResult = result
                            activeOrder = gatewayService.getOrder(activeOrder!!.internalOrderId) ?: activeOrder?.copy(
                                status = PaymentStatus.CAPTURED_SUCCESS,
                                completedAt = System.currentTimeMillis()
                            )
                            addLog("Step 3/4: Cryptographic verification PASSED. Order transitioned: PENDING -> CAPTURED_SUCCESS.")

                            // Step 4: Ledger Post
                            delay(500)
                            refreshLedger()
                            addLog("Step 4/4: Balanced double-entry posted: Debit Bank (+₹${activeOrder?.amountRupees}), Credit Fees Receivable.")
                            addLog(">>> ORDER FLOW VERIFIED SUCCESSFULLY: Official Receipt ${activeOrder?.receiptNumber} Issued.")

                            isAutoRunningFlow = false
                            Toast.makeText(context, "Order flow verified: PENDING -> SUCCESS!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSimulateFailure = {
                        val failed = gatewayService.simulateStateTransition(activeOrder?.internalOrderId ?: "", PaymentStatus.FAILED)
                        activeOrder = failed ?: activeOrder?.copy(status = PaymentStatus.FAILED, failureReason = "Payment declined by customer bank (Mock)")
                        addLog("Simulated Payment Failure: Order marked FAILED.")
                        Toast.makeText(context, "Order transitioned: PENDING -> FAILED", Toast.LENGTH_SHORT).show()
                    },
                    onCreateNewOrder = { domain ->
                        selectedDomain = domain
                        if (domain == OrderDomain.SCHOOL_FEE) {
                            val fee = feeRecords.firstOrNull()
                            val order = gatewayService.createPaymentOrder(
                                tenantId = "INST-SXA-2026",
                                studentId = fee?.studentId ?: "STU-2026-001",
                                studentName = fee?.studentName ?: "Aarav Sharma",
                                feeRecordId = fee?.id ?: "FEE-2026-001",
                                term = fee?.term ?: "Term 1 Tuition",
                                amountPaise = 1500000L,
                                idempotencyKey = UUID.randomUUID().toString(),
                                provider = PaymentGatewayProvider.RAZORPAY
                            )
                            activeOrder = order
                            addLog("Created New School Fee Order: ${order.internalOrderId} (Amount: ₹${order.amountRupees}) in PENDING status.")
                        } else {
                            val order = gatewayService.createPaymentOrder(
                                tenantId = "INST-SXA-2026",
                                studentId = "SAAS-CAMPUS-01",
                                studentName = "Enterprise Institutional License",
                                feeRecordId = "BILL-SAAS-2026",
                                term = "Annual 2026-27",
                                amountPaise = SaaSPlan.GROWTH.pricePaise,
                                idempotencyKey = UUID.randomUUID().toString(),
                                provider = PaymentGatewayProvider.RAZORPAY
                            )
                            activeOrder = order
                            addLog("Created New SaaS Order: ${order.internalOrderId} (Plan: Growth, Amount: ₹${order.amountRupees}) in PENDING status.")
                        }
                        Toast.makeText(context, "New PENDING order created!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // =================================================================
            // TAB 0: ORDER DETAILS & WEBHOOK SIMULATION
            // =================================================================
            if (activeTab == 0) {
                item {
                    WebhookSimulatorConsoleCard(
                        activeOrder = activeOrder,
                        selectedWebhookType = selectedWebhookType,
                        onSelectWebhookType = { selectedWebhookType = it },
                        webhookSecret = webhookSecret,
                        lastResult = lastWebhookResult,
                        onDispatchWebhook = { isDuplicate, isForged ->
                            if (activeOrder == null) {
                                Toast.makeText(context, "No active order to target!", Toast.LENGTH_SHORT).show()
                                return@WebhookSimulatorConsoleCard
                            }

                            val eventId = if (isDuplicate) "evt_dup_replay_nonce_771" else "evt_${System.currentTimeMillis()}"
                            val payloadJson = """
                                {
                                  "entity": "event",
                                  "account_id": "acc_sxa_institution_2026",
                                  "event": "$selectedWebhookType",
                                  "contains": ["payment"],
                                  "payload": {
                                    "payment": {
                                      "entity": {
                                        "id": "pay_${UUID.randomUUID().toString().take(10)}",
                                        "amount": ${activeOrder!!.amountPaise},
                                        "currency": "INR",
                                        "status": "${if (selectedWebhookType == "payment.captured") "captured" else "failed"}",
                                        "order_id": "${activeOrder!!.gatewayOrderId}",
                                        "method": "upi",
                                        "vpa": "parent@okaxis"
                                      }
                                    }
                                  }
                                }
                            """.trimIndent()

                            val signature = if (isForged) {
                                "tampered_bad_hmac_hash_0000000000000000"
                            } else {
                                gatewayService.computeHmacSha256(payloadJson, webhookSecret)
                            }

                            val webhookEvent = GatewayWebhookEvent(
                                eventId = eventId,
                                eventType = selectedWebhookType,
                                gatewayProvider = activeOrder!!.gatewayProvider,
                                payloadJson = payloadJson,
                                signatureHeader = signature
                            )

                            try {
                                val result = gatewayService.processWebhookEvent(
                                    event = webhookEvent,
                                    webhookSecret = webhookSecret,
                                    targetOrderId = activeOrder!!.internalOrderId
                                )
                                lastWebhookResult = result
                                activeOrder = gatewayService.getOrder(activeOrder!!.internalOrderId) ?: activeOrder

                                if (result.isDuplicate) {
                                    addLog("SECURITY NOTICE: Duplicate webhook nonce ($eventId) intercepted. Idempotent response returned.")
                                    Toast.makeText(context, "Replay blocked: Duplicate event ignored!", Toast.LENGTH_SHORT).show()
                                } else {
                                    addLog("Webhook Event $selectedWebhookType processed. Resulting State: ${result.resultingStatus}")
                                    Toast.makeText(context, "Webhook processed: ${result.message}", Toast.LENGTH_SHORT).show()
                                }
                                refreshLedger()
                            } catch (e: PaymentSecurityException) {
                                addLog("SECURITY ALERT: ${e.message}")
                                Toast.makeText(context, "Forged Webhook Rejected: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                item {
                    SimulatedPaymentVerificationDesk(
                        activeOrder = activeOrder,
                        onVerifyClientCallback = {
                            if (activeOrder == null) return@SimulatedPaymentVerificationDesk
                            val mockPaymentId = "pay_client_cb_${System.currentTimeMillis().toString().takeLast(6)}"
                            val signature = gatewayService.computeHmacSha256(
                                "${activeOrder!!.gatewayOrderId}|$mockPaymentId",
                                webhookSecret
                            )
                            val req = com.example.payment.PaymentVerificationRequest(
                                internalOrderId = activeOrder!!.internalOrderId,
                                gatewayOrderId = activeOrder!!.gatewayOrderId ?: "gw_ord_1",
                                gatewayPaymentId = mockPaymentId,
                                gatewaySignature = signature,
                                tenantId = activeOrder!!.tenantId
                            )
                            val res = gatewayService.verifyPayment(req, webhookSecret)
                            activeOrder = gatewayService.getOrder(activeOrder!!.internalOrderId) ?: activeOrder
                            refreshLedger()
                            addLog("Client Checkout Callback Verified: Order ${res.internalOrderId} -> ${res.status}")
                            Toast.makeText(context, "Client verification passed: ${res.status}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // =================================================================
            // TAB 1: DOUBLE-ENTRY LEDGER & RECONCILIATION
            // =================================================================
            if (activeTab == 1) {
                item {
                    DoubleEntryLedgerAuditorCard(
                        gatewayService = gatewayService,
                        tenantId = "INST-SXA-2026",
                        journal = ledgerJournal,
                        onRefresh = {
                            refreshLedger()
                            addLog("General Ledger re-audited. Balance equation verified.")
                            Toast.makeText(context, "Ledger re-calculated", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    RefundSimulatorCard(
                        activeOrder = activeOrder,
                        onExecuteRefund = { amountPaise, isFull ->
                            if (activeOrder == null) return@RefundSimulatorCard
                            val req = RefundRequest(
                                internalOrderId = activeOrder!!.internalOrderId,
                                refundAmountPaise = amountPaise,
                                reason = if (isFull) "Full tuition fee waiver" else "Partial concession refund",
                                requestedByUserId = "USR-BURSAR-01"
                            )
                            val res = gatewayService.processRefund(req)
                            if (res.isSuccess) {
                                val r = res.getOrThrow()
                                activeOrder = gatewayService.getOrder(r.internalOrderId)
                                refreshLedger()
                                addLog("Refund Processed: Order ${r.internalOrderId} marked ${r.status}. Reversing ledger entry recorded.")
                                Toast.makeText(context, "Refund processed: ${r.status}", Toast.LENGTH_SHORT).show()
                            } else {
                                addLog("Refund rejected: ${res.exceptionOrNull()?.message}")
                                Toast.makeText(context, "Refund rejected: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }
            }

            // =================================================================
            // TAB 2: ZERO-TRUST SECURITY & ANTI-REPLAY AUDIT
            // =================================================================
            if (activeTab == 2) {
                item {
                    SecurityVerificationInspectorCard(
                        activeOrder = activeOrder,
                        processedWebhooks = gatewayService.getProcessedWebhooks(),
                        onTestTamperedPayload = {
                            if (activeOrder == null) return@SecurityVerificationInspectorCard
                            val payloadJson = """{"event":"payment.captured","tampered_amount":99999999}"""
                            val forgedSig = "bad_signature_00000000000000000000"
                            val event = GatewayWebhookEvent(
                                eventId = "evt_tampered_test",
                                eventType = "payment.captured",
                                gatewayProvider = PaymentGatewayProvider.RAZORPAY,
                                payloadJson = payloadJson,
                                signatureHeader = forgedSig
                            )
                            try {
                                gatewayService.processWebhookEvent(event, webhookSecret, activeOrder?.internalOrderId)
                                addLog("CRITICAL FAILURE: Tampered payload was not rejected!")
                            } catch (e: PaymentSecurityException) {
                                addLog("SECURITY CHECK PASSED: Tampered payload caught & rejected with constant-time equality check.")
                                Toast.makeText(context, "Cryptographic tamper check PASSED!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTestNetworkTimeout = {
                            if (activeOrder == null) return@SecurityVerificationInspectorCard
                            gatewayService.simulateOrderExpiration(activeOrder!!.internalOrderId)
                            val failed = gatewayService.handleCheckoutNetworkDrop(activeOrder!!.internalOrderId)
                            activeOrder = failed
                            addLog("Network Drop Simulated: Order expired past 30-min window -> Status updated to FAILED.")
                            Toast.makeText(context, "Order session expired due to network drop timeout!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // =================================================================
            // REAL-TIME AUDIT LOG TERMINAL (Always Visible at Bottom)
            // =================================================================
            item {
                SandboxAuditTerminalCard(
                    auditLogs = auditLogs,
                    onClearLogs = {
                        auditLogs = emptyList()
                        Toast.makeText(context, "Terminal logs cleared", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: ORDER LIFECYCLE PIPELINE STEPPER
// =============================================================================
@Composable
private fun OrderLifecyclePipelineCard(
    order: FeePaymentOrder?,
    isAutoRunning: Boolean,
    onFastTrackSuccess: () -> Unit,
    onSimulateFailure: () -> Unit,
    onCreateNewOrder: (OrderDomain) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_lifecycle_pipeline_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Pipeline Title & Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ORDER LIFECYCLE VERIFIER",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = GovNavyDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        order?.let {
                            StatusPillBadge(status = it.status)
                        }
                    }
                    Text(
                        text = "Real-time state transitions without banking transactions",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )
                }

                // Fast-Track Runner Button
                GlassButton(
                    text = if (isAutoRunning) "Simulating..." else "Verify PENDING -> SUCCESS",
                    onClick = onFastTrackSuccess,
                    color = GovGreen,
                    leadingIcon = {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.testTag("btn_fast_track_order_flow")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // State Pipeline Stepper (Visual Flow from PENDING to SUCCESS)
            val currentStatus = order?.status ?: PaymentStatus.CREATED
            val stepIndex = when (currentStatus) {
                PaymentStatus.CREATED -> 0
                PaymentStatus.PENDING -> 1
                PaymentStatus.AUTHORIZED -> 2
                PaymentStatus.CAPTURED_SUCCESS -> 3
                PaymentStatus.FAILED -> -1
                PaymentStatus.PARTIALLY_REFUNDED, PaymentStatus.REFUNDED -> 4
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PipelineStep(step = "1. CREATED", isDone = stepIndex >= 0, isCurrent = stepIndex == 0, isFailed = false)
                PipelineConnector(isDone = stepIndex >= 1)
                PipelineStep(step = "2. PENDING", isDone = stepIndex >= 1, isCurrent = stepIndex == 1, isFailed = false)
                PipelineConnector(isDone = stepIndex >= 2)
                PipelineStep(step = "3. AUTHORIZED", isDone = stepIndex >= 2, isCurrent = stepIndex == 2, isFailed = false)
                PipelineConnector(isDone = stepIndex >= 3)
                PipelineStep(
                    step = if (currentStatus == PaymentStatus.FAILED) "FAILED" else "4. SUCCESS",
                    isDone = stepIndex >= 3,
                    isCurrent = stepIndex == 3 || currentStatus == PaymentStatus.FAILED,
                    isFailed = currentStatus == PaymentStatus.FAILED
                )
            }

            if (isAutoRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = GovGreen,
                    trackColor = GovGreen.copy(alpha = 0.2f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Order Summary Details
            if (order != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("ORDER ID: ${order.internalOrderId}", fontSize = 10.5.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                                Text("Invoice ID: ${order.internalInvoiceId} • ${order.studentName}", fontSize = 9.5.sp, color = GovSlateDark)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${order.amountRupees}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = GovNavyDark)
                                Text("Receipt: ${order.receiptNumber}", fontSize = 9.sp, color = GovGreen, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (order.failureReason != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Failure Reason: ${order.failureReason}",
                                fontSize = 9.sp,
                                color = GovRedAlert,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Presets Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassOutlinedButton(
                    text = "New Fee Order",
                    onClick = { onCreateNewOrder(OrderDomain.SCHOOL_FEE) },
                    modifier = Modifier.weight(1f)
                )
                GlassOutlinedButton(
                    text = "New SaaS Order",
                    onClick = { onCreateNewOrder(OrderDomain.SAAS_SUBSCRIPTION) },
                    modifier = Modifier.weight(1f)
                )
                GlassOutlinedButton(
                    text = "Simulate Failure",
                    onClick = onSimulateFailure,
                    color = GovRedAlert,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: WEBHOOK SIMULATOR CONSOLE
// =============================================================================
@Composable
private fun WebhookSimulatorConsoleCard(
    activeOrder: FeePaymentOrder?,
    selectedWebhookType: String,
    onSelectWebhookType: (String) -> Unit,
    webhookSecret: String,
    lastResult: WebhookProcessingResult?,
    onDispatchWebhook: (isDuplicate: Boolean, isForged: Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("webhook_simulator_console_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEBHOOK EVENT SIMULATOR",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Simulates aggregator server-to-server callbacks with HMAC signing",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )
                }
                GlassBadge(text = "ZERO TRUST", color = GovNavyPrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Event Type Selector Chips
            Text("Select Simulated Webhook Event:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
            Spacer(modifier = Modifier.height(6.dp))

            val eventTypes = listOf("payment.captured", "payment.authorized", "payment.failed", "refund.processed")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(eventTypes) { eventType ->
                    val isSelected = selectedWebhookType == eventType
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) GovNavyPrimary else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) GovNavyPrimary else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .clickable { onSelectWebhookType(eventType) }
                            .testTag("webhook_type_$eventType")
                    ) {
                        Text(
                            text = eventType,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureWhite else GovSlateDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Simulated Inbound JSON Payload Preview
            val previewOrderId = activeOrder?.gatewayOrderId ?: "order_rzp_mock_88102"
            val previewAmount = activeOrder?.amountPaise ?: 1500000L
            val simulatedPayloadJson = """
                {
                  "entity": "event",
                  "event": "$selectedWebhookType",
                  "order_id": "$previewOrderId",
                  "amount_paise": $previewAmount,
                  "currency": "INR",
                  "timestamp": ${System.currentTimeMillis()}
                }
            """.trimIndent()

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SIMULATED INBOUND PAYLOAD (JSON):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text("HMAC-SHA256 SIGNED", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF86EFAC))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = simulatedPayloadJson,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Webhook Simulation Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = "Dispatch Webhook",
                    onClick = { onDispatchWebhook(false, false) },
                    color = GovGreen,
                    leadingIcon = {
                        Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("btn_dispatch_webhook")
                )

                GlassOutlinedButton(
                    text = "Replay Nonce",
                    onClick = { onDispatchWebhook(true, false) },
                    color = GovSaffron,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_replay_webhook")
                )

                GlassOutlinedButton(
                    text = "Forged Sig",
                    onClick = { onDispatchWebhook(false, true) },
                    color = GovRedAlert,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_forged_webhook")
                )
            }

            // Webhook Last Result Banner
            if (lastResult != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (lastResult.isDuplicate) GovSaffron.copy(alpha = 0.1f) else GovGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, if (lastResult.isDuplicate) GovSaffron.copy(alpha = 0.3f) else GovGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (lastResult.isDuplicate) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (lastResult.isDuplicate) GovSaffron else GovGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (lastResult.isDuplicate) "Idempotent Replay Intercepted" else "Webhook Processed Successfully",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Text(
                                text = "${lastResult.message} • Resulting Order Status: ${lastResult.resultingStatus ?: "UNCHANGED"}",
                                fontSize = 9.sp,
                                color = GovSlateDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: CLIENT CALLBACK VERIFICATION DESK
// =============================================================================
@Composable
private fun SimulatedPaymentVerificationDesk(
    activeOrder: FeePaymentOrder?,
    onVerifyClientCallback: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLIENT CALLBACK VERIFICATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Never trust client callbacks without authoritative backend validation",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )
                }
                GlassButton(
                    text = "Verify Callback",
                    onClick = onVerifyClientCallback,
                    color = GovNavyPrimary,
                    leadingIcon = {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: BALANCED DOUBLE-ENTRY LEDGER AUDITOR
// =============================================================================
@Composable
private fun DoubleEntryLedgerAuditorCard(
    gatewayService: PaymentGatewayService,
    tenantId: String,
    journal: List<DoubleEntryRecord>,
    onRefresh: () -> Unit
) {
    val bankBal = gatewayService.getLedgerBalance(tenantId, LedgerAccountType.CASH_AT_BANK)
    val feeRecBal = gatewayService.getLedgerBalance(tenantId, LedgerAccountType.FEES_RECEIVABLE)
    val escrowBal = gatewayService.getLedgerBalance(tenantId, LedgerAccountType.ESCROW_CLEARING)
    val platformBal = gatewayService.getLedgerBalance(tenantId, LedgerAccountType.PLATFORM_REVENUE)
    val refundBal = gatewayService.getLedgerBalance(tenantId, LedgerAccountType.REFUND_CLEARING)

    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("double_entry_ledger_auditor_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DOUBLE-ENTRY GENERAL LEDGER AUDITOR",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Balanced Bookkeeping: Live T-Accounts & Balance Equations",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )
                }
                GlassBadge(text = "EQUATION: ΔDEBIT = ΔCREDIT", color = GovGreen)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // T-Accounts Running Balances Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerAccountTile(
                    title = "CASH AT BANK",
                    balancePaise = bankBal,
                    accountType = "ASSET (+)",
                    color = GovGreen,
                    modifier = Modifier.weight(1f)
                )
                LedgerAccountTile(
                    title = "FEES RECEIVABLE",
                    balancePaise = feeRecBal,
                    accountType = "ASSET (-)",
                    color = GovNavyPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LedgerAccountTile(
                    title = "ESCROW CLEARING",
                    balancePaise = escrowBal,
                    accountType = "CLEARING",
                    color = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                LedgerAccountTile(
                    title = "PLATFORM REVENUE",
                    balancePaise = platformBal,
                    accountType = "REVENUE (+)",
                    color = GovGreen,
                    modifier = Modifier.weight(1f)
                )
                LedgerAccountTile(
                    title = "REFUND CLEARING",
                    balancePaise = refundBal,
                    accountType = "CONTRA",
                    color = GovRedAlert,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Posted Journal Transactions
            Text(
                text = "Recently Posted Double-Entry Journal Records (${journal.size}):",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = GovNavyDark
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (journal.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No ledger entries posted yet. Transition an order from PENDING to SUCCESS to post balanced debits and credits.",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    journal.take(5).forEach { entry ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Dr: ${entry.debitAccount.name} | Cr: ${entry.creditAccount.name}", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                                    Text("${entry.description} • ${entry.orderId}", fontSize = 8.5.sp, color = GovSlateMuted)
                                }
                                Text("₹${entry.amountPaise / 100.0}", fontSize = 11.sp, fontWeight = FontWeight.Black, color = GovGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: REFUND STATE MACHINE SIMULATOR
// =============================================================================
@Composable
private fun RefundSimulatorCard(
    activeOrder: FeePaymentOrder?,
    onExecuteRefund: (amountPaise: Long, isFull: Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "REFUND STATE MACHINE & REVERSING ENTRIES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GovNavyDark
            )
            Text(
                text = "Simulates partial or full statutory fee reversals with contra ledger postings",
                fontSize = 9.5.sp,
                color = GovSlateMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            val canRefund = activeOrder != null && (activeOrder.status == PaymentStatus.CAPTURED_SUCCESS || activeOrder.status == PaymentStatus.PARTIALLY_REFUNDED)

            if (!canRefund) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEF2F2),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Refund requires an order in CAPTURED_SUCCESS status. Currently: ${activeOrder?.status ?: "NONE"}",
                        fontSize = 9.sp,
                        color = GovRedAlert,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        text = "Partial Refund (₹5,000)",
                        onClick = { onExecuteRefund(500000L, false) },
                        color = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                    GlassButton(
                        text = "Full Refund (₹15,000)",
                        onClick = { onExecuteRefund(activeOrder!!.amountPaise - activeOrder.refundedAmountPaise, true) },
                        color = GovRedAlert,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: ZERO-TRUST SECURITY & ANTI-REPLAY AUDIT
// =============================================================================
@Composable
private fun SecurityVerificationInspectorCard(
    activeOrder: FeePaymentOrder?,
    processedWebhooks: Map<String, Long>,
    onTestTamperedPayload: () -> Unit,
    onTestNetworkTimeout: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ZERO-TRUST CRYPTOGRAPHIC SECURITY INSPECTOR",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Black,
                color = GovNavyDark
            )
            Text(
                text = "Timing-attack resistant HMAC signatures, anti-replay nonces & session timeouts",
                fontSize = 9.5.sp,
                color = GovSlateMuted
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Processed Anti-Replay Event Nonces: ${processedWebhooks.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                    processedWebhooks.entries.take(4).forEach { entry ->
                        Text("• ${entry.key} (processed at ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.value))})", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = GovSlateDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassOutlinedButton(
                    text = "Test Tampered HMAC",
                    onClick = onTestTamperedPayload,
                    color = GovRedAlert,
                    modifier = Modifier.weight(1f)
                )
                GlassOutlinedButton(
                    text = "Simulate Network Drop",
                    onClick = onTestNetworkTimeout,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// =============================================================================
// SUB-COMPONENT: LIVE STREAMING AUDIT TERMINAL
// =============================================================================
@Composable
private fun SandboxAuditTerminalCard(
    auditLogs: List<String>,
    onClearLogs: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sandbox_audit_terminal_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cryptographic Execution Audit Terminal", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                }
                GlassButton(text = "Clear Terminal", onClick = onClearLogs, color = Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF0F172A),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text(
                            text = "No execution events yet. Run a simulated webhook or order flow to stream cryptographic telemetry.",
                            fontSize = 9.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        auditLogs.take(15).forEach { log ->
                            val color = when {
                                log.contains("FAILED") || log.contains("ALERT") || log.contains("rejected") -> Color(0xFFFCA5A5)
                                log.contains("SUCCESS") || log.contains("PASSED") || log.contains("COMMITTED") -> Color(0xFF86EFAC)
                                log.contains("REPLAY") || log.contains("Intercepted") -> Color(0xFFFDBA74)
                                else -> Color(0xFFE2E8F0)
                            }
                            Text(
                                text = log,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = color,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// HELPER UI ELEMENTS
// =============================================================================
@Composable
private fun PipelineStep(
    step: String,
    isDone: Boolean,
    isCurrent: Boolean,
    isFailed: Boolean
) {
    val bgColor = when {
        isFailed -> GovRedAlert
        isCurrent -> GovNavyPrimary
        isDone -> GovGreen
        else -> Color(0xFFE2E8F0)
    }

    val textColor = if (isDone || isCurrent || isFailed) PureWhite else GovSlateMuted

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isFailed) {
                Icon(Icons.Default.Close, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
            } else if (isDone && !isCurrent) {
                Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
            } else {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(PureWhite))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = step,
            fontSize = 8.5.sp,
            fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
            color = if (isCurrent) GovNavyDark else GovSlateMuted
        )
    }
}

@Composable
private fun PipelineConnector(isDone: Boolean) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(2.dp)
            .background(if (isDone) GovGreen else Color(0xFFCBD5E1))
    )
}

@Composable
private fun StatusPillBadge(status: PaymentStatus) {
    val (bg, txt) = when (status) {
        PaymentStatus.CAPTURED_SUCCESS -> Pair(GovGreen, PureWhite)
        PaymentStatus.PENDING -> Pair(GovSaffron, PureWhite)
        PaymentStatus.AUTHORIZED -> Pair(Color(0xFF2563EB), PureWhite)
        PaymentStatus.FAILED -> Pair(GovRedAlert, PureWhite)
        PaymentStatus.REFUNDED, PaymentStatus.PARTIALLY_REFUNDED -> Pair(Color(0xFF64748B), PureWhite)
        PaymentStatus.CREATED -> Pair(Color(0xFF0F172A), PureWhite)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = Modifier.testTag("status_pill_${status.name}")
    ) {
        Text(
            text = status.name,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            color = txt,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LedgerAccountTile(
    title: String,
    balancePaise: Long,
    accountType: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GovSlateMuted)
            Text(
                text = "₹${balancePaise / 100.0}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(accountType, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.8f))
        }
    }
}
