package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
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
import com.example.core.FeatureAvailability
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entities.StudentEntity
import com.example.data.model.UserRole
import com.example.ui.components.AttendanceFeeSyncWorkManagerCard
import com.example.ui.components.AttendanceWhatsAppGenerator
import com.example.ui.components.AttendanceWhatsAppPushSheet
import com.example.ui.components.DailyAttendanceRecorder
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.launchWhatsAppMessage
import com.example.ui.theme.GlassBorderLight
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
import com.example.ui.viewmodel.TeacherAttendanceViewModel
import com.example.ui.viewmodel.TeacherAttendanceViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: SchoolViewModel,
    currentRole: UserRole,
    onNavigateBack: () -> Unit,
    onNavigateToExamResults: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayDateFormatted = remember {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())
    }

    val attendanceViewModel: TeacherAttendanceViewModel = viewModel(
        factory = TeacherAttendanceViewModelFactory(
            attendanceRepository = viewModel.attendanceRepository,
            schoolRepository = viewModel.repository
        )
    )

    val attendanceUiState by attendanceViewModel.uiState.collectAsState()
    val students by viewModel.students.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showWhatsAppBroadcastSheet by remember { mutableStateOf(false) }

    // RBAC Security Check: If current user is Student or Parent, strictly isolate to StudentAttendancePortal
    // Never allow students or parents to access Teacher DailyAttendanceRecorder or batch saving!
    if (currentRole == UserRole.STUDENT || currentRole == UserRole.PARENT) {
        val activeStudent = students.firstOrNull { it.admissionNumber == "ADM-2026-001" } ?: students.firstOrNull()
        com.example.ui.components.StudentAttendancePortal(
            viewModel = viewModel,
            student = activeStudent,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("attendance_screen_root")
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
                                .testTag("btn_attendance_back")
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
                                    text = "ATTENDANCE CENTRAL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassBadge(
                                    text = attendanceUiState.selectedClass,
                                    color = GovNavyPrimary
                                )
                            }
                            Text(
                                text = "Portal > Attendance Management • $todayDateFormatted",
                                fontSize = 10.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    // Shortcut to Exam Results
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GovSaffron.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, GovSaffron.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable(onClick = onNavigateToExamResults)
                            .testTag("btn_jump_to_exam_results_from_attendance")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Exam Results",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KPI Mini Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val summary = attendanceUiState.summary
                    val total = summary.totalCount
                    val present = summary.presentCount
                    val absent = summary.absentCount
                    val pct = summary.attendancePercentage

                    QuickKpiCard(
                        title = "Present Rate",
                        value = "${pct.toInt()}%",
                        color = if (pct >= 85f) GovGreen else if (pct >= 75f) GovSaffron else GovRedAlert,
                        modifier = Modifier.weight(1f)
                    )
                    QuickKpiCard(
                        title = "Present",
                        value = "$present / $total",
                        color = GovGreen,
                        modifier = Modifier.weight(1f)
                    )
                    QuickKpiCard(
                        title = "Absent Today",
                        value = "$absent",
                        color = if (absent > 0) GovRedAlert else GovSlateMuted,
                        modifier = Modifier.weight(1f)
                    )
                    QuickKpiCard(
                        title = "Outbox Status",
                        value = if (attendanceUiState.isAlreadyRecorded) "Committed" else "Pending",
                        color = GovNavyPrimary,
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
                text = { Text("Daily Roll Call", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_attendance_roll_call")
            )
            if (FeatureAvailability.isWhatsAppEnabled) {
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("WhatsApp Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_attendance_whatsapp")
                )
            }
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Roster Overview", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_attendance_roster")
            )
        }

        // =====================================================================
        // 3. TAB CONTENT VIEWS
        // =====================================================================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Embeds complete Daily Attendance Recorder with Room DB batch saving & per-student controls
                    DailyAttendanceRecorder(
                        viewModel = attendanceViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                1 -> {
                    // Automated WhatsApp Push Central
                    AttendanceWhatsAppConsole(
                        attendanceViewModel = attendanceViewModel,
                        onOpenBroadcastModal = { showWhatsAppBroadcastSheet = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                2 -> {
                    // Scholar Roster & Attendance Analytics
                    AttendanceRosterOverview(
                        students = students.filter { it.section == "A" },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Modal Broadcast Sheet
    if (showWhatsAppBroadcastSheet) {
        AttendanceWhatsAppPushSheet(
            roster = attendanceUiState.roster,
            selectedClass = attendanceUiState.selectedClass,
            selectedDate = attendanceUiState.displayDate,
            currentSession = attendanceUiState.selectedSession,
            onDismiss = { showWhatsAppBroadcastSheet = false },
            onAuditDispatch = { count ->
                attendanceViewModel.recordWhatsAppBatchAudit(count)
            }
        )
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================

@Composable
private fun QuickKpiCard(
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
private fun AttendanceWhatsAppConsole(
    attendanceViewModel: TeacherAttendanceViewModel,
    onOpenBroadcastModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by attendanceViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerTitle = "Automated WhatsApp Push System",
            headerBadge = "COMPUTER GENERATED",
            headerRibbonColor = Color(0xFF25D366)
        ) {
            Text(
                text = "Push computerized daily attendance status to students' verified mobile numbers via WhatsApp. Messages automatically evaluate whether a student was present in all classes, missed specific sessions, or was absent today.",
                fontSize = 12.sp,
                color = GovSlateDark,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassButton(
                    text = "Launch Class Broadcast Console",
                    onClick = onOpenBroadcastModal,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    color = Color(0xFF25D366),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Live Template Preview Showcase
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerTitle = "Computer-Generated Template Previews",
            headerBadge = "3 STANDARD RULES",
            headerRibbonColor = GovNavyPrimary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TemplatePreviewBox(
                    ruleTitle = "Rule 1: Attended All Classes",
                    sampleText = "ST. XAVIER'S INTERNATIONAL ACADEMY\nDate: 2026-09-24\n\nScholar: Aarav Sharma\nAarav Sharma was present in all classes \n\n_This is a default computer-generated notification._",
                    accentColor = GovGreen
                )

                TemplatePreviewBox(
                    ruleTitle = "Rule 2: Marked Absent in Specific Classes",
                    sampleText = "ST. XAVIER'S INTERNATIONAL ACADEMY\nDate: 2026-09-24\n\nScholar: Diya Patel\nDiya Patel was not present in this class: Physics Lab\n\n_This is a default computer-generated notification._",
                    accentColor = GovSaffron
                )

                TemplatePreviewBox(
                    ruleTitle = "Rule 3: Absent Today",
                    sampleText = "ST. XAVIER'S INTERNATIONAL ACADEMY\nDate: 2026-09-24\n\nScholar: Rohan Verma\nRohan Verma was absent today\n\n_This is a default computer-generated notification._",
                    accentColor = GovRedAlert
                )
            }
        }
    }
}

@Composable
private fun TemplatePreviewBox(
    ruleTitle: String,
    sampleText: String,
    accentColor: Color
) {
    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ruleTitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Automated",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = Color(0xFFE7FBD3),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sampleText,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF075E54),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun AttendanceRosterOverview(
    students: List<StudentEntity>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AttendanceFeeSyncWorkManagerCard(
                institutionId = "INST-SXA-2026"
            )
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerTitle = "Cumulative Attendance Standing (Grade 10-A)",
                headerBadge = "${students.size} SCHOLARS",
                headerRibbonColor = GovNavyPrimary
            ) {
                Text(
                    text = "Statutory academic compliance mandates minimum 75% attendance to sit for Central Board Examinations.",
                    fontSize = 11.5.sp,
                    color = GovSlateDark
                )
            }
        }

        items(students, key = { it.id }) { student ->
            val pct = student.attendancePercentage
            val isLow = pct < 75.0f

            Surface(
                color = PureWhite,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isLow) GovRedAlert.copy(alpha = 0.4f) else GovBorderGrey),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isLow) GovRedAlert else GovNavyPrimary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.rollNumber.toString(),
                                    color = PureWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = student.name,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Text(
                                text = "Adm: ${student.admissionNumber} • Phone: ${student.studentPhone}",
                                fontSize = 10.5.sp,
                                color = GovSlateMuted
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${pct.toInt()}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (pct >= 85f) GovGreen else if (pct >= 75f) GovSaffron else GovRedAlert
                        )
                        Text(
                            text = if (isLow) "Shortage (<75%)" else "Eligible",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) GovRedAlert else GovGreen
                        )
                    }
                }
            }
        }
    }
}
