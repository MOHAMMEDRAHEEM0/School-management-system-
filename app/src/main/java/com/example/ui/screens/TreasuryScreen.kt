package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.FeatureAvailability
import com.example.data.local.entities.FeeRecordEntity
import com.example.ui.components.ModernBlueCard
import com.example.ui.components.PaymentSandboxDashboard
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.ModernBlue100
import com.example.ui.theme.ModernBlue200
import com.example.ui.theme.ModernBlue50
import com.example.ui.theme.ModernBlue600
import com.example.ui.theme.ModernBlue700
import com.example.ui.theme.ModernBlue800
import com.example.ui.theme.ModernCardBackground
import com.example.ui.theme.ModernCardBorder
import com.example.ui.theme.ModernSurfaceSubtle
import com.example.ui.theme.ModernTextMuted
import com.example.ui.theme.ModernTextPrimary
import com.example.ui.theme.ModernTextSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel

/**
 * Modern Institutional Treasury & Fee Management Screen.
 * Provides unified access to Student Fee Ledgers, Instant Receipt Printing,
 * and the Production Payment Gateway Sandbox.
 */
@Composable
fun TreasuryScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeReceiptPreview by remember { mutableStateOf<FeeRecordEntity?>(null) }
    val feeRecords by viewModel.feeRecords.collectAsState()
    val totalCollected = remember(feeRecords) { feeRecords.sumOf { it.paidRupees } }
    val totalDue = remember(feeRecords) { feeRecords.sumOf { it.dueRupees } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("treasury_screen")
    ) {
        // Treasury Directorate Header
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, ModernCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ModernBlue50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Treasury",
                                tint = ModernBlue700,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Institutional Treasury & Ledger",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = "Composite Fee Accounts • Minor Unit Precision (Paise)",
                                fontSize = 11.sp,
                                color = ModernTextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = "LEDGER BALANCED",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // KPI Ribbon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreasuryKpiCard(
                        title = "TOTAL REVENUE",
                        value = "₹${totalCollected.formatRupees()}",
                        subtitle = "${feeRecords.count { it.status == "PAID" }} Accounts Settled",
                        accentColor = ModernBlue700,
                        modifier = Modifier.weight(1f)
                    )
                    TreasuryKpiCard(
                        title = "PENDING ARREARS",
                        value = "₹${totalDue.formatRupees()}",
                        subtitle = "${feeRecords.count { it.status != "PAID" }} Accounts Due",
                        accentColor = if (totalDue > 0) GovSaffron else GovGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Modern Navigation Tabs (Payment Sandbox hidden in Phase A)
                if (FeatureAvailability.isPaymentSandboxEnabled) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = PureWhite,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = ModernBlue600,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = "Fee Register & Receipts",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) ModernBlue700 else ModernTextSecondary
                                )
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = "Payment Gateway & Sandbox",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) ModernBlue700 else ModernTextSecondary
                                )
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = ModernBlue700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Official Student Fee Register & Receipts Ledger",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            if (FeatureAvailability.isPaymentSandboxEnabled && selectedTab == 1) {
                PaymentSandboxDashboard(viewModel = viewModel)
            } else {
                FeeLedgerTabContent(
                    feeRecords = feeRecords,
                    onPrintReceipt = { record ->
                        activeReceiptPreview = record
                    }
                )
            }
        }

        // Official Receipt Voucher Preview Dialog (replaces fake printing simulation)
        activeReceiptPreview?.let { record ->
            OfficialFeeReceiptVoucherDialog(
                record = record,
                onDismiss = { activeReceiptPreview = null }
            )
        }
    }
}

@Composable
private fun TreasuryKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ModernSurfaceSubtle,
        border = BorderStroke(1.dp, ModernCardBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = ModernTextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = ModernTextSecondary
            )
        }
    }
}

@Composable
private fun FeeLedgerTabContent(
    feeRecords: List<FeeRecordEntity>,
    onPrintReceipt: (FeeRecordEntity) -> Unit
) {
    val context = LocalContext.current

    if (feeRecords.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No fee records registered for current academic batch.",
                fontSize = 13.sp,
                color = ModernTextMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(feeRecords) { record ->
                FeeRecordCard(
                    record = record,
                    onPrintReceipt = { onPrintReceipt(record) }
                )
            }
        }
    }
}

@Composable
private fun FeeRecordCard(
    record: FeeRecordEntity,
    onPrintReceipt: () -> Unit
) {
    ModernBlueCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ModernBlue50),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.gradeSection,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.studentName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary
                        )
                        Text(
                            text = "Adm No: ${record.admissionNumber} • ${record.term}",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (record.status) {
                        "PAID" -> Color(0xFFDCFCE7)
                        "PARTIAL" -> Color(0xFFFEF3C7)
                        else -> Color(0xFFFEE2E2)
                    }
                ) {
                    Text(
                        text = record.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            "PAID" -> Color(0xFF16A34A)
                            "PARTIAL" -> Color(0xFFD97706)
                            else -> Color(0xFFDC2626)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = ModernSurfaceSubtle,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Total Fee", fontSize = 9.sp, color = ModernTextMuted)
                        Text(text = "₹${record.totalRupees}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ModernTextPrimary)
                    }
                    Column {
                        Text(text = "Paid Amount", fontSize = 9.sp, color = ModernTextMuted)
                        Text(text = "₹${record.paidRupees}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                    }
                    Column {
                        Text(text = "Due Balance", fontSize = 9.sp, color = ModernTextMuted)
                        Text(text = "₹${record.dueRupees}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (record.dueRupees > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Receipt: ${record.receiptNumber ?: "SXA-RCP-AUTO"}",
                    fontSize = 10.sp,
                    color = ModernTextMuted
                )

                OutlinedButton(
                    onClick = onPrintReceipt,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ModernBlue600),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = "Receipt",
                        tint = ModernBlue700,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Receipt Voucher",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernBlue700
                    )
                }
            }
        }
    }
}

@Composable
fun OfficialFeeReceiptVoucherDialog(
    record: FeeRecordEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    tint = ModernBlue700,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Official Fee Receipt Voucher",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ModernSurfaceSubtle,
                    border = BorderStroke(1.dp, ModernCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Receipt No: ${record.receiptNumber ?: "SXA-RCP-${record.id}"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Student: ${record.studentName} (Adm: ${record.admissionNumber})",
                            fontSize = 12.sp,
                            color = GovNavyDark,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Class & Section: ${record.gradeSection} • ${record.term}",
                            fontSize = 11.sp,
                            color = ModernTextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Fee Category:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(text = record.feeType, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Amount Paid:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(text = "₹${record.paidRupees}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Remaining Due:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(
                        text = "₹${record.dueRupees}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.dueRupees > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Payment Mode:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(text = record.paymentMode, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Transaction Date:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(text = record.paidDate ?: "Recorded on Institutional Ledger", fontSize = 11.sp, color = ModernTextSecondary)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Receipt Status:", fontSize = 12.sp, color = ModernTextMuted)
                    Text(
                        text = record.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (record.status.contains("PAID", ignoreCase = true)) Color(0xFF16A34A) else Color(0xFFD97706)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = ModernBlue700)
            }
        }
    )
}

private fun Long.formatRupees(): String {
    return java.text.NumberFormat.getNumberInstance(java.util.Locale("en", "IN")).format(this)
}
