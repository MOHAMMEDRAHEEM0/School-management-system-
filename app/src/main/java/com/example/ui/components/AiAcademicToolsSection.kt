package com.example.ui.components

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch

/**
 * AI Academic Tools Section featuring Gemini-powered capabilities for Principal & Teachers:
 * 1. AI Performance Remarks & Report Cards
 * 2. Smart Circular & Notice Drafter
 * 3. AI Question Paper & Quiz Generator
 * 4. Attendance Insights & At-Risk Alerts
 */
@Composable
fun AiAcademicToolsSection(
    viewModel: SchoolViewModel,
    userRole: String, // "PRINCIPAL" or "TEACHER"
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val students by viewModel.students.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val feeRecords by viewModel.feeRecords.collectAsState()

    var activeSubTool by remember {
        mutableIntStateOf(if (userRole == "PRINCIPAL") 1 else 0) // Principal defaults to Circulars, Teacher to Student Remarks
    }

    Column(
        modifier = modifier
            .testTag("ai_academic_tools_section")
            .fillMaxWidth()
    ) {
        // AI Hub Header Ribbon
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerRibbonColor = GovNavyPrimary,
            headerTitle = "GEMINI ACADEMIC CO-PILOT",
            headerBadge = "AI 3.5 FLASH"
        ) {
            Column {
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
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Gemini",
                                tint = GovNavyPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Intelligent Educational Suite",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = GovNavyDark
                            )
                            Text(
                                text = "Teacher-supervised report evaluations, circular drafting & test generators",
                                fontSize = 9.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }
                    GlassBadge(
                        text = if (userRole == "PRINCIPAL") "EXECUTIVE SUITE" else "FACULTY ASSIST",
                        color = if (userRole == "PRINCIPAL") GovSaffron else GovGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub-tool Navigation Tabs
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeSubTool,
                containerColor = PureWhite,
                edgePadding = 6.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTool]),
                        color = GovNavyPrimary,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = activeSubTool == 0,
                    onClick = { activeSubTool = 0 },
                    text = { Text("Report Card Remarks", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeSubTool == 1,
                    onClick = { activeSubTool = 1 },
                    text = { Text("Smart Circulars", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeSubTool == 2,
                    onClick = { activeSubTool = 2 },
                    text = { Text("Question Paper / Quiz", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = activeSubTool == 3,
                    onClick = { activeSubTool = 3 },
                    text = { Text("At-Risk & Insights", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (activeSubTool) {
            // =================================================================
            // 1. AI PERFORMANCE REMARKS & REPORT CARDS
            // =================================================================
            0 -> {
                var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }
                var isGenerating by remember { mutableStateOf(false) }
                var generatedRemarks by remember { mutableStateOf("") }
                val currentStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovGreen,
                    headerTitle = "TEACHER SUPERVISED REMARKS",
                    headerBadge = currentStudent?.let { "CLASS ${it.grade}-${it.section}" } ?: "ROSTER"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Generate comprehensive, balanced evaluation summaries highlighting academic strengths, growth areas, and classroom demeanor.",
                            fontSize = 10.5.sp,
                            color = GovSlateDark
                        )

                        // Scholar Selection Selector
                        Text("Select Scholar for Term Report Card:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            students.take(5).forEach { stu ->
                                val isSelected = stu.id == (currentStudent?.id ?: "")
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) GovNavyPrimary else PureWhite)
                                        .border(1.dp, if (isSelected) GovNavyPrimary else GlassBorderLight, RoundedCornerShape(6.dp))
                                        .clickable { selectedStudentId = stu.id }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "#${stu.rollNumber}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PureWhite else GovSlateMuted
                                        )
                                        Text(
                                            text = stu.name.split(" ").firstOrNull() ?: stu.name,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PureWhite else GovSlateDark,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        if (currentStudent != null) {
                            val studentExams = examRecords.filter { it.studentId == currentStudent.id }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Attendance: ${currentStudent.attendancePercentage}%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                                Text("Exams on Record: ${studentExams.size} subjects", fontSize = 10.sp, color = GovSlateMuted)
                            }
                        }

                        GlassButton(
                            text = if (isGenerating) "Consulting Gemini 3.5 Flash..." else "Generate AI Report Remark",
                            onClick = {
                                if (currentStudent == null) return@GlassButton
                                isGenerating = true
                                coroutineScope.launch {
                                    val studentExams = examRecords.filter { it.studentId == currentStudent.id }
                                    generatedRemarks = viewModel.generateStudentAiEvaluation(
                                        student = currentStudent,
                                        exams = studentExams,
                                        attendancePercentage = currentStudent.attendancePercentage
                                    )
                                    isGenerating = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = GovNavyPrimary,
                            enabled = !isGenerating,
                            leadingIcon = {
                                if (isGenerating) {
                                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        if (generatedRemarks.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GovGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Official Evaluation Remark Preview",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GovGreen
                                        )
                                        Row {
                                            GlassOutlinedButton(
                                                text = "Copy",
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(generatedRemarks))
                                                    Toast.makeText(context, "Remark copied to clipboard!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.height(32.dp),
                                                leadingIcon = {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                                }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = generatedRemarks,
                                        fontSize = 11.sp,
                                        color = GovSlateDark,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 2. SMART CIRCULAR & NOTICE DRAFTER
            // =================================================================
            1 -> {
                var circularTitle by remember { mutableStateOf("Parent-Teacher Conference & Mid-Term Review") }
                var circularType by remember { mutableStateOf("PARENT_CIRCULAR") }
                var targetAudience by remember { mutableStateOf("Parents of Grades 9 & 10") }
                var keyPoints by remember { mutableStateOf("Discussion of Term 1 progress, submission of science projects, fee reconciliation, and interactive session with class in-charges.") }
                var isDrafting by remember { mutableStateOf(false) }
                var draftedCircularText by remember { mutableStateOf("") }
                var isPublishedAlert by remember { mutableStateOf(false) }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovSaffron,
                    headerTitle = "OFFICIAL NOTICE & CIRCULAR DRAFTER",
                    headerBadge = "ADMIN DESK"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Draft formal administrative notices, parent communications, board circulars, or faculty agendas formatted with institutional letterhead standards.",
                            fontSize = 10.5.sp,
                            color = GovSlateDark
                        )

                        GlassTextField(
                            value = circularTitle,
                            onValueChange = { circularTitle = it },
                            placeholder = "e.g. Annual Sports Meet Schedule",
                            label = "Notice / Circular Subject"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassTextField(
                                value = targetAudience,
                                onValueChange = { targetAudience = it },
                                placeholder = "e.g. All Faculty / Parents",
                                label = "Target Audience",
                                modifier = Modifier.weight(1f)
                            )
                            GlassTextField(
                                value = circularType,
                                onValueChange = { circularType = it },
                                placeholder = "PARENT_CIRCULAR",
                                label = "Category",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        GlassTextField(
                            value = keyPoints,
                            onValueChange = { keyPoints = it },
                            placeholder = "Key directives, dates, timings, venues, and mandatory instructions...",
                            label = "Key Directives & Agenda Notes",
                            singleLine = false,
                            maxLines = 3
                        )

                        GlassButton(
                            text = if (isDrafting) "Drafting Institutional Notice..." else "Draft Circular with Gemini AI",
                            onClick = {
                                if (circularTitle.isBlank()) return@GlassButton
                                isDrafting = true
                                isPublishedAlert = false
                                coroutineScope.launch {
                                    draftedCircularText = viewModel.generateSmartNotice(
                                        title = circularTitle,
                                        circularType = circularType,
                                        targetAudience = targetAudience,
                                        keyPoints = keyPoints,
                                        effectiveDate = "2026-09-22"
                                    )
                                    isDrafting = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = GovNavyPrimary,
                            enabled = !isDrafting,
                            leadingIcon = {
                                if (isDrafting) {
                                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        if (draftedCircularText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Official Circular Preview",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GovNavyDark
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            GlassOutlinedButton(
                                                text = "Copy",
                                                onClick = {
                                                    clipboardManager.setText(AnnotatedString(draftedCircularText))
                                                    Toast.makeText(context, "Circular copied!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.height(32.dp)
                                            )
                                            GlassButton(
                                                text = "Publish & Broadcast",
                                                onClick = {
                                                    viewModel.publishGeneratedCircular(
                                                        title = circularTitle,
                                                        content = draftedCircularText,
                                                        targetAudience = targetAudience
                                                    )
                                                    isPublishedAlert = true
                                                    Toast.makeText(context, "Circular published to school register!", Toast.LENGTH_LONG).show()
                                                },
                                                color = GovGreen,
                                                modifier = Modifier.height(32.dp),
                                                leadingIcon = {
                                                    Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(12.dp))
                                                }
                                            )
                                        }
                                    }

                                    if (isPublishedAlert) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovGreen, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Committed into Room Database notices table with active push broadcast.", fontSize = 9.5.sp, color = GovGreen, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = draftedCircularText,
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GovSlateDark,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 3. AI QUESTION PAPER & QUIZ GENERATOR
            // =================================================================
            2 -> {
                var quizGrade by remember { mutableStateOf("10") }
                var quizSubject by remember { mutableStateOf("Physics (Science)") }
                var quizTopic by remember { mutableStateOf("Electromagnetic Induction & Faraday's Laws") }
                var quizTotalMarks by remember { mutableStateOf("25") }
                var quizDifficulty by remember { mutableStateOf("Standard / Balanced") }
                var quizPattern by remember { mutableStateOf("MCQs + Short Conceptual + 1 HOTS Problem") }
                var isGeneratingQuiz by remember { mutableStateOf(false) }
                var generatedQuizText by remember { mutableStateOf("") }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovNavyPrimary,
                    headerTitle = "QUESTION PAPER & RUBRICS",
                    headerBadge = "CURRICULUM ENGINE"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Instantly design class tests, unit quizzes, and grading rubrics from syllabus topics with answer keys and marking criteria.",
                            fontSize = 10.5.sp,
                            color = GovSlateDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassTextField(
                                value = quizGrade,
                                onValueChange = { quizGrade = it },
                                placeholder = "10",
                                label = "Grade / Class",
                                modifier = Modifier.weight(1f)
                            )
                            GlassTextField(
                                value = quizSubject,
                                onValueChange = { quizSubject = it },
                                placeholder = "Mathematics / Science",
                                label = "Subject",
                                modifier = Modifier.weight(2f)
                            )
                        }

                        GlassTextField(
                            value = quizTopic,
                            onValueChange = { quizTopic = it },
                            placeholder = "e.g. Quadratic Equations, Plant Physiology...",
                            label = "Syllabus Topic / Chapter"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassTextField(
                                value = quizTotalMarks,
                                onValueChange = { quizTotalMarks = it },
                                placeholder = "25",
                                label = "Total Marks",
                                modifier = Modifier.weight(1f)
                            )
                            GlassTextField(
                                value = quizDifficulty,
                                onValueChange = { quizDifficulty = it },
                                placeholder = "Standard / Balanced",
                                label = "Difficulty Level",
                                modifier = Modifier.weight(2f)
                            )
                        }

                        GlassButton(
                            text = if (isGeneratingQuiz) "Generating Assessment & Rubric..." else "Generate Test Paper & Rubric",
                            onClick = {
                                if (quizTopic.isBlank()) return@GlassButton
                                isGeneratingQuiz = true
                                coroutineScope.launch {
                                    val marks = quizTotalMarks.toIntOrNull() ?: 25
                                    generatedQuizText = viewModel.generateClassQuizAndRubric(
                                        grade = quizGrade,
                                        subject = quizSubject,
                                        topic = quizTopic,
                                        totalMarks = marks,
                                        difficultyLevel = quizDifficulty,
                                        questionTypes = quizPattern
                                    )
                                    isGeneratingQuiz = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = GovNavyPrimary,
                            enabled = !isGeneratingQuiz,
                            leadingIcon = {
                                if (isGeneratingQuiz) {
                                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        if (generatedQuizText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Test Paper & Rubric Scheme",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GovNavyDark
                                        )
                                        GlassOutlinedButton(
                                            text = "Copy All",
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(generatedQuizText))
                                                Toast.makeText(context, "Test Paper copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = generatedQuizText,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GovSlateDark,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =================================================================
            // 4. ATTENDANCE INSIGHTS & AT-RISK ALERTS
            // =================================================================
            3 -> {
                var isAnalyzing by remember { mutableStateOf(false) }
                var analysisReport by remember { mutableStateOf("") }

                val lowAttendanceCount = students.count { it.attendancePercentage < 75f }
                val feeDuesCount = feeRecords.count { it.dueAmount > 0 }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovRedAlert,
                    headerTitle = "AT-RISK SCHOLAR & ATTENDANCE ALERTS",
                    headerBadge = "$lowAttendanceCount FLAGGED"
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "AI-driven detection of chronic absenteeism drop patterns, statutory attendance thresholds (<75%), and automated guardian reminder notices.",
                            fontSize = 10.5.sp,
                            color = GovSlateDark
                        )

                        // Risk Highlights Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GovRedAlert.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GovRedAlert.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ATTENDANCE RISK", fontSize = 9.sp, fontWeight = FontWeight.Black, color = GovRedAlert)
                                    Text("$lowAttendanceCount Scholars", fontSize = 15.sp, fontWeight = FontWeight.Black, color = GovRedAlert)
                                    Text("Below statutory 75%", fontSize = 9.sp, color = GovSlateMuted)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GovSaffron.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GovSaffron.copy(alpha = 0.3f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("FEE RECONCILIATION", fontSize = 9.sp, fontWeight = FontWeight.Black, color = GovSaffron)
                                    Text("$feeDuesCount Schedules", fontSize = 15.sp, fontWeight = FontWeight.Black, color = GovSaffron)
                                    Text("Dues pending collection", fontSize = 9.sp, color = GovSlateMuted)
                                }
                            }
                        }

                        GlassButton(
                            text = if (isAnalyzing) "Synthesizing Attendance & Fee Signals..." else "Run Institutional Risk Analysis",
                            onClick = {
                                isAnalyzing = true
                                coroutineScope.launch {
                                    analysisReport = viewModel.getAttendanceAndFeeRiskAnalysis(
                                        students = students,
                                        feeRecords = feeRecords
                                    )
                                    isAnalyzing = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = GovRedAlert,
                            enabled = !isAnalyzing,
                            leadingIcon = {
                                if (isAnalyzing) {
                                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                }
                            }
                        )

                        if (analysisReport.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = PureWhite,
                                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Executive Risk Briefing",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GovNavyDark
                                        )
                                        GlassOutlinedButton(
                                            text = "Copy Briefing",
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(analysisReport))
                                                Toast.makeText(context, "Briefing copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.height(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = analysisReport,
                                        fontSize = 10.5.sp,
                                        color = GovSlateDark,
                                        lineHeight = 15.sp
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
