package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.model.UserRole
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamResultsScreen(
    viewModel: SchoolViewModel,
    currentRole: UserRole,
    onNavigateBack: () -> Unit,
    onNavigateToAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val students by viewModel.students.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubjectFilter by remember { mutableStateOf("All") }

    // Dialog for entering / updating marks
    var showMarksDialog by remember { mutableStateOf(false) }
    var activeStudentForMarks by remember { mutableStateOf<StudentEntity?>(null) }
    var activeExamRecord by remember { mutableStateOf<ExamRecordEntity?>(null) }

    var inputSubject by remember { mutableStateOf("Physics (Science)") }
    var inputMarksObtained by remember { mutableStateOf("90") }
    var inputMaxMarks by remember { mutableStateOf("100") }
    var inputGrade by remember { mutableStateOf("A+") }
    var inputRemarks by remember { mutableStateOf("Demonstrates solid theoretical understanding.") }

    val subjects = listOf("All", "Mathematics", "Physics", "English", "Chemistry", "Biology")

    val classStudents = students.filter { it.section == "A" }

    // Filtered records
    val filteredStudents = classStudents.filter { student ->
        searchQuery.isBlank() ||
                student.name.contains(searchQuery, ignoreCase = true) ||
                student.admissionNumber.contains(searchQuery, ignoreCase = true) ||
                student.rollNumber.toString().contains(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("exam_results_screen_root")
    ) {
        // =====================================================================
        // 1. TOP HEADER & BREADCRUMB
        // =====================================================================
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, GovBorderGrey.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_exam_results_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = GovNavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "EXAMINATION & RESULTS",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassBadge(
                                    text = "TERM 1 & FINAL",
                                    color = GovSaffron
                                )
                            }
                            Text(
                                text = "Portal > Academic Performance & Grade Cards • Grade 10-A",
                                fontSize = 10.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    // Shortcut to Attendance
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GovNavyPrimary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, GovNavyPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable(onClick = onNavigateToAttendance)
                            .testTag("btn_jump_to_attendance_from_exam_results")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventAvailable,
                                contentDescription = null,
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Attendance",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KPI Ribbon
                val averageScore = if (examRecords.isNotEmpty()) {
                    examRecords.map { it.percentage }.average().toFloat()
                } else 88.5f

                val distinctionCount = examRecords.count { it.grade == "A+" || it.grade == "A" }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExamKpiCard(
                        title = "Class Average",
                        value = "${String.format(Locale.US, "%.1f", averageScore)}%",
                        color = GovGreen,
                        modifier = Modifier.weight(1f)
                    )
                    ExamKpiCard(
                        title = "Evaluated",
                        value = "${examRecords.size} Records",
                        color = GovNavyPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    ExamKpiCard(
                        title = "Distinction",
                        value = "$distinctionCount Scholars",
                        color = Color(0xFFB45309),
                        modifier = Modifier.weight(1f)
                    )
                    ExamKpiCard(
                        title = "Pass Rate",
                        value = "100%",
                        color = GovGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // =====================================================================
        // 2. TAB SELECTOR
        // =====================================================================
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = PureWhite,
            contentColor = GovNavyPrimary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Scholar Grade Cards", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_exam_grade_cards")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Subject Analytics", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_exam_analytics")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Report Card Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_exam_report_card")
            )
        }

        // =====================================================================
        // 3. TAB CONTENT
        // =====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // List of Student Score Cards
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Filter Bar & Search
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search scholar name, roll or adm...",
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = GovSlateMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Subject Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subjects.forEach { subj ->
                                val isSelected = selectedSubjectFilter == subj
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedSubjectFilter = subj },
                                    label = { Text(subj, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GovNavyPrimary,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }

                        // Student Exam Cards
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredStudents, key = { it.id }) { student ->
                                val studentExams = examRecords.filter { it.studentId == student.id }
                                val matchingExam = if (selectedSubjectFilter == "All") {
                                    studentExams.firstOrNull()
                                } else {
                                    studentExams.firstOrNull { it.subject.contains(selectedSubjectFilter, ignoreCase = true) }
                                }

                                ScholarExamCard(
                                    student = student,
                                    exam = matchingExam,
                                    onEditMarks = {
                                        activeStudentForMarks = student
                                        activeExamRecord = matchingExam
                                        inputSubject = matchingExam?.subject ?: "Physics"
                                        inputMarksObtained = matchingExam?.marksObtained?.toInt()?.toString() ?: "92"
                                        inputMaxMarks = matchingExam?.maxMarks?.toInt()?.toString() ?: "100"
                                        inputGrade = matchingExam?.grade ?: "A+"
                                        inputRemarks = matchingExam?.teacherRemarks ?: "Excellent work."
                                        showMarksDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Subject Breakdown & Class Analytics
                    SubjectAnalyticsView(
                        examRecords = examRecords,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // Official Sealed Report Card Preview for Top Scholar
                    val topStudent = classStudents.firstOrNull()
                    val topExams = examRecords.filter { it.studentId == (topStudent?.id ?: "") }
                    OfficialReportCardView(
                        student = topStudent,
                        exams = topExams,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Enter / Update Marks Modal Dialog
    if (showMarksDialog && activeStudentForMarks != null) {
        val scholar = activeStudentForMarks!!
        AlertDialog(
            onDismissRequest = { showMarksDialog = false },
            title = {
                Text(
                    text = "Record Academic Marks: ${scholar.name}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = GovNavyPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Scholar: ${scholar.name} (Roll #${scholar.rollNumber} • ${scholar.admissionNumber})",
                        fontSize = 11.5.sp,
                        color = GovSlateMuted
                    )

                    GlassTextField(
                        value = inputSubject,
                        onValueChange = { inputSubject = it },
                        label = "Subject Name",
                        placeholder = "e.g. Physics, Mathematics"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GlassTextField(
                            value = inputMarksObtained,
                            onValueChange = { inputMarksObtained = it },
                            label = "Marks Scored",
                            placeholder = "e.g. 92",
                            modifier = Modifier.weight(1f)
                        )
                        GlassTextField(
                            value = inputMaxMarks,
                            onValueChange = { inputMaxMarks = it },
                            label = "Max Marks",
                            placeholder = "100",
                            modifier = Modifier.weight(1f)
                        )
                        GlassTextField(
                            value = inputGrade,
                            onValueChange = { inputGrade = it },
                            label = "Grade",
                            placeholder = "A+",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    GlassTextField(
                        value = inputRemarks,
                        onValueChange = { inputRemarks = it },
                        label = "Teacher Evaluation Remarks",
                        placeholder = "e.g. High analytical aptitude and lab rigor."
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val scored = inputMarksObtained.toFloatOrNull() ?: 90f
                        val max = inputMaxMarks.toFloatOrNull() ?: 100f
                        viewModel.recordExamMarks(
                            examId = activeExamRecord?.examId ?: "EXAM-${scholar.id}-${inputSubject.take(4).uppercase()}",
                            examType = "TERM_1_ASSESSMENT",
                            examName = "Term 1 Assessment",
                            classSection = "10-A",
                            subject = inputSubject,
                            studentId = scholar.id,
                            studentName = scholar.name,
                            rollNo = scholar.rollNumber,
                            marksObtained = scored,
                            maxMarks = max,
                            grade = inputGrade,
                            remarks = inputRemarks
                        )
                        Toast.makeText(context, "Exam Marks Recorded for ${scholar.name}", Toast.LENGTH_SHORT).show()
                        showMarksDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary)
                ) {
                    Text("Save & Commit to Room DB", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarksDialog = false }) {
                    Text("Cancel", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================

@Composable
private fun ExamKpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                color = GovSlateMuted,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
private fun ScholarExamCard(
    student: StudentEntity,
    exam: ExamRecordEntity?,
    onEditMarks: () -> Unit
) {
    val marksObtained = exam?.marksObtained ?: 0f
    val maxMarks = exam?.maxMarks ?: 100f
    val percentage = if (maxMarks > 0) (marksObtained / maxMarks) * 100f else 0f
    val grade = exam?.grade ?: "PENDING"
    val subject = exam?.subject ?: "Physics"

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, GovBorderGrey),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exam_card_${student.rollNumber}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = GovNavyPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = student.rollNumber.toString(),
                                color = PureWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateDark
                        )
                        Text(
                            text = "Adm: ${student.admissionNumber} • Subject: $subject",
                            fontSize = 10.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (grade) {
                        "A+", "A" -> GovGreen
                        "B+", "B" -> GovNavyPrimary
                        "C" -> GovSaffron
                        else -> Color(0xFFD97706)
                    }
                ) {
                    Text(
                        text = grade,
                        color = PureWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Score Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Score: ${marksObtained.toInt()} / ${maxMarks.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyPrimary
                )
                Text(
                    text = "${percentage.toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = if (percentage >= 75f) GovGreen else GovSaffron
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { (percentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = if (percentage >= 75f) GovGreen else GovSaffron,
                trackColor = Color(0xFFE2E8F0)
            )

            if (exam?.teacherRemarks?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "\"${exam.teacherRemarks}\"",
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = GovSlateDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GlassButton(
                    text = if (exam != null) "Update Marks" else "Enter Marks",
                    onClick = onEditMarks,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    color = GovNavyPrimary,
                    modifier = Modifier.height(34.dp)
                )
            }
        }
    }
}

@Composable
private fun SubjectAnalyticsView(
    examRecords: List<ExamRecordEntity>,
    modifier: Modifier = Modifier
) {
    val subjects = listOf("Physics", "Mathematics", "English", "Chemistry", "Biology")

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerTitle = "Subject Performance Matrix",
                headerBadge = "ACADEMIC AUDIT",
                headerRibbonColor = GovNavyPrimary
            ) {
                Text(
                    text = "Standardized summative assessment analytics calculated across registered scholars in Grade 10-A.",
                    fontSize = 11.5.sp,
                    color = GovSlateDark
                )
            }
        }

        items(subjects) { subjectName ->
            val matchingRecords = examRecords.filter { it.subject.contains(subjectName, ignoreCase = true) }
            val avg = if (matchingRecords.isNotEmpty()) {
                matchingRecords.map { it.percentage }.average().toFloat()
            } else 85f

            val highest = if (matchingRecords.isNotEmpty()) {
                matchingRecords.maxOf { it.marksObtained }
            } else 98f

            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, GovBorderGrey),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subjectName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyPrimary
                        )
                        Text(
                            text = "Avg: ${avg.toInt()}% • Highest: ${highest.toInt()}/100",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GovSlateMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (avg / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (avg >= 80f) GovGreen else GovSaffron,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialReportCardView(
    student: StudentEntity?,
    exams: List<ExamRecordEntity>,
    modifier: Modifier = Modifier
) {
    val scholar = student ?: return

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = PureWhite,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, GovNavyPrimary),
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Crest Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ST. XAVIER'S INTERNATIONAL ACADEMY",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Black,
                            color = GovNavyPrimary
                        )
                        Text(
                            text = "OFFICIAL REPORT CARD & GRADE TRANSCRIPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Affiliation: CBSE Board No. 1130492 • Academic Year 2026-2027",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verified Seal",
                        tint = GovGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Student Identity Strip
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Scholar: ${scholar.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                        Text("Roll: #${scholar.rollNumber} | Class: 10-A", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GovNavyPrimary)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subject", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.weight(2f))
                    Text("Max", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.weight(1f))
                    Text("Scored", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.weight(1f))
                    Text("Grade", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = PureWhite, modifier = Modifier.weight(1f))
                }

                val subjectsList = if (exams.isNotEmpty()) exams else listOf(
                    ExamRecordEntity(examId = "E1", subject = "Physics", studentId = scholar.id, marksObtained = 95f, maxMarks = 100f, grade = "A+"),
                    ExamRecordEntity(examId = "E2", subject = "Mathematics", studentId = scholar.id, marksObtained = 92f, maxMarks = 100f, grade = "A+"),
                    ExamRecordEntity(examId = "E3", subject = "Chemistry", studentId = scholar.id, marksObtained = 88f, maxMarks = 100f, grade = "A"),
                    ExamRecordEntity(examId = "E4", subject = "English", studentId = scholar.id, marksObtained = 90f, maxMarks = 100f, grade = "A")
                )

                subjectsList.forEachIndexed { index, record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFFF8FAFC) else PureWhite)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(record.subject, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GovSlateDark, modifier = Modifier.weight(2f))
                        Text(record.maxMarks.toInt().toString(), fontSize = 11.sp, color = GovSlateDark, modifier = Modifier.weight(1f))
                        Text(record.marksObtained.toInt().toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovGreen, modifier = Modifier.weight(1f))
                        Text(record.grade, fontSize = 11.sp, fontWeight = FontWeight.Black, color = GovNavyPrimary, modifier = Modifier.weight(1f))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Principal Signoff & Seal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Class Teacher Signoff", fontSize = 9.sp, color = GovSlateMuted)
                        Text("Mrs. Sharma, M.Sc.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Principal & Registrar Seal", fontSize = 9.sp, color = GovSlateMuted)
                        Text("Prof. Robert Vance, Ph.D.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                    }
                }
            }
        }
    }
}
