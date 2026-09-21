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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
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
import com.example.data.model.UserRole
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.OfficialSealStamp
import com.example.ui.components.OfficialStudentIdCard
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
fun StudentDashboard(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentRole by viewModel.currentRole.collectAsState()
    val isParentMode = currentRole == UserRole.PARENT

    val students by viewModel.students.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val feeRecords by viewModel.feeRecords.collectAsState()
    val certificates by viewModel.certificates.collectAsState()
    val diaryEntries by viewModel.diaryEntries.collectAsState()
    val notices by viewModel.notices.collectAsState()

    // Isolated student (Aarav Sharma - Roll #1)
    val student = students.firstOrNull { it.admissionNumber == "ADM-2026-001" } ?: students.firstOrNull()

    // Filter data strictly to this student
    val studentExams = examRecords.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val studentFees = feeRecords.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val studentCerts = certificates.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val classDiary = diaryEntries.filter { it.classSection.contains("10-A") }

    // Dialog for Requesting Certificate
    var showCertDialog by remember { mutableStateOf(false) }
    var certType by remember { mutableStateOf("BONAFIDE") }
    var certPurpose by remember { mutableStateOf("Passport Verification & Higher Studies") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("student_dashboard")
    ) {
        if (student == null) {
            Text("No student record found.", color = GovRedAlert)
            return@Column
        }

        // Student / Parent Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isParentMode) "Parent & Guardian Desk" else "Student Scholar Portal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(
                        text = if (isParentMode) "PARENT OF SCHOLAR" else "GRADE 10-A",
                        color = if (isParentMode) GovSaffron else GovNavyPrimary
                    )
                }
                Text(
                    text = if (isParentMode) "Child: ${student.name} | Guardian: ${student.guardianName}" else "${student.name} | Admission No: ${student.admissionNumber}",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted
                )
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(GovNavyPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = PureWhite, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Student Quick Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = GovGreen,
                headerTitle = "ATTENDANCE"
            ) {
                Column {
                    Text(
                        text = "${student.attendancePercentage}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(
                        text = "Exemplary Record",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                }
            }

            GlassCard(
                modifier = Modifier.weight(1f),
                headerRibbonColor = if (student.feeDue > 0) GovRedAlert else GovGreen,
                headerTitle = "COMPOSITE FEE"
            ) {
                Column {
                    Text(
                        text = if (student.feeDue > 0) " ${student.feeDue.toInt()}" else "CLEARED",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (student.feeDue > 0) GovRedAlert else GovGreen
                    )
                    Text(
                        text = if (student.feeDue > 0) "Term Due" else "Official Receipt #9901",
                        fontSize = 9.sp,
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
                    text = { Text("Digital ID", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Report Card", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Diary & HW", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Certificates", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: COMPUTERIZED DIGITAL ID CARD
        // =====================================================================
        if (selectedTab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Official Computer-Generated Student Identity Card:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                // Render the official non-glass printable PVC card
                OfficialStudentIdCard(
                    student = student,
                    schoolName = "ST. XAVIER'S INTERNATIONAL ACADEMY",
                    academicYear = "2026-2027"
                )
            }
        }

        // =====================================================================
        // TAB 1: OFFICIAL TERM REPORT CARD
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
                            text = "Term 1 Summative Assessment Report Card:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "Session: 2026-2027 | Class 10-A",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }

                    OfficialSealStamp(
                        title = "ACADEMY",
                        code = "SEAL-A+",
                        modifier = Modifier.size(52.dp),
                        color = GovGreen
                    )
                }

                studentExams.forEach { exam ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = exam.subject.uppercase(),
                        headerBadge = "GRADE ${exam.grade}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.subject,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Faculty Remarks: ${exam.teacherRemarks}",
                                    fontSize = 10.sp,
                                    color = GovSlateText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${exam.marksObtained.toInt()} / ${exam.maxMarks.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovGreen
                                )
                                Text(
                                    text = "${(exam.marksObtained * 100 / exam.maxMarks).toInt()}%",
                                    fontSize = 9.sp,
                                    color = GovSlateMuted
                                )
                            }
                        }
                    }
                }

                // Summary Cumulative Banner
                val totalMarks = studentExams.sumOf { it.marksObtained.toDouble() }
                val maxMarks = studentExams.sumOf { it.maxMarks.toDouble() }.coerceAtLeast(1.0)
                val cumulativePct = (totalMarks * 100 / maxMarks).toInt()

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovGreen,
                    headerTitle = "OVERALL SCHOLASTIC PERFORMANCE"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cumulative Grade: A+ (Outstanding)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Text(
                                text = "Total Score: ${totalMarks.toInt()} / ${maxMarks.toInt()} ($cumulativePct%)",
                                fontSize = 10.sp,
                                color = GovSlateMuted
                            )
                        }

                        GlassButton(
                            text = "Download PDF",
                            onClick = {
                                Toast.makeText(context, "Generating official sealed Report Card PDF...", Toast.LENGTH_SHORT).show()
                            },
                            color = GovNavyPrimary,
                            leadingIcon = {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                            }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: SCHOOL DIARY & HOMEWORK
        // =====================================================================
        if (selectedTab == 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Daily School Diary & Homework (Class 10-A):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                classDiary.forEach { entry ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = "${entry.subject.uppercase()} - ${entry.date}",
                        headerBadge = "TEACHER POSTED"
                    ) {
                        Column {
                            Text(
                                text = "Topic: ${entry.topicCovered}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Homework Assigned: ${entry.homeworkAssigned}",
                                fontSize = 11.sp,
                                color = GovNavyPrimary
                            )
                            if (entry.specialInstructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Special Note: ${entry.specialInstructions}",
                                    fontSize = 10.sp,
                                    color = GovSaffron,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Teacher: ${entry.teacherName} (In-Charge Verified)",
                                fontSize = 9.sp,
                                color = GovSlateMuted
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 3: CERTIFICATES & REQUEST DESK
        // =====================================================================
        if (selectedTab == 3) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Certificates & Digital Verifications:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )

                    GlassButton(
                        text = "Apply Certificate",
                        onClick = { showCertDialog = true },
                        color = GovNavyPrimary
                    )
                }

                studentCerts.forEach { cert ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") GovGreen else GovSaffron,
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
                                        text = "Certificate ID: ${cert.certificateId}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Purpose: ${cert.purposeOrAchievement}",
                                        fontSize = 10.sp,
                                        color = GovSlateText
                                    )
                                    Text(
                                        text = "Status: ${cert.applicationStatus.replace("_", " ")}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") GovGreen else GovSaffron
                                    )
                                }

                                if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") {
                                    OfficialSealStamp(
                                        title = "EXECUTIVE",
                                        code = "APPROVED",
                                        modifier = Modifier.size(52.dp),
                                        color = GovGreen
                                    )
                                }
                            }

                            if (cert.isDownloadUnlocked) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassButton(
                                        text = "Download Sealed Certificate",
                                        onClick = {
                                            Toast.makeText(context, "Downloading authenticated Certificate PDF: ${cert.certificateId}", Toast.LENGTH_SHORT).show()
                                        },
                                        color = GovGreen,
                                        leadingIcon = {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (cert.secureWhatsAppToken != null) {
                                        OfficialWhatsAppButton(
                                            phone = student.guardianPhone,
                                            message = "St. Xavier's International Academy: Official ${cert.certificateType} Certificate generated for ${student.name}. Token: ${cert.secureWhatsAppToken}",
                                            label = "WhatsApp Copy",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Apply for Certificate Dialog
    if (showCertDialog && student != null) {
        val curStudent = student
        AlertDialog(
            onDismissRequest = { showCertDialog = false },
            title = {
                Text("Apply for Official School Certificate", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${curStudent.name} | Grade 10-A", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(
                        value = certType,
                        onValueChange = { certType = it },
                        placeholder = "BONAFIDE, TRANSFER, or MERIT",
                        label = "Certificate Type"
                    )
                    GlassTextField(
                        value = certPurpose,
                        onValueChange = { certPurpose = it },
                        placeholder = "e.g. Passport, Olympiad, Board verification",
                        label = "Official Purpose"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Submit Application",
                    onClick = {
                        viewModel.applyForCertificate(
                            type = certType,
                            studentId = curStudent.id,
                            studentName = curStudent.name,
                            grade = "Grade ${curStudent.grade}-${curStudent.section}",
                            purpose = certPurpose
                        )
                        Toast.makeText(context, "Application submitted for In-Charge and Principal review!", Toast.LENGTH_SHORT).show()
                        showCertDialog = false
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showCertDialog = false })
            }
        )
    }
}
