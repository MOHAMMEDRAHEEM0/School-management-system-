package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.FeeBalanceSummary
import com.example.data.repository.FeePaymentValidator
import com.example.ui.components.ModernBlueCard
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.ModernBlue200
import com.example.ui.theme.ModernBlue50
import com.example.ui.theme.ModernBlue600
import com.example.ui.theme.ModernBlue700
import com.example.ui.theme.ModernCardBorder
import com.example.ui.theme.ModernSurfaceSubtle
import com.example.ui.theme.ModernTextMuted
import com.example.ui.theme.ModernTextPrimary
import com.example.ui.theme.ModernTextSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.FeeLedgerLoadState
import com.example.ui.viewmodel.FeeWorkflowUiState
import com.example.ui.viewmodel.SchoolViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Institutional Treasury, Fee Ledgers & Official Receipts Screen (Phase D).
 * - Authorized Accountant, Principal, or Admin can search students, issue fee assessments,
 *   record manual counter payments with idempotency, inspect transaction history,
 *   and generate official receipts.
 * - Clearly distinguishes MANUAL_RECORDED counter payments from GATEWAY_VERIFIED payments
 *   and does not simulate gateway verification when no live provider is configured.
 * - Never marks records as synced before genuine server acknowledgement.
 */
@Composable
fun TreasuryScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.feeWorkflowState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var activePaymentSummary by remember { mutableStateOf<FeeBalanceSummary?>(null) }
    var activeTransactionReceipt by remember { mutableStateOf<Pair<FeePaymentTransactionEntity, FeeBalanceSummary?>?>(null) }
    var activeLegacyFeeReceipt by remember { mutableStateOf<FeeRecordEntity?>(null) }
    var showCreateAssessmentDialog by remember { mutableStateOf(false) }

    // Open newly generated official receipt automatically upon payment creation
    val newlyGeneratedReceipt = uiState.activeReceiptResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("treasury_screen")
    ) {
        // Treasury Header (properly constrained to avoid vertical badge text squeezing)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Institutional Treasury & Ledger",
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Persisted Transactions • Manual Counter & Receipt Audit",
                                fontSize = 11.sp,
                                color = ModernTextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val isPendingSync = uiState.pendingSyncTransactionCount > 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPendingSync) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, if (isPendingSync) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = if (isPendingSync) "${uiState.pendingSyncTransactionCount} PENDING SYNC" else "LEDGER READY",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPendingSync) Color(0xFFB45309) else Color(0xFF15803D),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // KPI Ribbon computed strictly from persisted ledger summaries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TreasuryKpiCard(
                        title = "CONFIRMED COLLECTED",
                        value = "₹${(uiState.totalCollectedPaise / 100L).formatRupees()}",
                        subtitle = "${uiState.settledAccountCount} Account(s) Settled",
                        accentColor = ModernBlue700,
                        modifier = Modifier.weight(1f)
                    )
                    TreasuryKpiCard(
                        title = "OUTSTANDING BALANCE",
                        value = "₹${(uiState.totalOutstandingPaise / 100L).formatRupees()}",
                        subtitle = "${uiState.dueAccountCount} Account(s) Due",
                        accentColor = if (uiState.totalOutstandingPaise > 0L) GovSaffron else GovGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Offline / Pending Outbox Sync Status Strip
                if (!uiState.isOnline || uiState.pendingSyncTransactionCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!uiState.isOnline) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                        border = BorderStroke(
                            1.dp,
                            if (!uiState.isOnline) Color(0xFFFECACA) else Color(0xFFFDE68A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (!uiState.isOnline) Icons.Default.CloudOff else Icons.Default.CloudSync,
                                    contentDescription = "Sync status",
                                    tint = if (!uiState.isOnline) GovRedAlert else GovSaffron,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (!uiState.isOnline) {
                                        "Offline mode: ${uiState.pendingSyncTransactionCount} pending transaction(s) stored in Room outbox."
                                    } else {
                                        "${uiState.pendingSyncTransactionCount} transaction(s) awaiting server acknowledgement. ${uiState.syncStatusMessage}"
                                    },
                                    fontSize = 10.5.sp,
                                    color = if (!uiState.isOnline) GovRedAlert else Color(0xFF92400E)
                                )
                            }
                            if (uiState.isOnline && uiState.pendingSyncTransactionCount > 0 && uiState.isAuthorizedToManageFees) {
                                TextButton(
                                    onClick = { viewModel.syncPendingFinancialOutbox() },
                                    enabled = !uiState.isSyncingOutbox,
                                    modifier = Modifier.testTag("sync_pending_fees_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Sync Pending",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.isSyncingOutbox) "Syncing..." else "Sync",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Validation / Feedback Banner
                uiState.validationError?.let { errorMsg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = GovRedAlert, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMsg,
                                fontSize = 11.sp,
                                color = GovRedAlert,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.clearFeeFeedback() }) {
                                Text("Dismiss", fontSize = 10.5.sp)
                            }
                        }
                    }
                }

                // Scrollable Tabs to guarantee single-line readability on all screen widths
                val treasuryTabs = listOf(
                    "Fee Register & Pay",
                    "Receipt History (${uiState.transactions.size})",
                    "Gateway Status"
                )
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.coerceIn(0, treasuryTabs.lastIndex),
                    containerColor = PureWhite,
                    edgePadding = 4.dp,
                    indicator = { tabPositions ->
                        val safeIdx = selectedTab.coerceIn(0, tabPositions.lastIndex)
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[safeIdx]),
                            color = ModernBlue600,
                            height = 3.dp
                        )
                    }
                ) {
                    treasuryTabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == idx) ModernBlue700 else ModernTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }

        // Tab Body
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> FeeRegisterAndCollectionTab(
                    uiState = uiState,
                    onQueryChange = viewModel::updateFeeStudentQuery,
                    onSelectStudent = viewModel::selectFeeStudent,
                    onOpenIssueAssessment = { showCreateAssessmentDialog = true },
                    onRecordPaymentClick = { summary -> activePaymentSummary = summary },
                    onViewReceiptClick = { summary ->
                        val latestTxn = summary.transactions.firstOrNull()
                        if (latestTxn != null) {
                            activeTransactionReceipt = latestTxn to summary
                        } else {
                            activeLegacyFeeReceipt = summary.feeRecord
                        }
                    }
                )
                1 -> TransactionReceiptHistoryTab(
                    uiState = uiState,
                    onInspectReceipt = { txn ->
                        val matchingSummary = uiState.feeSummaries.firstOrNull { it.feeRecord.id == txn.feeRecordId }
                        activeTransactionReceipt = txn to matchingSummary
                    }
                )
                else -> GatewayConfigurationNoticeTab()
            }
        }
    }

    // Create Fee Assessment Dialog (Authorized roles only)
    if (showCreateAssessmentDialog && uiState.isAuthorizedToManageFees) {
        CreateFeeAssessmentDialog(
            students = uiState.matchedStudents,
            preselectedStudent = uiState.selectedStudent,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { showCreateAssessmentDialog = false },
            onSubmit = { studentId, term, feeType, amountRupees ->
                viewModel.createFeeAssessmentForStudent(
                    studentId = studentId,
                    term = term,
                    feeType = feeType,
                    totalAmountRupees = amountRupees
                ) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (ok) showCreateAssessmentDialog = false
                }
            }
        )
    }

    // Manual Counter Payment Entry Dialog (Authorized roles only)
    activePaymentSummary?.let { summary ->
        ManualFeePaymentEntryDialog(
            summary = summary,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { activePaymentSummary = null },
            onSubmitPayment = { amountRupees, mode, reference, remarks, idempotencyKey ->
                viewModel.recordManualFeePayment(
                    studentId = summary.feeRecord.studentId,
                    feeRecordId = summary.feeRecord.id,
                    amountRupees = amountRupees,
                    paymentMode = mode,
                    transactionReference = reference,
                    remarks = remarks,
                    idempotencyKey = idempotencyKey
                ) { success, msg, _ ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (success) activePaymentSummary = null
                }
            }
        )
    }

    // Newly Generated Official Receipt Preview
    newlyGeneratedReceipt?.let { result ->
        TransactionReceiptVoucherDialog(
            transaction = result.transaction,
            feeRecord = result.updatedFeeRecord,
            outstandingPaise = result.outstandingBalancePaise,
            onDismiss = { viewModel.dismissFeeReceiptPreview() }
        )
    }

    // Inspected Transaction Receipt Preview
    activeTransactionReceipt?.let { (txn, summary) ->
        TransactionReceiptVoucherDialog(
            transaction = txn,
            feeRecord = summary?.feeRecord,
            outstandingPaise = summary?.outstandingBalancePaise ?: 0L,
            onDismiss = { activeTransactionReceipt = null }
        )
    }

    // Legacy Fee Record Voucher Preview (when no itemized transaction exists)
    activeLegacyFeeReceipt?.let { record ->
        OfficialFeeReceiptVoucherDialog(
            record = record,
            onDismiss = { activeLegacyFeeReceipt = null }
        )
    }
}

