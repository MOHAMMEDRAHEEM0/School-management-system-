package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.core.FeatureAvailability
import com.example.data.local.entities.StaffEntity
import com.example.ui.components.AiAcademicToolsSection
import com.example.ui.components.DynamicTimetablePeriodTracker
import com.example.ui.components.ExecutiveCommandShortcutsDeck
import com.example.ui.components.GpsTransportRouteTracker
import com.example.ui.components.InchargeLeaveApprovalDeck
import com.example.ui.components.SchoolCircularNoticeBoard
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.OfficialSealStamp
import com.example.ui.components.OfficialWhatsAppButton
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
import kotlinx.coroutines.launch

@Composable
fun PrincipalDashboard(
    viewModel: SchoolViewModel,
    onNavigateToAuditLogs: () -> Unit,
    onNavigateToWhatsAppHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val institution by viewModel.institution.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val students by viewModel.students.collectAsState()
    val batches by viewModel.registerBatches.collectAsState()
    val certificates by viewModel.certificates.collectAsState()
    val idCards by viewModel.idCardRecords.collectAsState()
    val inchargeRequests by viewModel.inchargeRequests.collectAsState()
    val bills by viewModel.institutionalBills.collectAsState()
    val complianceDocs by viewModel.complianceDocuments.collectAsState()

    // Add Staff Dialog State
    var showAddStaffDialog by remember { mutableStateOf(false) }
    var newStaffName by remember { mutableStateOf("") }
    var newStaffRole by remember { mutableStateOf("TEACHING") }
    var newStaffDesignation by remember { mutableStateOf("") }
    var newStaffDept by remember { mutableStateOf("") }
    var newStaffPhone by remember { mutableStateOf("") }
    var newStaffEmail by remember { mutableStateOf("") }
    var addStaffError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("principal_dashboard")
    ) {
        // Executive Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Executive Directorate",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(text = "SUPER ADMIN", color = GovGreen)
                }
                Text(
                    text = "${institution?.name ?: "St. Xavier's International Academy"} | Master Clearance",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted
                )
            }

            IconButton(
                onClick = onNavigateToAuditLogs,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GovNavyPrimary.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Audit Logs",
                    tint = GovNavyPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // High-Level Institutional KPIs Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Students KPI
            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovNavyPrimary,
                headerTitle = "ENROLLMENT"
            ) {
                Column {
                    Text(
                        text = "${students.size + 420}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Active Scholars",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                    Text(
                        text = "Classes: Nur - 10 (A-D)",
                        fontSize = 8.5.sp,
                        color = GovGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Staff Attendance KPI
            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovGreen,
                headerTitle = "STAFF ATTENDANCE"
            ) {
                Column {
                    val presentStaff = staffList.count { it.isPresentToday }
                    val totalStaff = staffList.size.coerceAtLeast(1)
                    val pct = (presentStaff * 100) / totalStaff
                    Text(
                        text = "$pct%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(
                        text = "$presentStaff / $totalStaff Present",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                    Text(
                        text = "Today's biometric sync",
                        fontSize = 8.5.sp,
                        color = GovNavyPrimary
                    )
                }
            }

            // Approvals KPI
            val pendingApprovalsCount = batches.count { it.status == "VERIFIED_BY_INCHARGE" } +
                    certificates.count { it.applicationStatus == "VERIFIED_BY_INCHARGE" } +
                    idCards.count { it.verificationStatus == "VERIFIED_BY_INCHARGE" }

            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = if (pendingApprovalsCount > 0) GovSaffron else GovSlateMuted,
                headerTitle = "APPROVALS"
            ) {
                Column {
                    Text(
                        text = "$pendingApprovalsCount",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (pendingApprovalsCount > 0) GovSaffron else GovSlateDark
                    )
                    Text(
                        text = "Pending Desk",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                    Text(
                        text = if (pendingApprovalsCount > 0) "Requires Seal" else "All Cleared",
                        fontSize = 8.5.sp,
                        color = if (pendingApprovalsCount > 0) GovSaffron else GovGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Executive Command Shortcuts Deck (Gemini AI, WhatsApp Hub / Push, PDF Export)
        ExecutiveCommandShortcutsDeck(
            viewModel = viewModel,
            onNavigateToWhatsAppHub = onNavigateToWhatsAppHub
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Segmented Control for Principal Modules
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
                    text = { Text("Approvals", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Staff Registry", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Assets & Bills", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                if (FeatureAvailability.isAiAcademicToolsEnabled) {
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("AI Academic Tools", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }
                if (FeatureAvailability.isPaymentSandboxEnabled) {
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Payment Sandbox", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_principal_payment_sandbox")
                    )
                }
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Circulars & Notices", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    text = { Text("Leave Directorate", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 7,
                    onClick = { selectedTab = 7 },
                    text = { Text("Campus Timetable", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 8,
                    onClick = { selectedTab = 8 },
                    text = { Text("Fleet & Transport", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: MASTER EXECUTIVE APPROVALS DESK
        // =====================================================================
        if (selectedTab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Section: Daily Attendance Batches
                Text(
                    text = "Daily Register Attendance Batches (Verified by In-Charge):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                val pendingBatches = batches.filter { it.status == "VERIFIED_BY_INCHARGE" }
                if (pendingBatches.isEmpty()) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("All class attendance registers have been approved with Executive Seal.", fontSize = 11.sp, color = GovSlateMuted)
                        }
                    }
                } else {
                    pendingBatches.forEach { batch ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            headerRibbonColor = GovSaffron,
                            headerTitle = "ATTENDANCE BATCH - CLASS ${batch.classSection}",
                            headerBadge = "AWAITING PRINCIPAL SEAL"
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Batch ID: ${batch.batchId}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = GovSlateDark
                                        )
                                        Text(
                                            text = "Scanned By: ${batch.teacherName} | Incharge Verified",
                                            fontSize = 10.sp,
                                            color = GovSlateMuted
                                        )
                                        Text(
                                            text = "Present: ${batch.presentCount} | Absent: ${batch.absentCount} (Total: ${batch.totalStudents})",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GovNavyPrimary
                                        )
                                    }

                                    OfficialSealStamp(
                                        title = "VERIFIED",
                                        code = "BY INCHARGE",
                                        modifier = Modifier.size(52.dp),
                                        color = GovGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                GlassButton(
                                    text = "Affix Principal Executive Seal & Commit Ledger",
                                    onClick = {
                                        viewModel.approveRegisterBatchPrincipal(batch.batchId)
                                        Toast.makeText(context, "Executive Seal Affixed: Batch committed permanently!", Toast.LENGTH_SHORT).show()
                                    },
                                    color = GovNavyPrimary,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Section: Student Certificates
                Text(
                    text = "Institutional Certificates Approval (Bonafide / Transfer / Character):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                certificates.forEach { cert ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = when (cert.applicationStatus) {
                            "APPROVED_BY_PRINCIPAL" -> GovGreen
                            "VERIFIED_BY_INCHARGE" -> GovSaffron
                            else -> GovNavyPrimary
                        },
                        headerTitle = "${cert.certificateType} CERTIFICATE",
                        headerBadge = cert.applicationStatus.replace("_", " ")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = cert.studentName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "ID: ${cert.studentId} | Class: ${cert.grade}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                    Text(
                                        text = "Purpose: ${cert.purposeOrAchievement}",
                                        fontSize = 10.5.sp,
                                        color = GovSlateText
                                    )
                                    if (!cert.inchargeRemarks.isNullOrBlank()) {
                                        Text(
                                            text = "Incharge: ${cert.inchargeRemarks}",
                                            fontSize = 9.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = GovGreen
                                        )
                                    }
                                }

                                if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") {
                                    OfficialSealStamp(
                                        title = "EXECUTIVE",
                                        code = "APPROVED",
                                        modifier = Modifier.size(54.dp),
                                        color = GovGreen
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (cert.applicationStatus == "VERIFIED_BY_INCHARGE") {
                                GlassButton(
                                    text = "Authorize with Institutional Master Barcode",
                                    onClick = {
                                        viewModel.approveCertificatePrincipal(
                                            cert.certificateId,
                                            "Approved and digitally signed under Institutional Seal No. 9942"
                                        )
                                        Toast.makeText(context, "Certificate authorized! Download unlocked for student.", Toast.LENGTH_SHORT).show()
                                    },
                                    color = GovGreen,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                                    }
                                )
                            } else if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OfficialWhatsAppButton(
                                        phone = "9123456780",
                                        message = "Official Notice from St. Xavier's Academy: Your Bonafide Certificate ${cert.certificateId} has been approved by the Principal. Download Token: ${cert.secureWhatsAppToken ?: "SXA-CERT-9942"}",
                                        label = "Send to Parent",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GlassOutlinedButton(
                                        text = "Seal Validated",
                                        onClick = { Toast.makeText(context, "Certificate Barcode: ${cert.digitalBarcode}", Toast.LENGTH_SHORT).show() },
                                        color = GovNavyPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 1: STAFF MANAGEMENT & UNIQUE PHONE NUMBER CONSTRAINT
        // =====================================================================
        if (selectedTab == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Institutional Staff & Faculty Roster",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "Strict Unique Mobile Number Rule Enforced on Database",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }

                    GlassButton(
                        text = "Add Staff",
                        onClick = {
                            addStaffError = null
                            showAddStaffDialog = true
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                staffList.forEach { staff ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (staff.roleType == "PRINCIPAL") GovNavyPrimary else if (staff.isClassIncharge) GovSaffron else GovSlateDark,
                        headerTitle = "${staff.roleType} - ${staff.department.uppercase()}",
                        headerBadge = if (staff.isPresentToday) "PRESENT TODAY" else "ABSENT"
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(GovNavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(22.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = staff.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = staff.designation,
                                        fontSize = 10.sp,
                                        color = GovSlateText
                                    )
                                    Text(
                                        text = "Phone: +91 ${staff.phone} (Unique) | ID: ${staff.id}",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GovNavyPrimary
                                    )
                                }

                                if (staff.isOtpVerified) {
                                    GlassBadge(text = "2FA VERIFIED", color = GovGreen)
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: INSTITUTIONAL UTILITIES & REGULATORY COMPLIANCE
        // =====================================================================
        if (selectedTab == 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Institutional Utility Bills & Operational Disbursements:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                bills.forEach { bill ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (bill.paymentStatus == "PAID") GovGreen else GovSaffron,
                        headerTitle = "${bill.billType} EXPENSE VOUCHER",
                        headerBadge = bill.paymentStatus
                    ) {
                        Column {
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
                                        text = "Invoice: ${bill.billNumber} | Month: ${bill.billingMonth}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                    Text(
                                        text = "Due: ${bill.dueDate}",
                                        fontSize = 9.5.sp,
                                        color = GovRedAlert
                                    )
                                }
                                Text(
                                    text = " ${bill.amountRupees}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyDark
                                )
                            }

                            if (bill.paymentStatus != "PAID") {
                                Spacer(modifier = Modifier.height(8.dp))
                                GlassButton(
                                    text = "Record Institutional Payment Voucher",
                                    onClick = {
                                        viewModel.recordBillPayment(bill.billId, "DISB-2026-${(1000..9999).random()}")
                                        Toast.makeText(context, "Payment voucher recorded and debited from Academy ledger.", Toast.LENGTH_SHORT).show()
                                    },
                                    color = GovNavyPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Government & Board Regulatory Compliance Certificates:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                complianceDocs.forEach { doc ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = "GOVERNMENT CLEARANCE",
                        headerBadge = doc.status
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = GovGreen, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = doc.documentTitle,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Reg No: ${doc.registrationNumber} | Authority: ${doc.authorityName}",
                                    fontSize = 9.5.sp,
                                    color = GovSlateMuted
                                )
                                Text(
                                    text = "Valid through: ${doc.expiryDate} (Official Record Active)",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovGreen
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 3: AI ACADEMIC TOOLS (GEMINI 3.5 FLASH CO-PILOT)
        // =====================================================================
        if (FeatureAvailability.isAiAcademicToolsEnabled && selectedTab == 3) {
            AiAcademicToolsSection(
                viewModel = viewModel,
                userRole = "PRINCIPAL",
                modifier = Modifier.fillMaxWidth()
            )
        }

        // =====================================================================
        // TAB 4: PAYMENT SANDBOX DASHBOARD (ADMIN WEBHOOK & LEDGER VERIFIER)
        // =====================================================================
        if (FeatureAvailability.isPaymentSandboxEnabled && selectedTab == 4) {
            com.example.ui.components.PaymentSandboxDashboard(
                viewModel = viewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // =====================================================================
        // TAB 5: CAMPUS NOTICE BOARD & OFFICIAL CIRCULARS
        // =====================================================================
        if (selectedTab == 5) {
            SchoolCircularNoticeBoard()
        }

        // =====================================================================
        // TAB 6: MASTER LEAVE DIRECTORATE
        // =====================================================================
        if (selectedTab == 6) {
            InchargeLeaveApprovalDeck(
                classSection = "All Sections"
            )
        }

        // =====================================================================
        // TAB 7: CAMPUS-WIDE TIMETABLE OVERVIEW
        // =====================================================================
        if (selectedTab == 7) {
            DynamicTimetablePeriodTracker(
                classSection = "10-A"
            )
        }

        // =====================================================================
        // TAB 8: LOGISTICS & BUS FLEET MONITOR
        // =====================================================================
        if (selectedTab == 8) {
            GpsTransportRouteTracker(
                routeNumber = "Fleet Unit #1",
                routeName = "Civil Lines - Sector 5 Express",
                studentName = "Executive Campus Transport Monitor"
            )
        }
    }

    // =========================================================================
    // ADD STAFF DIALOG (Enforcing Strict Unique Mobile Number)
    // =========================================================================
    if (showAddStaffDialog) {
        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = {
                Text(
                    text = "Register New Institutional Staff",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = GovNavyDark
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (addStaffError != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(6.dp))
                                .border(1.dp, GovRedAlert, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(text = addStaffError ?: "", color = GovRedAlert, fontSize = 10.sp)
                        }
                    }

                    GlassTextField(
                        value = newStaffName,
                        onValueChange = { newStaffName = it },
                        placeholder = "e.g. Dr. Rajesh Khanna",
                        label = "Full Name"
                    )

                    GlassTextField(
                        value = newStaffPhone,
                        onValueChange = { newStaffPhone = it },
                        placeholder = "10-digit mobile number (Must be Unique)",
                        label = "Registered Mobile Number"
                    )

                    GlassTextField(
                        value = newStaffDesignation,
                        onValueChange = { newStaffDesignation = it },
                        placeholder = "e.g. Senior Faculty in Chemistry",
                        label = "Designation"
                    )

                    GlassTextField(
                        value = newStaffDept,
                        onValueChange = { newStaffDept = it },
                        placeholder = "e.g. Science Division",
                        label = "Department"
                    )

                    GlassTextField(
                        value = newStaffEmail,
                        onValueChange = { newStaffEmail = it },
                        placeholder = "e.g. r.khanna@stxaviers.edu",
                        label = "Institutional Email"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Validate & Register",
                    onClick = {
                        if (newStaffName.isBlank() || newStaffPhone.isBlank()) {
                            addStaffError = "Name and Unique Mobile Number are required!"
                            return@GlassButton
                        }
                        val cleanPhone = newStaffPhone.trim()
                        val newStaff = StaffEntity(
                            id = "FAC-NEW-${(100..999).random()}",
                            name = newStaffName.trim(),
                            roleType = newStaffRole,
                            designation = newStaffDesignation.trim().ifEmpty { "Faculty" },
                            department = newStaffDept.trim().ifEmpty { "Academic Division" },
                            phone = cleanPhone,
                            email = newStaffEmail.trim().ifEmpty { "faculty@stxaviers.edu" },
                            qualification = "M.Sc., B.Ed.",
                            experienceYears = 5,
                            govtRegNumber = "SXA-FAC-${(200..999).random()}",
                            isPreAuthorizedByPrincipal = true,
                            isOtpVerified = false,
                            isPresentToday = true,
                            checkInTime = "08:00 AM",
                            avatarColorHex = "#0B2545"
                        )
                        viewModel.addStaffMember(newStaff) { success, msg ->
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                showAddStaffDialog = false
                            } else {
                                addStaffError = msg
                            }
                        }
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(
                    text = "Cancel",
                    onClick = { showAddStaffDialog = false }
                )
            }
        )
    }
}
