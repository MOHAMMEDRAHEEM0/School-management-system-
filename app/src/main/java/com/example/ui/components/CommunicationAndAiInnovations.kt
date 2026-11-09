package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.AiAcademicAssistant
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.ModernBlue100
import com.example.ui.theme.ModernBlue200
import com.example.ui.theme.ModernBlue50
import com.example.ui.theme.ModernBlue600
import com.example.ui.theme.ModernBlue700
import com.example.ui.theme.ModernBlue800
import com.example.ui.theme.ModernCardBackground
import com.example.ui.theme.ModernCardBorder
import com.example.ui.theme.ModernTextMuted
import com.example.ui.theme.ModernTextPrimary
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// 1. IN-APP PARENT-TEACHER MESSAGING (ROLE-RESTRICTED CHANNEL)
// =============================================================================

data class ParentTeacherMessage(
    val messageId: String,
    val senderName: String,
    val senderRole: String, // "PARENT" or "CLASS_INCHARGE"
    val timestamp: String,
    val messageText: String,
    val categoryTag: String, // "Academic Progress", "Leave Request", "Diet & Health", "General Notice"
    val isRead: Boolean = true
)

@Composable
fun ParentTeacherMessagingSystem(
    parentName: String = "Vikram Sharma (Parent of Aarav Sharma)",
    inchargeName: String = "Mrs. Sunita Kapoor (Class Incharge 10-A)",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputMessageText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Academic Progress") }

    val messages = remember {
        mutableStateListOf(
            ParentTeacherMessage(
                messageId = "MSG-001",
                senderName = "Mrs. Sunita Kapoor (Class Incharge)",
                senderRole = "CLASS_INCHARGE",
                timestamp = "Yesterday 04:30 PM",
                messageText = "Namaste Mr. Sharma. Aarav achieved 95% in Mathematics Summative Assessment 1. He shows great enthusiasm in quadratic formulas.",
                categoryTag = "Academic Progress"
            ),
            ParentTeacherMessage(
                messageId = "MSG-002",
                senderName = "Vikram Sharma (Father)",
                senderRole = "PARENT",
                timestamp = "Today 08:45 AM",
                messageText = "Thank you Mrs. Kapoor! We are working on his science diagrams at home as advised. He will also be attending the Olympiad coaching.",
                categoryTag = "Academic Progress"
            ),
            ParentTeacherMessage(
                messageId = "MSG-003",
                senderName = "Mrs. Sunita Kapoor (Class Incharge)",
                senderRole = "CLASS_INCHARGE",
                timestamp = "Today 10:15 AM",
                messageText = "Wonderful. Tomorrow we have the Chemistry practical exam in Lab A. Please ensure he carries his lab apron and workbook.",
                categoryTag = "General Notice"
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.2.dp, ModernCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("parent_teacher_messaging_system")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ModernBlue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Chat,
                            contentDescription = "Message",
                            tint = ModernBlue700,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = inchargeName,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary
                        )
                        Text(
                            text = "Direct Channel • Secure In-App (No personal phone exposed)",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, GovGreen)
                ) {
                    Text(
                        text = "ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Thread Message Bubbles
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                messages.forEach { msg ->
                    val isFromParent = msg.senderRole == "PARENT"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isFromParent) Alignment.End else Alignment.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isFromParent) 12.dp else 2.dp,
                                bottomEnd = if (isFromParent) 2.dp else 12.dp
                            ),
                            color = if (isFromParent) ModernBlue700 else PureWhite,
                            border = BorderStroke(1.dp, if (isFromParent) ModernBlue800 else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.senderName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFromParent) PureWhite else ModernBlue800
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isFromParent) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
                                    ) {
                                        Text(
                                            text = msg.categoryTag,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFromParent) Color(0xFFBFDBFE) else ModernBlue700,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = msg.messageText,
                                    fontSize = 11.5.sp,
                                    color = if (isFromParent) PureWhite else ModernTextPrimary,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.timestamp,
                                        fontSize = 9.sp,
                                        color = if (isFromParent) Color(0xFF93C5FD) else ModernTextMuted
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        tint = if (isFromParent) Color(0xFF93C5FD) else GovGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message Composer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMessageText,
                    onValueChange = { inputMessageText = it },
                    placeholder = { Text("Message Class Incharge...", fontSize = 11.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_parent_message"),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputMessageText.isNotBlank()) {
                            val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                            messages.add(
                                ParentTeacherMessage(
                                    messageId = "MSG-${System.currentTimeMillis() % 1000}",
                                    senderName = parentName,
                                    senderRole = "PARENT",
                                    timestamp = "Today $timeNow",
                                    messageText = inputMessageText.trim(),
                                    categoryTag = selectedCategory
                                )
                            )
                            inputMessageText = ""
                            Toast.makeText(context, "Message sent to Incharge", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ModernBlue700)
                        .testTag("btn_send_parent_message")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = PureWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// 2. GEMINI AI EXAM BUILDER (TEACHER AI QUESTION PAPER GENERATOR)
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiAiExamBuilderModal(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiAssistant = remember { AiAcademicAssistant() }

    var selectedGrade by remember { mutableStateOf("10") }
    var selectedSubject by remember { mutableStateOf("Science & Physics") }
    var topicInput by remember { mutableStateOf("Optics & Refraction through Lenses") }
    var totalMarks by remember { mutableStateOf("25") }
    var difficultyLevel by remember { mutableStateOf("Standard / Balanced (CBSE)") }
    var questionPattern by remember { mutableStateOf("MCQs + Short Answer + HOTS Conceptual") }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedExamPaper by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ModernBlue100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Exam Builder",
                        tint = ModernBlue700,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Gemini AI Exam Builder",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                    Text(
                        text = "Instant syllabus question paper & marking rubric generator",
                        fontSize = 10.5.sp,
                        color = ModernTextMuted
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (generatedExamPaper == null) {
                    Text(text = "Subject:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModernTextPrimary)
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = { selectedSubject = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_exam_subject")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Grade / Class & Topic:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ModernTextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = selectedGrade,
                            onValueChange = { selectedGrade = it },
                            placeholder = { Text("Grade") },
                            modifier = Modifier.width(90.dp).testTag("input_exam_grade")
                        )
                        OutlinedTextField(
                            value = topicInput,
                            onValueChange = { topicInput = it },
                            placeholder = { Text("Topic name") },
                            modifier = Modifier.weight(1f).testTag("input_exam_topic")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Total Marks:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = totalMarks,
                                onValueChange = { totalMarks = it },
                                modifier = Modifier.fillMaxWidth().testTag("input_exam_marks")
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Difficulty:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = difficultyLevel,
                                onValueChange = { difficultyLevel = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            isGenerating = true
                            coroutineScope.launch {
                                val result = aiAssistant.generateTestPaperAndRubric(
                                    grade = selectedGrade,
                                    subject = selectedSubject,
                                    topic = topicInput,
                                    totalMarks = totalMarks.toIntOrNull() ?: 25,
                                    difficultyLevel = difficultyLevel,
                                    questionTypes = questionPattern
                                )
                                generatedExamPaper = result
                                isGenerating = false
                            }
                        },
                        enabled = !isGenerating && topicInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("btn_generate_exam_ai")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Drafting Question Paper...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Exam Paper with Gemini AI", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Generated Question Paper Display
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
                                    text = "Generated Question Paper & Rubric",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ModernBlue800
                                )

                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Exam Paper", generatedExamPaper)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Exam paper copied to clipboard!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = ModernBlue700, modifier = Modifier.size(16.dp))
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 6.dp))

                            Text(
                                text = generatedExamPaper ?: "",
                                fontSize = 10.5.sp,
                                color = GovSlateDark,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { generatedExamPaper = null },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Draft Another Question Paper", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = ModernBlue700)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(16.dp)
    )
}

// =============================================================================
// 3. GEMINI AI PREDICTIVE ANALYTICS & REPORT REMARKS
// =============================================================================

@Composable
fun GeminiAiPredictiveAnalyticsDeck(
    students: List<StudentEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiAssistant = remember { AiAcademicAssistant() }

    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedStudentForRemarks by remember { mutableStateOf<StudentEntity?>(null) }
    var generatedRemarkResult by remember { mutableStateOf<String?>(null) }

    // Identify At-Risk Students (<75% attendance or falling marks)
    val atRiskStudents = remember(students) {
        if (students.isNotEmpty()) {
            students.filter { it.attendancePercentage < 75.0f }
        } else {
            listOf(
                StudentEntity(
                    id = "ADM-2026-004",
                    admissionNumber = "ADM-2026-004",
                    name = "Kabir Patel",
                    grade = "10",
                    section = "A",
                    rollNumber = 4,
                    attendancePercentage = 71.4f,
                    studentPhone = "+91 91234 56783",
                    guardianName = "Deepak Patel"
                )
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.2.dp, ModernCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gemini_predictive_analytics_deck")
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
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "At-Risk",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Predictive Academic Risk Radar",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary
                        )
                        Text(
                            text = "Detects attendance shortfall (<75%) & falling grade trajectory",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, GovRedAlert)
                ) {
                    Text(
                        text = "${atRiskStudents.size} AT RISK",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovRedAlert,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            atRiskStudents.forEach { stu ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${stu.name} (Roll #${stu.rollNumber}, Class ${stu.grade}-${stu.section})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = "Current Attendance: ${stu.attendancePercentage}% (Mandatory Threshold: 75%)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "Guardian: ${stu.guardianName} • Remedial alert active",
                                fontSize = 9.5.sp,
                                color = ModernTextMuted
                            )
                        }

                        Button(
                            onClick = {
                                selectedStudentForRemarks = stu
                                isAnalyzing = true
                                coroutineScope.launch {
                                    val dummyExams = listOf(
                                        ExamRecordEntity(
                                            examId = "EXAM-1",
                                            examType = "TERM1",
                                            examName = "Summative Assessment",
                                            classSection = "10-A",
                                            subject = "Mathematics",
                                            studentId = stu.id,
                                            studentName = stu.name,
                                            rollNumber = stu.rollNumber,
                                            marksObtained = 64f,
                                            maxMarks = 100f,
                                            grade = "B"
                                        ),
                                        ExamRecordEntity(
                                            examId = "EXAM-2",
                                            examType = "TERM1",
                                            examName = "Summative Assessment",
                                            classSection = "10-A",
                                            subject = "Science",
                                            studentId = stu.id,
                                            studentName = stu.name,
                                            rollNumber = stu.rollNumber,
                                            marksObtained = 58f,
                                            maxMarks = 100f,
                                            grade = "C+"
                                        )
                                    )
                                    val remarks = aiAssistant.generateStudentPerformanceRemarks(
                                        student = stu,
                                        exams = dummyExams,
                                        attendancePercentage = stu.attendancePercentage
                                    )
                                    generatedRemarkResult = remarks
                                    isAnalyzing = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_ai_remedial_remarks_${stu.id}")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Remarks", fontSize = 10.5.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Modal with AI generated diagnostic appraisal
    selectedStudentForRemarks?.let { stu ->
        AlertDialog(
            onDismissRequest = {
                selectedStudentForRemarks = null
                generatedRemarkResult = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ModernBlue700, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI Diagnostic Appraisal: ${stu.name}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                }
            },
            text = {
                if (isAnalyzing) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = ModernBlue700)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Gemini is analyzing academic logs & attendance trend...", fontSize = 11.sp, color = ModernTextMuted)
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Automated Official Report Card Endorsement:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ModernBlue800
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = generatedRemarkResult ?: "",
                                    fontSize = 11.sp,
                                    color = GovSlateDark,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("AI Remark", generatedRemarkResult)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Remark copied to report card!", Toast.LENGTH_SHORT).show()
                        selectedStudentForRemarks = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Report Card", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStudentForRemarks = null }) {
                    Text("Close", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
