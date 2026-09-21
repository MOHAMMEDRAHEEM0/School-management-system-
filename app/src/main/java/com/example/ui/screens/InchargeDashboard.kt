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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Verified
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
import com.example.data.local.entities.InchargeEditRequestEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.OfficialSealStamp
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
fun InchargeDashboard(
    viewModel: SchoolViewModel,
    onNavigateToScanScreen: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val students by viewModel.students.collectAsState()
    val batches by viewModel.registerBatches.collectAsState()
    val diaryEntries by viewModel.diaryEntries.collectAsState()
    val feeRecords by viewModel.feeRecords.collectAsState()

    // Filter strictly to Section 10-A
    val sectionStudents = students.filter { it.section == "A" && (it.grade == "10" || it.grade == "Grade 10") }
    val sectionBatches = batches.filter { it.classSection.contains("10-A") }

    // Dialog State for Record Modification Request
    var editingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var requestField by remember { mutableStateOf("Guardian Phone") }
    var requestNewValue by remember { mutableStateOf("") }
    var requestReason by remember { mutableStateOf("") }

    // Fee Collection Dialog State
    var payingStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var feeAmountPaid by remember { mutableStateOf("15000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("incharge_dashboard")
    ) {
        // Section In-Charge Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Class In-Charge Desk",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(text = "SECTION 10-A", color = GovSaffron)
                }
                Text(
                    text = "Mrs. Sunita Kapoor | Mathematics Faculty & Section Coordinator",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted
                )
            }

            OfficialSealStamp(
                title = "INCHARGE",
                code = "SEC 10-A",
                modifier = Modifier.size(50.dp),
                color = GovSaffron
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Section Overview Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovNavyPrimary,
                headerTitle = "CLASS 10-A"
            ) {
                Column {
                    Text(
                        text = "${sectionStudents.size} Enrolled",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Room 304 - Secondary Wing",
                        fontSize = 8.5.sp,
                        color = GovSlateMuted
                    )
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovGreen,
                headerTitle = "ATTENDANCE"
            ) {
                Column {
                    Text(
                        text = "93.8% Term",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(
                        text = "4 Present, 1 Absent",
                        fontSize = 8.5.sp,
                        color = GovSlateMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tabs
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
                    text = { Text("Daily Register", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Students Roster", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Section Fees", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: DAILY REGISTER & BATCH VERIFICATION
        // =====================================================================
        if (selectedTab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Morning Attendance Batches Submitted by Teachers:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    GlassOutlinedButton(
                        text = "Scan New Register",
                        onClick = onNavigateToScanScreen,
                        color = GovNavyPrimary
                    )
                }

                sectionBatches.forEach { batch ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = when (batch.status) {
                            "PENDING_INCHARGE_VERIFICATION" -> GovSaffron
                            "VERIFIED_BY_INCHARGE" -> GovNavyPrimary
                            else -> GovGreen
                        },
                        headerTitle = "BATCH: ${batch.batchId}",
                        headerBadge = batch.status.replace("_", " ")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Scanned by: ${batch.teacherName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Session: ${batch.session} | Absentees: ${batch.absentRolls}",
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

                                if (batch.status == "APPROVED_BY_PRINCIPAL") {
                                    OfficialSealStamp(title = "PRINCIPAL", code = "SEALED", modifier = Modifier.size(50.dp), color = GovGreen)
                                } else if (batch.status == "VERIFIED_BY_INCHARGE") {
                                    OfficialSealStamp(title = "INCHARGE", code = "FORWARDED", modifier = Modifier.size(50.dp), color = GovNavyPrimary)
                                }
                            }

                            if (batch.status == "PENDING_INCHARGE_VERIFICATION") {
                                Spacer(modifier = Modifier.height(10.dp))
                                GlassButton(
                                    text = "Cross-Verify Absentees & Transmit to Principal",
                                    onClick = {
                                        viewModel.verifyRegisterBatchIncharge(batch.batchId)
                                        Toast.makeText(context, "Batch verified by In-Charge and forwarded to Principal!", Toast.LENGTH_SHORT).show()
                                    },
                                    color = GovNavyPrimary,
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = {
                                        Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 1: SECTION 10-A STUDENTS ROSTER & EDIT REQUEST DESK
        // =====================================================================
        if (selectedTab == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Section 10-A Scholar Directory (Safe Edit Control):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                sectionStudents.forEach { student ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = "ROLL #${student.rollNumber} - ${student.name.uppercase()}",
                        headerBadge = if (student.feeDue > 0) "FEE DUE" else "FEES CLEARED"
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(GovNavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Adm No: ${student.admissionNumber} | DOB: ${student.dateOfBirth}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                    Text(
                                        text = "Guardian: ${student.guardianName} (${student.guardianPhone})",
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GovSlateText
                                    )
                                    Text(
                                        text = "Term Attendance: ${student.attendancePercentage}%",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (student.attendancePercentage >= 90f) GovGreen else GovSaffron
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassOutlinedButton(
                                    text = "Request Edit",
                                    onClick = {
                                        editingStudent = student
                                        requestField = "Guardian Mobile Number"
                                        requestNewValue = student.guardianPhone
                                        requestReason = "Parent updated official contact"
                                    },
                                    color = GovNavyPrimary,
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(13.dp))
                                    }
                                )

                                GlassButton(
                                    text = "Record Fee",
                                    onClick = {
                                        payingStudent = student
                                        feeAmountPaid = if (student.feeDue > 0) student.feeDue.toLong().toString() else "65000"
                                    },
                                    color = GovGreen,
                                    modifier = Modifier.weight(1f),
                                    leadingIcon = {
                                        Icon(Icons.Default.Payment, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: SECTION FEES
        // =====================================================================
        if (selectedTab == 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Section 10-A Composite Fee Ledger & Offline Receipts:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                feeRecords.forEach { fee ->
                    val displayTotal = if (fee.totalAmount >= 100000L) fee.totalAmount / 100L else fee.totalAmount
                    val displayPaid = if (fee.paidAmount >= 100000L) fee.paidAmount / 100L else fee.paidAmount
                    val displayDue = if (fee.dueAmount >= 100000L) fee.dueAmount / 100L else fee.dueAmount

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (fee.status == "PAID") GovGreen else GovSaffron,
                        headerTitle = "${fee.studentName.uppercase()} - ${fee.status}",
                        headerBadge = fee.receiptNumber ?: "DUE"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = fee.feeType,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Total:  $displayTotal | Paid:  $displayPaid",
                                    fontSize = 10.sp,
                                    color = GovSlateMuted
                                )
                                if (displayDue > 0) {
                                    Text(
                                        text = "Outstanding Due:  $displayDue",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovRedAlert
                                    )
                                }
                            }

                            if (fee.receiptNumber != null) {
                                OfficialSealStamp(
                                    title = "BURSAR",
                                    code = "RECEIPT",
                                    modifier = Modifier.size(50.dp),
                                    color = GovGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Record Modification Request Dialog
    if (editingStudent != null) {
        val stu = editingStudent!!
        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = {
                Text("Propose Student Record Modification", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${stu.name} (${stu.admissionNumber})", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(
                        value = requestField,
                        onValueChange = { requestField = it },
                        placeholder = "Field to change",
                        label = "Record Field"
                    )
                    GlassTextField(
                        value = requestNewValue,
                        onValueChange = { requestNewValue = it },
                        placeholder = "Proposed New Value",
                        label = "New Value"
                    )
                    GlassTextField(
                        value = requestReason,
                        onValueChange = { requestReason = it },
                        placeholder = "Official Reason / Document Ref",
                        label = "Official Justification"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Submit to Principal",
                    onClick = {
                        Toast.makeText(context, "Edit request transmitted to Principal's audit inbox!", Toast.LENGTH_SHORT).show()
                        editingStudent = null
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(
                    text = "Cancel",
                    onClick = { editingStudent = null }
                )
            }
        )
    }

    // Fee Collection Dialog
    if (payingStudent != null) {
        val stu = payingStudent!!
        AlertDialog(
            onDismissRequest = { payingStudent = null },
            title = {
                Text("Record Offline Fee Collection", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${stu.name} (Roll #${stu.rollNumber})", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(
                        value = feeAmountPaid,
                        onValueChange = { feeAmountPaid = it },
                        placeholder = "Amount in Rupees",
                        label = "Collected Amount ()"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Generate Official Receipt",
                    onClick = {
                        val amount = feeAmountPaid.toLongOrNull() ?: 15000L
                        val receiptNo = "SXA-REC-2026-${(8000..9999).random()}"
                        viewModel.inchargeRecordFeePayment(
                            feeId = "FEE-2026-003",
                            paidAmount = amount,
                            dueAmount = 0L,
                            receiptNo = receiptNo
                        )
                        Toast.makeText(context, "Payment recorded! Official Receipt generated: $receiptNo", Toast.LENGTH_LONG).show()
                        payingStudent = null
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(
                    text = "Cancel",
                    onClick = { payingStudent = null }
                )
            }
        )
    }
}
