package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entities.IdCardRecordEntity
import com.example.data.local.entities.SchoolDiaryEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.AiAcademicToolsSection
import com.example.ui.components.DailyAttendanceRecorder
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
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
import com.example.ui.viewmodel.TeacherAttendanceViewModel
import com.example.ui.viewmodel.TeacherAttendanceViewModelFactory

@Composable
fun TeacherDashboard(
    viewModel: SchoolViewModel,
    onNavigateToScanScreen: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val attendanceViewModel: TeacherAttendanceViewModel = viewModel(
        factory = TeacherAttendanceViewModelFactory(
            attendanceRepository = viewModel.attendanceRepository,
            schoolRepository = viewModel.repository
        )
    )

    val students by viewModel.students.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val diaryEntries by viewModel.diaryEntries.collectAsState()
    val timetableSlots by viewModel.timetableSlots.collectAsState()

    // 10-A scholars
    val classStudents = students.filter { it.section == "A" }

    // Dialog for Marks Entry
    var showMarksDialog by remember { mutableStateOf(false) }
    var marksStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var marksSubject by remember { mutableStateOf("Physics (Science)") }
    var marksObtained by remember { mutableStateOf("92") }
    var marksMax by remember { mutableStateOf("100") }
    var marksGrade by remember { mutableStateOf("A+") }
    var marksRemarks by remember { mutableStateOf("Good experimental intuition & theoretical grasp.") }

    // Dialog for Diary / Homework Entry
    var showDiaryDialog by remember { mutableStateOf(false) }
    var diarySubject by remember { mutableStateOf("Physics (Science)") }
    var diaryTopic by remember { mutableStateOf("Refraction & Snell's Law") }
    var diaryHomework by remember { mutableStateOf("Solve Problem Set 4.2 in laboratory record.") }
    var diarySpecial by remember { mutableStateOf("Wear lab apron for afternoon practical session.") }

    // Dialog for Student Real Photo ID Capture
    var photoStudent by remember { mutableStateOf<StudentEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("teacher_dashboard")
    ) {
        // Teacher Faculty Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Subject Faculty Portal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(text = "PHYSICS FACULTY", color = GovGreen)
                }
                Text(
                    text = "Dr. Meenakshi Sundaram | Senior Faculty in Physics & General Science",
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

        // Fast Action Ribbon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                text = "Daily Roll Call",
                onClick = { selectedTab = 0 },
                color = GovNavyPrimary,
                leadingIcon = {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                },
                modifier = Modifier.weight(1f)
            )

            GlassButton(
                text = "Camera Scan",
                onClick = onNavigateToScanScreen,
                color = Color(0xFF0284C7),
                leadingIcon = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                },
                modifier = Modifier.weight(1f)
            )

            GlassButton(
                text = "Post Homework",
                onClick = { showDiaryDialog = true },
                color = GovGreen,
                leadingIcon = {
                    Icon(Icons.Default.EventNote, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
                },
                modifier = Modifier.weight(1f)
            )
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
                    text = { Text("Daily Attendance", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Exam Marks", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ID Photo Capture", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Timetable", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("AI Academic Tools", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: DAILY ATTENDANCE (ROOM DATABASE PERSISTENCE)
        // =====================================================================
        if (selectedTab == 0) {
            DailyAttendanceRecorder(
                viewModel = attendanceViewModel,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // =====================================================================
        // TAB 1: EXAM & MARKS ENTRY
        // =====================================================================
        if (selectedTab == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Term 1 Assessment Marks Entry (Class 10-A):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    GlassBadge(text = "PHYSICS / SCIENCE", color = GovGreen)
                }

                classStudents.forEach { student ->
                    val existingExam = examRecords.firstOrNull { it.studentId == student.id && it.subject.contains("Physics", ignoreCase = true) }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (existingExam != null) GovNavyPrimary else GovSaffron,
                        headerTitle = "ROLL #${student.rollNumber} - ${student.name.uppercase()}",
                        headerBadge = if (existingExam != null) "GRADE ${existingExam.grade}" else "MARKS PENDING"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Adm No: ${student.admissionNumber}",
                                    fontSize = 9.5.sp,
                                    color = GovSlateMuted
                                )
                                if (existingExam != null) {
                                    Text(
                                        text = "Marks: ${existingExam.marksObtained.toInt()} / ${existingExam.maxMarks.toInt()} (${existingExam.grade})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovGreen
                                    )
                                    Text(
                                        text = "Remarks: ${existingExam.teacherRemarks}",
                                        fontSize = 9.5.sp,
                                        color = GovSlateText
                                    )
                                } else {
                                    Text(
                                        text = "Awaiting marks submission for Term 1",
                                        fontSize = 10.sp,
                                        color = GovSaffron,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            GlassButton(
                                text = if (existingExam != null) "Update Marks" else "Enter Marks",
                                onClick = {
                                    marksStudent = student
                                    marksSubject = "Physics"
                                    marksObtained = existingExam?.marksObtained?.toInt()?.toString() ?: "90"
                                    marksGrade = existingExam?.grade ?: "A+"
                                    marksRemarks = existingExam?.teacherRemarks ?: "Demonstrates strong understanding."
                                    showMarksDialog = true
                                },
                                color = if (existingExam != null) GovNavyPrimary else GovSaffron
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: ID CARD REAL PHOTO CAPTURE & CONVERSION
        // =====================================================================
        if (selectedTab == 2) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Student ID Photo Capture (Camera to Computerized Conversion):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                classStudents.forEach { student ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (student.realIdPhotoUri != null) GovGreen else GovSaffron,
                        headerTitle = "SCHOLAR: ${student.name.uppercase()}",
                        headerBadge = student.digitalIdStatus
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .border(1.dp, GovBorderGrey, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (student.realIdPhotoUri != null) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovGreen, modifier = Modifier.size(28.dp))
                                } else {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = GovSlateMuted, modifier = Modifier.size(24.dp))
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Digital ID: ${student.convertedDigitalId}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GovNavyPrimary
                                )
                                Text(
                                    text = if (student.realIdPhotoUri != null) "Real portrait photo attached" else "No portrait photo captured yet",
                                    fontSize = 9.5.sp,
                                    color = if (student.realIdPhotoUri != null) GovGreen else GovRedAlert
                                )
                            }

                            GlassButton(
                                text = if (student.realIdPhotoUri != null) "Retake" else "Capture",
                                onClick = {
                                    photoStudent = student
                                },
                                color = GovNavyPrimary,
                                leadingIcon = {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                }
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 3: TIMETABLE
        // =====================================================================
        if (selectedTab == 3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Weekly Teaching Timetable (Science Division):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                timetableSlots.forEach { slot ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = "PERIOD ${slot.periodNumber} - ${slot.dayOfWeek.uppercase()}",
                        headerBadge = slot.roomNumber
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = slot.subject,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Class: ${slot.classSection} | Teacher: ${slot.teacherName}",
                                    fontSize = 10.sp,
                                    color = GovSlateMuted
                                )
                            }
                            Text(
                                text = slot.timeRange,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 4: AI ACADEMIC TOOLS (GEMINI CO-PILOT)
        // =====================================================================
        if (selectedTab == 4) {
            AiAcademicToolsSection(
                viewModel = viewModel,
                userRole = "TEACHER",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Marks Entry Dialog
    if (showMarksDialog && marksStudent != null) {
        val stu = marksStudent!!
        AlertDialog(
            onDismissRequest = { showMarksDialog = false },
            title = {
                Text("Enter Term Marks: ${stu.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Class: Grade 10-A | Roll #${stu.rollNumber}", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(value = marksSubject, onValueChange = { marksSubject = it }, placeholder = "Subject", label = "Subject")
                    GlassTextField(
                        value = marksObtained,
                        onValueChange = {
                            marksObtained = it
                            val num = it.toFloatOrNull() ?: 0f
                            marksGrade = when {
                                num >= 90 -> "A+"
                                num >= 80 -> "A"
                                num >= 70 -> "B+"
                                num >= 60 -> "B"
                                else -> "C"
                            }
                        },
                        placeholder = "e.g. 92",
                        label = "Marks Obtained"
                    )
                    GlassTextField(value = marksGrade, onValueChange = { marksGrade = it }, placeholder = "Grade", label = "Letter Grade (A+, A, B...)")
                    GlassTextField(value = marksRemarks, onValueChange = { marksRemarks = it }, placeholder = "Teacher remarks", label = "Teacher Remarks")
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Submit to Central Records",
                    onClick = {
                        val marksVal = marksObtained.toFloatOrNull() ?: 90f
                        viewModel.recordExamMarks(
                            examId = "EXAM-10A-${stu.id}-${marksSubject.take(3).uppercase()}",
                            examType = "SUMMATIVE_ASSESSMENT",
                            examName = "Term 1 Summative Assessment 2026",
                            classSection = "10-A",
                            subject = marksSubject,
                            studentId = stu.id,
                            studentName = stu.name,
                            rollNo = stu.rollNumber,
                            marksObtained = marksVal,
                            grade = marksGrade,
                            remarks = marksRemarks
                        )
                        Toast.makeText(context, "Marks published to Student Report Card!", Toast.LENGTH_SHORT).show()
                        showMarksDialog = false
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showMarksDialog = false })
            }
        )
    }

    // Homework / Diary Dialog
    if (showDiaryDialog) {
        AlertDialog(
            onDismissRequest = { showDiaryDialog = false },
            title = {
                Text("Publish Daily Homework to School Diary", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassTextField(value = diarySubject, onValueChange = { diarySubject = it }, placeholder = "Subject", label = "Subject")
                    GlassTextField(value = diaryTopic, onValueChange = { diaryTopic = it }, placeholder = "Topic covered in class", label = "Class Topic")
                    GlassTextField(value = diaryHomework, onValueChange = { diaryHomework = it }, placeholder = "Homework instructions", label = "Homework Assigned")
                    GlassTextField(value = diarySpecial, onValueChange = { diarySpecial = it }, placeholder = "Special requirements", label = "Special Instructions")
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Broadcast to Class Diary",
                    onClick = {
                        val entry = SchoolDiaryEntity(
                            id = "DIARY-${System.currentTimeMillis()}",
                            date = "2026-09-15",
                            classSection = "Grade 10-A",
                            subject = diarySubject,
                            topicCovered = diaryTopic,
                            homeworkAssigned = diaryHomework,
                            specialInstructions = diarySpecial,
                            teacherName = "Dr. Meenakshi Sundaram",
                            inchargeVerified = true
                        )
                        viewModel.addSchoolDiaryEntry(entry)
                        Toast.makeText(context, "Homework published to all scholars and parents!", Toast.LENGTH_SHORT).show()
                        showDiaryDialog = false
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showDiaryDialog = false })
            }
        )
    }

    // Capture Student Photo Simulation Dialog
    if (photoStudent != null) {
        val stu = photoStudent!!
        AlertDialog(
            onDismissRequest = { photoStudent = null },
            title = {
                Text("Capture Student Portrait Photo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Attaching portrait photo to: ${stu.name} (${stu.admissionNumber})", fontSize = 11.sp, color = GovSlateMuted)
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0))
                            .border(2.dp, GovNavyPrimary, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(60.dp))
                    }
                    Text("Optical Sensor Ready: Capturing high-resolution bio-portrait", fontSize = 9.5.sp, color = GovGreen, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Save Photo & Generate Digital ID",
                    onClick = {
                        val card = IdCardRecordEntity(
                            cardId = "IDC-${stu.id}",
                            personId = stu.id,
                            personName = stu.name,
                            roleCategory = "STUDENT",
                            designationOrGrade = "Grade 10-A (Roll #${stu.rollNumber})",
                            department = "Secondary Academic Division",
                            phone = stu.guardianPhone,
                            bloodGroup = stu.bloodGroup,
                            convertedDigitalCardId = stu.convertedDigitalId,
                            barcodeNumber = "SXA-STU-BARCODE-${stu.rollNumber}024",
                            issueDate = "15 Sep 2026",
                            verificationStatus = "VERIFIED_BY_INCHARGE",
                            inchargeRemarks = "Faculty verified student likeness."
                        )
                        viewModel.saveConvertedIdCard(card)
                        Toast.makeText(context, "Portrait captured and digital PVC card generated!", Toast.LENGTH_SHORT).show()
                        photoStudent = null
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { photoStudent = null })
            }
        )
    }
}
