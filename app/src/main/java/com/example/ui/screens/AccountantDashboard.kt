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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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

    val feeRecords by viewModel.feeRecords.collectAsState()
    val bills by viewModel.institutionalBills.collectAsState()

    // Dialog for Fee Collection Entry
    var showFeeEntryDialog by remember { mutableStateOf(false) }
    var selectedFee by remember { mutableStateOf<FeeRecordEntity?>(null) }
    var enteredAmount by remember { mutableStateOf("15000") }
    var paymentMode by remember { mutableStateOf("UPI_DIGITAL") }

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
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Finance & Bursar Desk",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(text = "ACCOUNTS HEAD", color = Color(0xFFD97706))
                }
                Text(
                    text = "Suresh Mehta | Head of Institutional Accounts & Bursar Directorate",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted
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

        // Financial KPIs
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
                        text = " 1.80 L",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(text = "Term 1 Tuition", fontSize = 8.5.sp, color = GovSlateMuted)
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovRedAlert,
                headerTitle = "OUTSTANDING"
            ) {
                Column {
                    Text(
                        text = " 15,000",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GovRedAlert
                    )
                    Text(text = "1 Pending Scholar", fontSize = 8.5.sp, color = GovSlateMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GovNavyPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Fee Ledger", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Defaulters & WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Institution Bills", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Payment Sandbox", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
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
                feeRecords.forEach { fee ->
                    val displayTotal = if (fee.totalAmount >= 100000L) fee.totalAmount / 100L else fee.totalAmount
                    val displayPaid = if (fee.paidAmount >= 100000L) fee.paidAmount / 100L else fee.paidAmount
                    val displayDue = if (fee.dueAmount >= 100000L) fee.dueAmount / 100L else fee.dueAmount

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
                                Column {
                                    Text(
                                        text = fee.feeType,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Adm No: ${fee.admissionNumber} | Receipt: ${fee.receiptNumber ?: "PENDING"}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                    Text(
                                        text = "Paid:  $displayPaid /  $displayTotal",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovNavyPrimary
                                    )
                                }

                                if (displayDue > 0) {
                                    GlassButton(
                                        text = "Collect  $displayDue",
                                        onClick = {
                                            selectedFee = fee
                                            enteredAmount = displayDue.toString()
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

                val defaulters = feeRecords.filter { it.status != "PAID" }
                if (defaulters.isEmpty()) {
                    Text("No outstanding defaulters in active roster.", fontSize = 11.sp, color = GovGreen)
                } else {
                    defaulters.forEach { def ->
                        val displayDue = if (def.dueAmount >= 100000L) def.dueAmount / 100L else def.dueAmount
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            headerRibbonColor = GovRedAlert,
                            headerTitle = "DEFAULTER: ${def.studentName.uppercase()}",
                            headerBadge = "DUE:  $displayDue"
                        ) {
                            Column {
                                Text(
                                    text = "Student: ${def.studentName} | Section: ${def.gradeSection}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Outstanding composite fee balance of  $displayDue for ${def.term}.",
                                    fontSize = 10.5.sp,
                                    color = GovRedAlert
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OfficialWhatsAppButton(
                                    phone = "9123456783",
                                    message = "Official Notice from St. Xavier's International Academy Finance Directorate: This is a polite reminder that Term 1 fees of  $displayDue for ${def.studentName} are due. Kindly clear the dues through the school portal.",
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
                                text = " ${bill.amountRupees}",
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
        // TAB 3: PAYMENT SANDBOX DASHBOARD (ADMIN WEBHOOK & LEDGER VERIFIER)
        // =====================================================================
        if (selectedTab == 3) {
            com.example.ui.components.PaymentSandboxDashboard(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Fee Collection Entry Dialog
    if (showFeeEntryDialog && selectedFee != null) {
        val fee = selectedFee!!
        AlertDialog(
            onDismissRequest = { showFeeEntryDialog = false },
            title = {
                Text("Process Fee Collection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Scholar: ${fee.studentName} (${fee.gradeSection})", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(
                        value = enteredAmount,
                        onValueChange = { enteredAmount = it },
                        placeholder = "Amount in Rupees",
                        label = "Amount Collected ()"
                    )
                    GlassTextField(
                        value = paymentMode,
                        onValueChange = { paymentMode = it },
                        placeholder = "CASH, CHEQUE, UPI, or CARD",
                        label = "Payment Mode"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Generate Official Receipt",
                    onClick = {
                        val amount = enteredAmount.toLongOrNull() ?: 15000L
                        val receiptNo = "SXA-REC-2026-${(8000..9999).random()}"
                        viewModel.inchargeRecordFeePayment(
                            feeId = fee.id,
                            paidAmount = amount,
                            dueAmount = 0L,
                            receiptNo = receiptNo
                        )
                        Toast.makeText(context, "Payment committed! Receipt No: $receiptNo", Toast.LENGTH_LONG).show()
                        showFeeEntryDialog = false
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showFeeEntryDialog = false })
            }
        )
    }
}
