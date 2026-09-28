package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentLeaveRequestEntity
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

/**
 * Dedicated Multi-Layered Student Attendance Portal (Read-Only Abstraction).
 *
 * Implements strict RBAC isolation for Students and Parents:
 * 1. Zero Write Access to Daily Roll Call: Faculty registers, checkboxes, and batch save buttons are completely abstracted away.
 * 2. Multi-Layered Attendance Information Architecture:
 *    - Layer 1: Statutory Summary & Telemetry (Overall %, Working Days, Days Present, Days Absent, Medical Leaves, Board Exam 75% Rule Clearance).
 *    - Layer 2: Subject-Wise Attendance Breakdown & Faculty Punch Clearance.
 *    - Layer 3: Day-by-Day Historical Attendance Calendar with Teacher Remarks.
 *    - Layer 4: Formal Leave Application Desk (Student/Parent submissions & In-Charge approval status tracking).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StudentAttendancePortal(
    viewModel: SchoolViewModel,
    student: StudentEntity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedLayerTab by remember { mutableIntStateOf(0) }
    val leaveRequests by viewModel.leaveRequests.collectAsState()

    // Filter leave requests to this student
    val studentLeaves = leaveRequests.filter { it.studentId == (student?.id ?: "ADM-2026-001") }

    var showApplyLeaveDialog by remember { mutableStateOf(false) }
    var leaveStartDate by remember { mutableStateOf("2026-10-02") }
    var leaveEndDate by remember { mutableStateOf("2026-10-04") }
    var leaveReason by remember { mutableStateOf("Severe viral fever and medically advised bed rest") }

    val studentName = student?.name ?: "Aarav Sharma"
    val admissionNo = student?.admissionNumber ?: "ADM-2026-001"
    val studentPct = student?.attendancePercentage?.toDouble() ?: 94.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("student_attendance_portal_root")
    ) {
        // =====================================================================
        // PORTAL BANNER: RBAC READ-ONLY ABSTRACTION BADGE
        // =====================================================================
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
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
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(GovNavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = PureWhite,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SCHOLAR ATTENDANCE PORTAL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyDark,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassBadge(text = "READ-ONLY AUDIT", color = GovGreen)
                            }
                            Text(
                                text = "Scholar: $studentName • Adm: $admissionNo • Class 10-A",
                                fontSize = 11.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    // Security Indicator
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Protected",
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "FACULTY CERTIFIED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Layer Tab Selector
                PrimaryTabRow(
                    selectedTabIndex = selectedLayerTab,
                    containerColor = PureWhite,
                    contentColor = GovNavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedLayerTab == 0,
                        onClick = { selectedLayerTab = 0 },
                        text = { Text("Layer 1: Overview", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_student_layer_overview")
                    )
                    Tab(
                        selected = selectedLayerTab == 1,
                        onClick = { selectedLayerTab = 1 },
                        text = { Text("Layer 2: Subjects", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_student_layer_subjects")
                    )
                    Tab(
                        selected = selectedLayerTab == 2,
                        onClick = { selectedLayerTab = 2 },
                        text = { Text("Layer 3: Daily Log", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_student_layer_log")
                    )
                    Tab(
                        selected = selectedLayerTab == 3,
                        onClick = { selectedLayerTab = 3 },
                        text = { Text("Layer 4: Leave Desk", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("tab_student_layer_leave")
                    )
                }
            }
        }

        // =====================================================================
        // TAB CONTENTS (4 DISTINCT ABSTRACTION LAYERS)
        // =====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            when (selectedLayerTab) {
                0 -> {
                    Layer1StatutoryOverview(
                        studentPct = studentPct,
                        onOpenLeaveDialog = { showApplyLeaveDialog = true }
                    )
                }
                1 -> {
                    Layer2SubjectWiseBreakdown()
                }
                2 -> {
                    Layer3DailyAttendanceLog()
                }
                3 -> {
                    Layer4LeaveApplicationDesk(
                        leaveRequests = studentLeaves,
                        onOpenApplyDialog = { showApplyLeaveDialog = true }
                    )
                }
            }
        }
    }

    // Leave Application Modal Dialog
    if (showApplyLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showApplyLeaveDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = GovNavyPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply for Scholar Leave of Absence", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Leave requests are routed to Class In-Charge & Principal for statutory board approval.",
                        fontSize = 11.sp,
                        color = GovSlateMuted
                    )
                    GlassTextField(
                        value = leaveStartDate,
                        onValueChange = { leaveStartDate = it },
                        label = "Commencement Date (YYYY-MM-DD)",
                        placeholder = "2026-10-02"
                    )
                    GlassTextField(
                        value = leaveEndDate,
                        onValueChange = { leaveEndDate = it },
                        label = "Conclusion Date (YYYY-MM-DD)",
                        placeholder = "2026-10-04"
                    )
                    GlassTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = "Detailed Medical / Statutory Reason",
                        placeholder = "Reason for absence..."
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Submit Leave Request",
                    onClick = {
                        viewModel.applyStudentLeave(
                            startDate = leaveStartDate,
                            endDate = leaveEndDate,
                            reason = leaveReason
                        ) { success: Boolean, msg: String ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showApplyLeaveDialog = false
                            }
                        }
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(
                    text = "Cancel",
                    onClick = { showApplyLeaveDialog = false }
                )
            }
        )
    }
}

// =============================================================================
// LAYER 1: STATUTORY SUMMARY & BOARD EXAM CLEARANCE
// =============================================================================
@Composable
private fun Layer1StatutoryOverview(
    studentPct: Double,
    onOpenLeaveDialog: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Main Percentage Card with Board Exam Rule
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (studentPct >= 75.0) Color(0xFF86EFAC) else Color(0xFFFECACA)),
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
                                text = "CUMULATIVE ATTENDANCE RATE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateMuted,
                                letterSpacing = 0.5.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${studentPct.toInt()}%",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (studentPct >= 85.0) GovGreen else if (studentPct >= 75.0) GovSaffron else GovRedAlert
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (studentPct >= 85.0) "EXEMPLARY RECORD" else "REGULAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (studentPct >= 85.0) GovGreen else GovSaffron,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        OfficialSealStamp(
                            title = "CBSE/BOARD",
                            code = "ELIGIBLE",
                            modifier = Modifier.size(54.dp),
                            color = GovGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (studentPct / 100.0).toFloat().coerceIn(0f, 1f) },
                        color = if (studentPct >= 75.0) GovGreen else GovRedAlert,
                        trackColor = Color(0xFFE2E8F0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Statutory 75% Rule Explanatory Banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Statutory Board Exam Eligibility Status: GRANTED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "Minimum mandatory attendance threshold is 75.0%. Your current average of ${studentPct.toInt()}% clears you for board examination admit card issuance.",
                                    fontSize = 9.5.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Attendance Breakdown Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMetricTile(
                    title = "Working Days",
                    value = "110",
                    color = GovNavyPrimary,
                    modifier = Modifier.weight(1f)
                )
                QuickMetricTile(
                    title = "Days Present",
                    value = "103",
                    color = GovGreen,
                    modifier = Modifier.weight(1f)
                )
                QuickMetricTile(
                    title = "Days Absent",
                    value = "5",
                    color = GovRedAlert,
                    modifier = Modifier.weight(1f)
                )
                QuickMetricTile(
                    title = "Medical Leave",
                    value = "2",
                    color = GovSaffron,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Biometric / Gate 1 Telemetry
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today's Biometric & Campus Access Telemetry",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                        }
                        GlassBadge(text = "RECORDED", color = GovGreen)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Morning Punch-In:", fontSize = 10.sp, color = GovSlateMuted)
                            Text("08:14:22 AM (Turnstile Gate 1)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Evening Dispersal:", fontSize = 10.sp, color = GovSlateMuted)
                            Text("In Progress (Ends 03:30 PM)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovGreen)
                        }
                    }
                }
            }
        }

        // Action: Apply for Medical Leave
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLeaveDialog() }
                    .testTag("btn_student_apply_leave")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalHospital, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Need to report an upcoming or medical absence?", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                            Text("Submit a formal leave slip to the Class In-Charge & Principal", fontSize = 9.5.sp, color = GovSlateMuted)
                        }
                    }
                    Text("Apply >", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                }
            }
        }
    }
}

// =============================================================================
// LAYER 2: SUBJECT-WISE ATTENDANCE BREAKDOWN
// =============================================================================
@Composable
private fun Layer2SubjectWiseBreakdown() {
    val subjects = listOf(
        Triple("Mathematics", "Prof. Rajesh Verma", 96 to (48 to 50)),
        Triple("Science (Physics/Chemistry)", "Dr. Anita Desai", 93 to (56 to 60)),
        Triple("English Literature", "Mrs. Sunita Kapoor", 98 to (49 to 50)),
        Triple("Social Science & Civics", "Mr. David Miller", 92 to (46 to 50)),
        Triple("Computer Applications", "Er. Vikram Sen", 100 to (45 to 45)),
        Triple("Physical Education & Sports", "Coach Amarjeet Singh", 90 to (36 to 40))
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Course-Wise Attendance & Faculty Clearance Register:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GovNavyDark
            )
            Text(
                text = "Faculty marked attendance for Term 1 academic classes (CBSE 75% rule enforced per subject).",
                fontSize = 10.sp,
                color = GovSlateMuted
            )
        }

        items(subjects) { (subject, teacher, stats) ->
            val pct = stats.first
            val (attended, total) = stats.second

            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = subject, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                            Text(text = "Faculty In-Charge: $teacher", fontSize = 10.sp, color = GovSlateMuted)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$pct%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (pct >= 85) GovGreen else if (pct >= 75) GovSaffron else GovRedAlert
                            )
                            Text(
                                text = "$attended / $total classes",
                                fontSize = 9.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (pct / 100f).coerceIn(0f, 1f) },
                        color = if (pct >= 85) GovGreen else if (pct >= 75) GovSaffron else GovRedAlert,
                        trackColor = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

// =============================================================================
// LAYER 3: DAY-BY-DAY ATTENDANCE LOG (READ ONLY)
// =============================================================================
@Composable
private fun Layer3DailyAttendanceLog() {
    val dailyLogs = listOf(
        DailyLogItem("2026-09-28", "Monday", "PRESENT", "Present in all 6 periods. Participated in Robotics Club.", "08:14 AM"),
        DailyLogItem("2026-09-27", "Sunday", "HOLIDAY", "Weekly Institutional Holiday", "-"),
        DailyLogItem("2026-09-26", "Saturday", "PRESENT", "Half-Day Academic Session & Olympiad Mentorship", "08:10 AM"),
        DailyLogItem("2026-09-25", "Friday", "PRESENT", "Present in all 6 periods. Chemistry Lab Completed.", "08:12 AM"),
        DailyLogItem("2026-09-24", "Thursday", "PRESENT", "Present. Physics Practical & English Essay Submission.", "08:15 AM"),
        DailyLogItem("2026-09-23", "Wednesday", "EXCUSED_LEAVE", "Sanctioned Medical Leave (Doctor Certificate #MED-99)", "-"),
        DailyLogItem("2026-09-22", "Tuesday", "EXCUSED_LEAVE", "Sanctioned Medical Leave (Fever)", "-"),
        DailyLogItem("2026-09-21", "Monday", "PRESENT", "Present in all 6 periods. Inter-School Quiz Selection.", "08:08 AM")
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Official Daily Attendance Log (September 2026):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )
                GlassBadge(text = "VERIFIED LOG", color = GovGreen)
            }
        }

        items(dailyLogs) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.date} (${item.dayOfWeek})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            GlassBadge(
                                text = item.status.replace("_", " "),
                                color = when (item.status) {
                                    "PRESENT" -> GovGreen
                                    "HOLIDAY" -> Color(0xFF64748B)
                                    "EXCUSED_LEAVE" -> GovSaffron
                                    else -> GovRedAlert
                                }
                            )
                        }
                        Text(
                            text = item.remarks,
                            fontSize = 10.sp,
                            color = GovSlateMuted
                        )
                    }

                    if (item.punchTime != "-") {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Punch-In", fontSize = 8.5.sp, color = GovSlateMuted)
                            Text(
                                text = item.punchTime,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = GovNavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class DailyLogItem(
    val date: String,
    val dayOfWeek: String,
    val status: String,
    val remarks: String,
    val punchTime: String
)

// =============================================================================
// LAYER 4: FORMAL LEAVE APPLICATION DESK
// =============================================================================
@Composable
private fun Layer4LeaveApplicationDesk(
    leaveRequests: List<StudentLeaveRequestEntity>,
    onOpenApplyDialog: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Formal Scholar Leave Applications:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Submit and track statutory leaves submitted to Class In-Charge.",
                        fontSize = 10.sp,
                        color = GovSlateMuted
                    )
                }

                GlassButton(
                    text = "+ Apply Leave",
                    onClick = onOpenApplyDialog,
                    color = GovNavyPrimary
                )
            }
        }

        if (leaveRequests.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GovSlateMuted, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No active or pending leave applications found.", fontSize = 11.sp, color = GovSlateMuted)
                        Text("Tap '+ Apply Leave' above to submit a medical or emergency absence request.", fontSize = 9.5.sp, color = GovSlateMuted)
                    }
                }
            }
        } else {
            items(leaveRequests) { leave ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Leave ID: ${leave.leaveId}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "${leave.startDate} to ${leave.endDate}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GovNavyPrimary
                                )
                            }

                            GlassBadge(
                                text = leave.status,
                                color = when (leave.status) {
                                    "APPROVED" -> GovGreen
                                    "PENDING" -> GovSaffron
                                    else -> GovRedAlert
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Reason: ${leave.reason}",
                            fontSize = 10.sp,
                            color = GovSlateDark
                        )

                        if (!leave.reviewerRemarks.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Faculty Note: ${leave.reviewerRemarks} (by ${leave.reviewedBy ?: "In-Charge"})",
                                fontSize = 9.5.sp,
                                color = GovGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMetricTile(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
            Text(text = title, fontSize = 8.5.sp, color = GovSlateMuted, maxLines = 1)
        }
    }
}
