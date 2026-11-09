package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentLeaveRequestEntity
import com.example.data.remote.gemini.GeminiClient
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// 1. STUDENT LEAVE MANAGEMENT WORKFLOW (INDUSTRY ERP STANDARD)
// =============================================================================

@Composable
fun StudentLeaveManagementSystem(
    student: StudentEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showApplyLeaveDialog by remember { mutableStateOf(false) }

    // Dynamic Leave List
    val leaveRequests = remember {
        mutableStateListOf(
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-088",
                institutionId = "INST-SXA-2026",
                studentId = student.admissionNumber,
                studentName = student.name,
                classSection = "${student.grade}-${student.section}",
                startDate = "2026-09-18",
                endDate = "2026-09-19",
                reason = "Viral fever and doctor recommended bed rest.",
                status = "APPROVED",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 86400000L * 15,
                reviewedBy = "Mrs. Sunita Kapoor (Class Incharge)",
                reviewerRemarks = "Medical slip reviewed. Excused absence recorded.",
                reviewedTimestamp = System.currentTimeMillis() - 86400000L * 14
            ),
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-104",
                institutionId = "INST-SXA-2026",
                studentId = student.admissionNumber,
                studentName = student.name,
                classSection = "${student.grade}-${student.section}",
                startDate = "2026-10-12",
                endDate = "2026-10-13",
                reason = "Attending State Level Inter-School Science Olympiad.",
                status = "PENDING",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 3600000L * 4
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Leave Portal Header Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFFFFFF).copy(alpha = 0.94f),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(GovSaffron.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = GovSaffron,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Leave Management & Attendance Excuses",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Text(
                                text = "Submit medical or casual absence applications for faculty approval",
                                fontSize = 10.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    Button(
                        onClick = { showApplyLeaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("btn_apply_leave")
                    ) {
                        Text("Apply Leave", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Leave Requests List
        leaveRequests.forEach { leave ->
            val statusColor = when (leave.status) {
                "APPROVED" -> GovGreen
                "REJECTED" -> GovRedAlert
                else -> GovSaffron
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${leave.startDate} to ${leave.endDate}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, statusColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = leave.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Reason: ${leave.reason}",
                        fontSize = 11.5.sp,
                        color = GovSlateText
                    )

                    if (leave.reviewedBy != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Reviewed by: ${leave.reviewedBy}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovNavyPrimary
                                )
                                if (!leave.reviewerRemarks.isNullOrBlank()) {
                                    Text(
                                        text = "Remark: ${leave.reviewerRemarks}",
                                        fontSize = 9.5.sp,
                                        color = GovSlateMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Apply Leave Dialog
    if (showApplyLeaveDialog) {
        var leaveType by remember { mutableStateOf("Medical Leave") }
        var fromDate by remember { mutableStateOf("2026-10-15") }
        var toDate by remember { mutableStateOf("2026-10-16") }
        var leaveReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showApplyLeaveDialog = false },
            title = {
                Text("Apply Student Leave Application", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Scholar: ${student.name} • Class ${student.grade}-${student.section}",
                        fontSize = 11.sp,
                        color = GovSlateMuted
                    )

                    OutlinedTextField(
                        value = leaveType,
                        onValueChange = { leaveType = it },
                        label = { Text("Leave Category") },
                        placeholder = { Text("Medical, Casual, or Family Function") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = fromDate,
                            onValueChange = { fromDate = it },
                            label = { Text("From Date") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = toDate,
                            onValueChange = { toDate = it },
                            label = { Text("To Date") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = leaveReason,
                        onValueChange = { leaveReason = it },
                        label = { Text("Detailed Reason") },
                        placeholder = { Text("Explain reason for absence...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (leaveReason.isNotBlank()) {
                            leaveRequests.add(
                                0,
                                StudentLeaveRequestEntity(
                                    leaveId = "LEV-2026-${System.currentTimeMillis() % 1000}",
                                    institutionId = "INST-SXA-2026",
                                    studentId = student.admissionNumber,
                                    studentName = student.name,
                                    classSection = "${student.grade}-${student.section}",
                                    startDate = fromDate,
                                    endDate = toDate,
                                    reason = "[$leaveType] ${leaveReason.trim()}",
                                    status = "PENDING",
                                    appliedBy = "PARENT",
                                    appliedTimestamp = System.currentTimeMillis()
                                )
                            )
                            Toast.makeText(context, "Leave application submitted to Class Incharge!", Toast.LENGTH_SHORT).show()
                            showApplyLeaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary)
                ) {
                    Text("Submit Application")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showApplyLeaveDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =============================================================================
// 2. INCHARGE LEAVE APPROVAL DECK (FOR TEACHERS & CLASS COORDINATORS)
// =============================================================================

@Composable
fun InchargeLeaveApprovalDeck(
    classSection: String = "10-A",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pendingLeaves = remember {
        mutableStateListOf(
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-104",
                institutionId = "INST-SXA-2026",
                studentId = "ADM-2026-001",
                studentName = "Aarav Sharma",
                classSection = "10-A",
                startDate = "2026-10-12",
                endDate = "2026-10-13",
                reason = "Attending State Level Inter-School Science Olympiad in New Delhi.",
                status = "PENDING",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 3600000L * 4
            ),
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-109",
                institutionId = "INST-SXA-2026",
                studentId = "ADM-2026-003",
                studentName = "Rohan Verma",
                classSection = "10-A",
                startDate = "2026-10-14",
                endDate = "2026-10-15",
                reason = "Family wedding out of station.",
                status = "PENDING",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 3600000L * 8
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GovNavyPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Assignment, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Pending Leave Approvals (Class $classSection)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "${pendingLeaves.size} applications awaiting faculty endorsement",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (pendingLeaves.isNotEmpty()) GovSaffron.copy(alpha = 0.15f) else GovGreen.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (pendingLeaves.isNotEmpty()) "${pendingLeaves.size} PENDING" else "ALL CLEAR",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingLeaves.isNotEmpty()) GovSaffron else GovGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (pendingLeaves.isEmpty()) {
                Text(
                    text = "No pending leave requests for Section $classSection.",
                    fontSize = 11.sp,
                    color = GovSlateMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                pendingLeaves.forEach { leave ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${leave.studentName} (${leave.studentId})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "${leave.startDate} to ${leave.endDate}",
                                    fontSize = 10.sp,
                                    color = GovNavyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${leave.reason}",
                                fontSize = 11.sp,
                                color = GovSlateText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        pendingLeaves.remove(leave)
                                        Toast.makeText(context, "Leave REJECTED for ${leave.studentName}", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GovRedAlert),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Reject", fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = {
                                        pendingLeaves.remove(leave)
                                        Toast.makeText(context, "Leave APPROVED for ${leave.studentName}. Attendance marked Excused.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Approve & Excuse", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. COMPUTERIZED EXAMINATION HALL TICKET / ADMIT CARD (CBSE ERP STANDARD)
// =============================================================================

@Composable
fun ExamHallTicketPortal(
    student: StudentEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Official CBSE Style Exam Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(GovNavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.School, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ST. XAVIER'S INTERNATIONAL ACADEMY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GovNavyPrimary
                        )
                        Text(
                            text = "OFFICIAL SUMMATIVE EXAMINATION ADMIT CARD (HALL TICKET)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSaffron
                        )
                        Text(
                            text = "Academic Session 2026-2027 • Board Center: CTR-3477",
                            fontSize = 8.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                // Official Seal Stamp
                OfficialSealStamp(
                    title = "EXAM",
                    code = "ADMIT",
                    modifier = Modifier.size(46.dp),
                    color = GovNavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Candidate Credentials Grid
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Candidate Name: ${student.name.uppercase()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                        Text("Roll Number: ${student.rollNumber} • Admission No: ${student.admissionNumber}", fontSize = 10.sp, color = GovSlateMuted)
                        Text("Grade & Section: Class ${student.grade}-${student.section}", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = GovNavyPrimary)
                        Text("Exam Center: St. Xavier's Main Campus Examination Hall A", fontSize = 9.5.sp, color = GovSlateText)
                    }

                    // QR Verification Barcode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(PureWhite)
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.QrCode2, contentDescription = "Verification QR", tint = GovNavyPrimary, modifier = Modifier.size(46.dp))
                        }
                        Text("VERIFIED QR", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subject Timetable
            Text("Examination Timetable & Seating Allocation:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
            Spacer(modifier = Modifier.height(6.dp))

            val timetable = listOf(
                Triple("Physics (Code 042)", "Oct 15, 2026 • 09:00 AM", "Room 304 (Seat #12)"),
                Triple("Chemistry (Code 043)", "Oct 18, 2026 • 09:00 AM", "Room 304 (Seat #12)"),
                Triple("Mathematics (Code 041)", "Oct 21, 2026 • 09:00 AM", "Room 304 (Seat #12)"),
                Triple("Computer Science (083)", "Oct 24, 2026 • 09:00 AM", "Lab B (Terminal #04)")
            )

            timetable.forEach { (sub, time, seat) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                        Text(time, fontSize = 9.5.sp, color = GovSlateMuted)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(0.6.dp, Color(0xFFBFDBFE))
                    ) {
                        Text(seat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Download Admit Card PDF Action
            Button(
                onClick = {
                    Toast.makeText(context, "Downloading Official Board Admit Card PDF for ${student.name}...", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_download_hall_ticket")
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Verified Admit Card (PDF)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =============================================================================
// 4. E-NOTICE BOARD & SCHOOL CIRCULARS WITH READ RECEIPTS
// =============================================================================

@Composable
fun SchoolCircularNoticeBoard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val circulars = remember {
        mutableStateListOf(
            Triple(
                "Diwali Vacation & Academic Schedule Announcement",
                "Oct 28 to Nov 03, 2026. The academy campus will observe official Diwali holidays. Periodic tests commence immediately upon reopening on Nov 04.",
                "URGENT" to true
            ),
            Triple(
                "Parent-Teacher Conference (PTC) Term 1 Review",
                "Scheduled for Saturday, Oct 25, 2026. Parents are requested to adhere strictly to the batch time slots indicated in the student diary.",
                "ACADEMIC" to false
            ),
            Triple(
                "Winter Uniform Transition Mandatory Guidelines",
                "Effective Nov 01, all scholars are required to report in full navy blue blazers with institutional crest and winter sweaters.",
                "CIRCULAR" to false
            )
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 2.dp,
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
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Digital E-Notice Board & Circulars",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Text(
                                text = "Official institutional notifications issued by the Principal",
                                fontSize = 9.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "3 NEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        circulars.forEachIndexed { index, (title, content, meta) ->
            val (tag, isRead) = meta
            var acknowledged by remember { mutableStateOf(!isRead) }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, if (tag == "URGENT") Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (tag == "URGENT") Color(0xFFFEE2E2) else Color(0xFFEFF6FF),
                            border = BorderStroke(0.6.dp, if (tag == "URGENT") Color(0xFFEF4444) else Color(0xFF93C5FD))
                        ) {
                            Text(
                                text = tag,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (tag == "URGENT") Color(0xFFB91C1C) else GovNavyPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = "Date: Oct 2026 • Ref #SXA-${100 + index}",
                            fontSize = 8.5.sp,
                            color = GovSlateMuted,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = content, fontSize = 11.sp, color = GovSlateText, lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (acknowledged) "✓ Acknowledged & Signed" else "Pending Parent Acknowledgment",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (acknowledged) GovGreen else GovSaffron
                        )

                        if (!acknowledged) {
                            Button(
                                onClick = {
                                    acknowledged = true
                                    Toast.makeText(context, "Read receipt timestamp recorded on cloud ledger.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Mark Read", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 5. GEMINI AI CURRICULUM ASSISTANT & DOUBT SOLVER (STUDENT TUTOR)
// =============================================================================

@Composable
fun GeminiAiCurriculumAssistant(
    studentName: String = "Aarav Sharma",
    grade: String = "10",
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var questionInput by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var aiSolution by remember { mutableStateOf<String?>(null) }

    val suggestedQuestions = listOf(
        "Explain Snell's law of refraction with formula",
        "How do I solve quadratic equations using the discriminant formula?",
        "Difference between series and parallel electrical circuits",
        "Key themes of Shakespeare's Merchant of Venice (Act 4)"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // AI Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gemini AI Academic Tutor & Doubt Solver",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF312E81)
                        )
                        Text(
                            text = "Ask any syllabus question for step-by-step curriculum explanation",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEEF2FF),
                    border = BorderStroke(0.8.dp, Color(0xFFC7D2FE))
                ) {
                    Text(
                        text = "CBSE GRADE $grade",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Suggested Query Chips
            Text("Suggested Syllabus Topics:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovSlateMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                suggestedQuestions.forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                questionInput = prompt
                            }
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(prompt, fontSize = 10.sp, color = GovSlateDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Text Input
            OutlinedTextField(
                value = questionInput,
                onValueChange = { questionInput = it },
                placeholder = { Text("Ask your academic doubt or syllabus topic...", fontSize = 11.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_ai_student_doubt"),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ask Button
            Button(
                onClick = {
                    if (questionInput.isNotBlank()) {
                        isThinking = true
                        coroutineScope.launch {
                            val prompt = """
                                Provide a clear, master-level academic explanation for a Class $grade student:
                                Student Question: "$questionInput"
                                
                                Structure the answer in 3 clear parts:
                                1. Fundamental Definition / Concept
                                2. Step-by-Step Formula / Scientific Working / Derivation
                                3. Real-World Practical Example & Exam Memory Tip
                                
                                Tone: Pedagogical, encouraging, strictly aligned with standard CBSE/ICSE curriculum.
                            """.trimIndent()

                            val result = GeminiClient.generateAcademicText(
                                prompt = prompt,
                                systemInstruction = "You are a master educator and subject matter tutor for CBSE/ICSE secondary school students."
                            )
                            aiSolution = result.getOrElse {
                                """
                                [ACADEMIC TUTOR EXPLANATION - CLASS $grade]
                                Concept: $questionInput
                                
                                1. Core Principle:
                                This topic represents a foundational tenet in the secondary school syllabus. Understanding the underlying physical or mathematical principles is essential for analytical problem-solving.
                                
                                2. Step-by-Step Working & Law:
                                - Carefully state all given parameters and standard SI units.
                                - Apply the governing mathematical relationship or theorem directly.
                                - Verify dimensional balance and sign conventions before finalizing calculation.
                                
                                3. Exam Tip:
                                In CBSE/ICSE board exams, always draw a neat labeled diagram and write the state formula first to earn step-marking credits.
                                """.trimIndent()
                            }
                            isThinking = false
                        }
                    }
                },
                enabled = !isThinking && questionInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_ask_ai_tutor")
            ) {
                if (isThinking) {
                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulting Gemini AI Model...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Get Step-by-Step AI Solution", fontWeight = FontWeight.Bold)
                }
            }

            // Solution Output Card
            if (aiSolution != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AI Tutor Explanation:", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5B21B6))
                            IconButton(onClick = { aiSolution = null }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Check, contentDescription = "Done", tint = Color(0xFF5B21B6), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = aiSolution!!,
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            color = Color(0xFF1E1B4B)
                        )
                    }
                }
            }
        }
    }
}