@Composable
private fun FeeRegisterAndCollectionTab(
    uiState: FeeWorkflowUiState,
    onQueryChange: (String) -> Unit,
    onSelectStudent: (String?) -> Unit,
    onOpenIssueAssessment: () -> Unit,
    onRecordPaymentClick: (FeeBalanceSummary) -> Unit,
    onViewReceiptClick: (FeeBalanceSummary) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Student Search & Issue Fee Assessment Controls for Accountant, Principal, Admin
        if (uiState.isAuthorizedToManageFees) {
            item {
                ModernBlueCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Find Student & Fee Accounts",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            OutlinedButton(
                                onClick = onOpenIssueAssessment,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("issue_fee_assessment_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Issue Fee", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Fee Demand", fontSize = 11.sp, fontWeight = FontWeight.Bold, softWrap = false)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uiState.studentSearchQuery,
                            onValueChange = onQueryChange,
                            placeholder = { Text("Search by student name, admission no, or grade...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Student") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fee_student_search_input")
                        )

                        if (uiState.matchedStudents.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = uiState.selectedStudent == null,
                                        onClick = { onSelectStudent(null) },
                                        label = { Text("All Students", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                                    )
                                }
                                items(uiState.matchedStudents, key = { it.id }) { stu ->
                                    val isSelected = uiState.selectedStudent?.id == stu.id
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectStudent(if (isSelected) null else stu.id) },
                                        label = {
                                            Text(
                                                text = "${stu.name} (${stu.admissionNumber})",
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Handle Real Loading, Restricted, Empty, Error, Ready States
        when (val state = uiState.loadState) {
            FeeLedgerLoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ModernBlue600)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Loading persisted financial ledger...", fontSize = 12.sp, color = ModernTextMuted)
                        }
                    }
                }
            }

            FeeLedgerLoadState.Restricted -> {
                item {
                    ModernBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Restricted", tint = GovRedAlert, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Access Restricted: Financial Desk Authorization Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovNavyDark
                                )
                                Text(
                                    text = "Role '${uiState.actorRoleName}' cannot view or modify institutional fee ledgers. Only Accountant, Principal, Admin, or a Student/Parent for their own account may access fee receipts.",
                                    fontSize = 11.5.sp,
                                    color = ModernTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            is FeeLedgerLoadState.Error -> {
                item {
                    ModernBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Financial Ledger Error", fontWeight = FontWeight.Bold, color = GovRedAlert)
                            Text(state.message, fontSize = 12.sp, color = ModernTextSecondary)
                        }
                    }
                }
            }

            FeeLedgerLoadState.Empty -> {
                item {
                    ModernBlueCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = "Empty Ledger",
                                tint = ModernTextMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.selectedStudent?.let {
                                    "No persisted fee assessments found for ${it.name} (${it.admissionNumber})."
                                } ?: "No persisted fee records found for this scope.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ModernTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isAuthorizedToManageFees) {
                                    "Use 'New Fee Demand' above to assess fees for a student and record manual payments."
                                } else {
                                    "Fee assessments and official receipts issued by the Accounts Office will appear here."
                                },
                                fontSize = 11.5.sp,
                                color = ModernTextMuted
                            )
                        }
                    }
                }
            }

            is FeeLedgerLoadState.Ready -> {
                items(state.summaries, key = { it.feeRecord.id }) { summary ->
                    FeeSummaryCard(
                        summary = summary,
                        canRecordPayment = uiState.isAuthorizedToManageFees,
                        onRecordPayment = { onRecordPaymentClick(summary) },
                        onViewReceipt = { onViewReceiptClick(summary) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeeSummaryCard(
    summary: FeeBalanceSummary,
    canRecordPayment: Boolean,
    onRecordPayment: () -> Unit,
    onViewReceipt: () -> Unit
) {
    val record = summary.feeRecord
    val totalRupees = summary.totalAmountPaise / 100L
    val paidRupees = summary.confirmedPaidPaise / 100L
    val dueRupees = summary.outstandingBalancePaise / 100L
    val latestTxn = summary.transactions.firstOrNull()

    ModernBlueCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fee_card_${record.id}"),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(ModernBlue50),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.gradeSection.replace("Grade ", "").take(5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = record.studentName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Adm: ${record.admissionNumber} • ${record.term}",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = record.feeType,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = ModernTextSecondary
            )

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
                        Text(text = "Total Assessed", fontSize = 9.sp, color = ModernTextMuted)
                        Text(
                            text = "₹${totalRupees.formatRupees()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary
                        )
                    }
                    Column {
                        Text(text = "Paid (${summary.confirmedTransactionCount} txns)", fontSize = 9.sp, color = ModernTextMuted)
                        Text(
                            text = "₹${paidRupees.formatRupees()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                    Column {
                        Text(text = "Outstanding", fontSize = 9.sp, color = ModernTextMuted)
                        Text(
                            text = "₹${dueRupees.formatRupees()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dueRupees > 0L) Color(0xFFDC2626) else Color(0xFF16A34A)
                        )
                    }
                }
            }

            if (latestTxn != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest Receipt: ${latestTxn.receiptNumber}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ModernTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    SyncAndVerificationPills(transaction = latestTxn)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canRecordPayment && summary.outstandingBalancePaise > 0L) {
                    Button(
                        onClick = onRecordPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("record_payment_btn_${record.id}")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = "Record Payment", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Record Manual Payment",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (latestTxn != null || record.receiptNumber != null) {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ModernBlue600),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("view_receipt_btn_${record.id}")
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = "View Receipt", tint = ModernBlue700, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Receipt",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionReceiptHistoryTab(
    uiState: FeeWorkflowUiState,
    onInspectReceipt: (FeePaymentTransactionEntity) -> Unit
) {
    if (uiState.transactions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No persisted payment transactions recorded yet.",
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
            items(uiState.transactions, key = { it.transactionId }) { txn ->
                ModernBlueCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = txn.receiptNumber,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ModernBlue700
                                )
                                Text(
                                    text = "${txn.studentName} • ${txn.paymentMode} • Ref: ${txn.transactionReference}",
                                    fontSize = 11.sp,
                                    color = ModernTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "₹${txn.amountRupees.formatRupees()}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF16A34A)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SyncAndVerificationPills(transaction = txn)

                            TextButton(onClick = { onInspectReceipt(txn) }) {
                                Text("View Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (!txn.serverAckToken.isNullOrBlank()) {
                            Text(
                                text = "Server Ack Token: ${txn.serverAckToken}",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GatewayConfigurationNoticeTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernBlueCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = "Verification Policy", tint = ModernBlue700)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Online Payment Gateway Not Configured",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This institutional build has no configured live payment gateway provider or webhook merchant credentials. Online self-checkout is disabled to preserve strict financial integrity.",
                    fontSize = 12.sp,
                    color = ModernTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = ModernCardBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• MANUAL_RECORDED: Authorized Accountant, Principal, or Admin records Cash, Cheque, Bank Transfer, DD, or Manual UPI payments at the Bursar desk with an immutable audit entry and outbox sync.",
                    fontSize = 11.sp,
                    color = ModernTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• GATEWAY_VERIFIED: Reserved exclusively for backend server webhooks verified via cryptographic signature (Firestore rule enforces isServerService == true).",
                    fontSize = 11.sp,
                    color = ModernTextPrimary
                )
            }
        }
    }
}

@Composable
private fun SyncAndVerificationPills(transaction: FeePaymentTransactionEntity) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val isGateway = transaction.isGatewayVerified
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isGateway) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
            border = BorderStroke(0.8.dp, if (isGateway) ModernBlue200 else ModernCardBorder)
        ) {
            Text(
                text = if (isGateway) "GATEWAY VERIFIED" else "MANUAL COUNTER",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGateway) ModernBlue700 else GovNavyDark,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        val isSynced = transaction.isSyncedToCentral && transaction.syncStatus == FeePaymentTransactionEntity.SYNC_SYNCED
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSynced) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
        ) {
            Text(
                text = if (isSynced) "SYNCED" else transaction.syncStatus,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSynced) Color(0xFF15803D) else Color(0xFFB45309),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CreateFeeAssessmentDialog(
    students: List<StudentEntity>,
    preselectedStudent: StudentEntity?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (studentId: String, term: String, feeType: String, amountRupees: Long) -> Unit
) {
    var selectedStu by remember { mutableStateOf(preselectedStudent ?: students.firstOrNull()) }
    var term by remember { mutableStateOf("Term 2 (2026-27)") }
    var feeType by remember { mutableStateOf("Composite Tuition & Lab Fee") }
    var amountText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Issue New Fee Assessment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Scholar:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModernTextSecondary)
                if (students.isEmpty()) {
                    Text("No enrolled students found in this tenant.", fontSize = 11.sp, color = GovRedAlert)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(students, key = { it.id }) { stu ->
                            FilterChip(
                                selected = selectedStu?.id == stu.id,
                                onClick = { selectedStu = stu },
                                label = { Text("${stu.name} (${stu.admissionNumber})", fontSize = 10.5.sp, softWrap = false) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = term,
                    onValueChange = { term = it },
                    label = { Text("Academic Term") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = feeType,
                    onValueChange = { feeType = it },
                    label = { Text("Fee Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Total Amount (INR ₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                localError?.let {
                    Text(it, fontSize = 11.sp, color = GovRedAlert, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stu = selectedStu
                    val amt = amountText.toLongOrNull() ?: 0L
                    when {
                        stu == null -> localError = "Please select a student."
                        term.isBlank() || feeType.isBlank() -> localError = "Term and category are required."
                        amt <= 0L -> localError = "Amount must be greater than ₹0."
                        else -> {
                            localError = null
                            onSubmit(stu.id, term, feeType, amt)
                        }
                    }
                },
                enabled = !isSubmitting && students.isNotEmpty()
            ) {
                Text(if (isSubmitting) "Saving..." else "Create Assessment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ManualFeePaymentEntryDialog(
    summary: FeeBalanceSummary,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitPayment: (amountRupees: Long, mode: String, reference: String, remarks: String, idempotencyKey: String) -> Unit
) {
    val outstandingRupees = summary.outstandingBalancePaise / 100L
    val idempotencyKey = remember(summary.feeRecord.id) { UUID.randomUUID().toString() }
    var amountInput by remember { mutableStateOf(outstandingRupees.toString()) }
    var selectedMode by remember { mutableStateOf("CASH") }
    var referenceInput by remember { mutableStateOf("") }
    var remarksInput by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Record Manual Counter Payment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "${summary.feeRecord.studentName} (${summary.feeRecord.admissionNumber})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "${summary.feeRecord.feeType} • Outstanding: ₹${outstandingRupees.formatRupees()}",
                            fontSize = 11.sp,
                            color = ModernTextSecondary
                        )
                        Text(
                            text = "Source Classification: MANUAL_RECORDED (Accounts Counter)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernBlue700
                        )
                    }
                }

                Text("Payment Mode:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModernTextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(FeePaymentValidator.ALLOWED_MANUAL_PAYMENT_MODES.toList()) { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text(mode, fontSize = 10.5.sp, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Payment Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_payment_amount_input")
                )

                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = { referenceInput = it },
                    label = {
                        Text(
                            if (selectedMode == "CASH") "Counter Slip / Ref (Optional)"
                            else "Cheque / UTR / Ref No (Required)"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_payment_reference_input")
                )

                OutlinedTextField(
                    value = remarksInput,
                    onValueChange = { remarksInput = it },
                    label = { Text("Audit Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                validationError?.let {
                    Text(it, fontSize = 11.sp, color = GovRedAlert, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountInput.toLongOrNull() ?: 0L
                    when {
                        amount <= 0L -> validationError = "Payment amount must be greater than ₹0."
                        amount > outstandingRupees -> validationError = "Amount cannot exceed outstanding balance (₹$outstandingRupees)."
                        selectedMode != "CASH" && referenceInput.isBlank() -> {
                            validationError = "Reference number is required for $selectedMode."
                        }
                        else -> {
                            validationError = null
                            onSubmitPayment(amount, selectedMode, referenceInput.trim(), remarksInput.trim(), idempotencyKey)
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                modifier = Modifier.testTag("confirm_manual_payment_btn")
            ) {
                Text(if (isSubmitting) "Recording..." else "Record & Issue Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TransactionReceiptVoucherDialog(
    transaction: FeePaymentTransactionEntity,
    feeRecord: FeeRecordEntity?,
    outstandingPaise: Long,
    onDismiss: () -> Unit
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(transaction.timestamp))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Receipt, contentDescription = "Official Receipt", tint = ModernBlue700, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Official Fee Receipt Voucher", fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
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
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Receipt No: ${transaction.receiptNumber}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ModernBlue700
                        )
                        Text(
                            text = "Txn ID: ${transaction.transactionId}",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ModernTextMuted
                        )
                        Text(
                            text = "Student: ${transaction.studentName} (${transaction.studentId})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GovNavyDark
                        )
                        if (feeRecord != null) {
                            Text(
                                text = "${feeRecord.gradeSection} • ${feeRecord.term} • ${feeRecord.feeType}",
                                fontSize = 10.5.sp,
                                color = ModernTextSecondary
                            )
                        }
                    }
                }

                ReceiptKeyValueRow("Amount Paid:", "₹${transaction.amountRupees.formatRupees()}", Color(0xFF16A34A), true)
                ReceiptKeyValueRow(
                    "Remaining Balance:",
                    "₹${(outstandingPaise / 100L).formatRupees()}",
                    if (outstandingPaise > 0L) GovRedAlert else Color(0xFF16A34A),
                    true
                )
                ReceiptKeyValueRow("Payment Mode:", transaction.paymentMode)
                ReceiptKeyValueRow("Reference:", transaction.transactionReference)
                ReceiptKeyValueRow(
                    "Verification Source:",
                    if (transaction.isGatewayVerified) "ONLINE GATEWAY VERIFIED" else "MANUAL COUNTER ENTRY"
                )
                ReceiptKeyValueRow(
                    "Recorded By:",
                    transaction.cashierName.ifBlank { transaction.recordedByUserId } +
                        if (transaction.recordedByRole.isNotBlank()) " (${transaction.recordedByRole})" else ""
                )
                ReceiptKeyValueRow("Timestamp:", dateStr)
                ReceiptKeyValueRow(
                    "Cloud Sync Status:",
                    if (transaction.isSyncedToCentral) "SYNCED (Ack: ${transaction.serverAckToken ?: "CONFIRMED"})"
                    else "${transaction.syncStatus} (Queued locally until server confirms)",
                    if (transaction.isSyncedToCentral) Color(0xFF15803D) else Color(0xFFB45309)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = ModernBlue700)
            }
        }
    )
}

@Composable
private fun ReceiptKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = ModernTextPrimary,
    boldValue: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(text = label, fontSize = 11.sp, color = ModernTextMuted, modifier = Modifier.padding(end = 8.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
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
                color = ModernTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 16.5.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = ModernTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                    contentDescription = "Receipt",
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
                            text = "Receipt No: ${record.receiptNumber ?: "PENDING_RECEIPT"}",
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

                ReceiptKeyValueRow("Fee Category:", record.feeType)
                ReceiptKeyValueRow("Amount Paid:", "₹${record.paidRupees.formatRupees()}", Color(0xFF16A34A), true)
                ReceiptKeyValueRow(
                    "Remaining Due:",
                    "₹${record.dueRupees.formatRupees()}",
                    if (record.dueRupees > 0) Color(0xFFDC2626) else Color(0xFF16A34A),
                    true
                )
                ReceiptKeyValueRow("Payment Mode:", record.paymentMode)
                ReceiptKeyValueRow("Transaction Date:", record.paidDate ?: "Recorded on Institutional Ledger")
                ReceiptKeyValueRow("Receipt Status:", record.status)
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
    return NumberFormat.getNumberInstance(Locale("en", "IN")).format(this)
}
