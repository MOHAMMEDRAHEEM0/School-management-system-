package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.repository.FeeBalanceSummary
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.OfficialSealStamp
import com.example.ui.components.OfficialWhatsAppButton
import com.example.ui.theme.GlassBorderLight
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

@Composable
fun AccountantDashboard(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val feeWorkflowState by viewModel.feeWorkflowState.collectAsState()
    val bills by viewModel.institutionalBills.collectAsState()

    // Dialog for Manual Fee Collection Entry
    var showFeeEntryDialog by remember { mutableStateOf(false) }
    var selectedSummary by remember { mutableStateOf<FeeBalanceSummary?>(null) }
    var enteredAmount by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("CASH") }
    var paymentReference by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("accountant_dashboard")
    ) {
        // Bursar & Finance Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Finance & Bursar Desk",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(text = "ACCOUNTS HEAD", color = Color(0xFFD97706))
                }
                Text(
                    text = "${feeWorkflowState.actorDisplayName.ifBlank { "Accounts Officer" }} | Institutional Bursar Directorate",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OfficialSealStamp(
                title = "BURSAR",
                code = "FINANCE",
                modifier = Modifier.size(50.dp),
                color = Color(0xFFD97706)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Financial KPIs derived strictly from persisted Room transactions
        val collectedRupees = feeWorkflowState.totalCollectedPaise / 100L
        val outstandingRupees = feeWorkflowState.totalOutstandingPaise / 100L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovGreen,
                headerTitle = "COLLECTED"
            ) {
                Column {
                    Text(
                        text = "₹$collectedRupees",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(
                        text = "${feeWorkflowState.settledAccountCount} Settled • ${feeWorkflowState.pendingSyncTransactionCount} Pending Sync",
                        fontSize = 8.5.sp,
                        color = GovSlateMuted
                    )
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = if (outstandingRupees > 0L) GovRedAlert else GovGreen,
                headerTitle = "OUTSTANDING"
            ) {
                Column {
                    Text(
                        text = "₹$outstandingRupees",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (outstandingRupees > 0L) GovRedAlert else GovGreen
                    )
                    Text(
                        text = "${feeWorkflowState.dueAccountCount} Pending Account(s)",
                        fontSize = 8.5.sp,
                        color = GovSlateMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Tabs to prevent narrow-screen text wrapping
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureWhite,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GovNavyPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Fee Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Defaulters & Notices", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Institution Bills", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Gateway Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) },
                    modifier = Modifier.testTag("tab_accountant_payment_sandbox")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: STUDENT FEE LEDGER
        // =====================================================================
        if (selectedTab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (feeWorkflowState.feeSummaries.isEmpty()) {
                    Text(
                        text = "No persisted fee records found for this institution yet. Open Treasury to issue a new fee assessment.",
                        fontSize = 12.sp,
                        color = GovSlateMuted
                    )
                }
                feeWorkflowState.feeSummaries.forEach { summary ->
                    val fee = summary.feeRecord
                    val displayTotal = summary.totalAmountPaise / 100L
                    val displayPaid = summary.confirmedPaidPaise / 100L
                    val displayDue = summary.outstandingBalancePaise / 100L

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (fee.status == "PAID") GovGreen else GovSaffron,
                        headerTitle = "${fee.studentName.uppercase()} (${fee.gradeSection})",
                        headerBadge = fee.status
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        text = fee.feeType,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Adm No: ${fee.admissionNumber} | Latest Receipt: ${fee.receiptNumber ?: "NONE"}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                    Text(
                                        text = "Paid: ₹$displayPaid / ₹$displayTotal",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovNavyPrimary
                                    )
                                }

                                if (displayDue > 0) {
                                    GlassButton(
                                        text = "Collect ₹$displayDue",
                                        onClick = {
                                            selectedSummary = summary
                                            enteredAmount = displayDue.toString()
                                            paymentMode = "CASH"
                                            paymentReference = ""
                                            showFeeEntryDialog = true
                                        },
                                        color = GovGreen
                                    )
                                } else {
                                    OfficialSealStamp(title = "PAID", code = "CLEARED", modifier = Modifier.size(50.dp), color = GovGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 1: DEFAULTERS & WHATSAPP REMINDER DISPATCH
        // =====================================================================
        if (selectedTab == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pending Fee Defaulters (1-Tap Official Notice Dispatch):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                val defaulters = feeWorkflowState.feeSummaries.filter { it.outstandingBalancePaise > 0L }
                if (defaulters.isEmpty()) {
                    Text("No outstanding defaulters in active roster.", fontSize = 11.sp, color = GovGreen)
                } else {
                    defaulters.forEach { summary ->
                        val def = summary.feeRecord
                        val displayDue = summary.outstandingBalancePaise / 100L
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            headerRibbonColor = GovRedAlert,
                            headerTitle = "DEFAULTER: ${def.studentName.uppercase()}",
                            headerBadge = "DUE: ₹$displayDue"
                        ) {
                            Column {
                                Text(
                                    text = "Student: ${def.studentName} | Section: ${def.gradeSection}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Outstanding fee balance of ₹$displayDue for ${def.term}.",
                                    fontSize = 10.5.sp,
                                    color = GovRedAlert
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OfficialWhatsAppButton(
                                    phone = "9123456783",
                                    message = "Official Notice from ${feeWorkflowState.activeTenantId} Finance Directorate: Outstanding fees of ₹$displayDue for ${def.studentName} (${def.term}) remain due.",
                                    label = "Dispatch Official WhatsApp Fee Notice",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: INSTITUTION UTILITIES & OPERATIONAL EXPENSES
        // =====================================================================
        if (selectedTab == 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Academy Operational Expense Disbursements:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                bills.forEach { bill ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (bill.paymentStatus == "PAID") GovGreen else GovSaffron,
                        headerTitle = "${bill.billType} INVOICE",
                        headerBadge = bill.paymentStatus
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = bill.vendorName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Invoice: ${bill.billNumber} | Due: ${bill.dueDate}",
                                    fontSize = 10.sp,
                                    color = GovSlateMuted
                                )
                            }
                            Text(
                                text = "₹${bill.amountRupees}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = GovNavyDark
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 3: GATEWAY STATUS (NO LIVE PROVIDER CONFIGURED)
        // =====================================================================
        if (selectedTab == 3) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerRibbonColor = GovNavyPrimary,
                headerTitle = "PAYMENT GATEWAY STATUS",
                headerBadge = "NOT CONFIGURED"
            ) {
                Text(
                    text = "No live online payment gateway provider or webhook merchant key is configured on this build. Use the Fee Ledger or Treasury tab to record manual counter payments (MANUAL_RECORDED) with unique receipts.",
                    fontSize = 11.5.sp,
                    color = GovSlateDark
                )
            }
        }
    }

    // Manual Counter Fee Collection Entry Dialog
    if (showFeeEntryDialog && selectedSummary != null) {
        val summary = selectedSummary!!
        val fee = summary.feeRecord
        val maxRupees = summary.outstandingBalancePaise / 100L
        val dialogIdempotencyKey = remember(fee.id, showFeeEntryDialog) { java.util.UUID.randomUUID().toString() }
        AlertDialog(
            onDismissRequest = { showFeeEntryDialog = false },
            title = {
                Text("Record Manual Counter Payment", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Scholar: ${fee.studentName} (${fee.gradeSection}) • Max Due: ₹$maxRupees",
                        fontSize = 11.sp,
                        color = GovSlateMuted
                    )
                    GlassTextField(
                        value = enteredAmount,
                        onValueChange = { enteredAmount = it.filter { ch -> ch.isDigit() } },
                        placeholder = "Amount in Rupees",
                        label = "Amount Collected (₹)"
                    )
                    GlassTextField(
                        value = paymentMode,
                        onValueChange = { paymentMode = it },
                        placeholder = "CASH, CHEQUE, BANK_TRANSFER, UPI_MANUAL, DD",
                        label = "Manual Payment Mode"
                    )
                    GlassTextField(
                        value = paymentReference,
                        onValueChange = { paymentReference = it },
                        placeholder = "Counter slip / Cheque / UTR ref",
                        label = "Reference Number"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Record & Issue Receipt",
                    onClick = {
                        val amount = enteredAmount.toLongOrNull() ?: 0L
                        viewModel.recordManualFeePayment(
                            studentId = fee.studentId,
                            feeRecordId = fee.id,
                            amountRupees = amount,
                            paymentMode = paymentMode,
                            transactionReference = paymentReference,
                            idempotencyKey = dialogIdempotencyKey
                        ) { ok, msg, _ ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (ok) showFeeEntryDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showFeeEntryDialog = false })
            }
        )
    }
}
