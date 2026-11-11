package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.core.FeatureAvailability
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.viewmodel.StudentAttendanceItem
import com.example.ui.viewmodel.TeacherAttendanceUiState
import com.example.ui.viewmodel.TeacherAttendanceViewModel

/**
 * Daily Attendance Recording Component for Teachers.
 * Connects directly to Room Database via TeacherAttendanceViewModel.
 */
@Composable
fun DailyAttendanceRecorder(
    viewModel: TeacherAttendanceViewModel,
    modifier: Modifier = Modifier,
    onSaveSuccess: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var singleStudentPreviewInfo by remember { mutableStateOf<StudentAttendancePushInfo?>(null) }

    val filteredRoster = if (searchQuery.isBlank()) {
        state.roster
    } else {
        state.roster.filter {
            it.studentName.contains(searchQuery, ignoreCase = true) ||
            it.rollNumber.toString().contains(searchQuery) ||
            it.admissionNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_attendance_recorder"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Class & Session Selector Bar
        ClassAndSessionRibbon(
            state = state,
            onClassSelected = { viewModel.selectClass(it) },
            onSessionSelected = { viewModel.selectSession(it) }
        )

        // Date Navigator Bar
        DateNavigatorBar(
            state = state,
            onPreviousDay = { viewModel.previousDay() },
            onNextDay = { viewModel.nextDay() },
            onToday = { viewModel.setToday() }
        )

        // Real-Time Stats & Room Sync Status Bar
        AttendanceStatsGauge(
            summary = state.summary,
            isAlreadyRecorded = state.isAlreadyRecorded,
            lastSavedTimestamp = state.lastSavedTimestamp
        )

        // Quick Batch Operations Ribbon (Mark All Present, Clear, Search, WhatsApp Push)
        BatchOperationsBar(
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onMarkAllPresent = {
                viewModel.markAllPresent()
                Toast.makeText(context, "All students set to PRESENT", Toast.LENGTH_SHORT).show()
            },
            onMarkAllAbsent = {
                viewModel.markAllAbsent()
                Toast.makeText(context, "All students set to ABSENT", Toast.LENGTH_SHORT).show()
            },
            onOpenWhatsAppPush = { viewModel.openWhatsAppPushSheet() }
        )

        // Post-Save WhatsApp Push Prompt Banner
        if (state.showPostSaveWhatsAppPrompt) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_post_save_whatsapp"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Push WhatsApp Notices to Linked Numbers?",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Computer-generated attendance notices ready for ${state.roster.size} students (${state.summary.presentCount} P, ${state.summary.absentCount} A).",
                                fontSize = 10.sp,
                                color = GovSlateMuted
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.dismissPostSaveWhatsAppPrompt() }) {
                            Text("Later", fontSize = 11.sp, color = GovSlateMuted)
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { viewModel.openWhatsAppPushSheet() }
                                .testTag("btn_banner_push_whatsapp"),
                            color = Color(0xFF25D366)
                        ) {
                            Text(
                                text = "Push Now",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Authorization Warning Banner
        if (!state.isAuthorizedToMark && state.authorizationError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_authorization_warning"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFEF2F2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = GovRedAlert, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Attendance Editing Restricted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = state.authorizationError ?: "You do not have authorization to mark attendance for this class.",
                            fontSize = 10.5.sp,
                            color = Color(0xFF7F1D1D)
                        )
                    }
                }
            }
        }

        // Duplicate Submission Warning Banner
        if (state.duplicateWarning != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_duplicate_warning"),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBBF24))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Attendance Previously Recorded",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "${state.duplicateWarning}. Saving new entries will atomically update this register.",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }
        }

        // Outbox Offline / Sync Pending Ribbon
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ribbon_outbox_sync_status"),
            shape = RoundedCornerShape(8.dp),
            color = if (state.pendingOutboxCount > 0) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (state.pendingOutboxCount > 0) Color(0xFF93C5FD) else GovBorderGrey)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = if (state.pendingOutboxCount > 0) Color(0xFF1D4ED8) else GovSlateMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isSyncingOutbox) {
                            "Syncing changes with Firestore..."
                        } else if (state.pendingOutboxCount > 0) {
                            "${state.pendingOutboxCount} attendance events pending cloud sync in Room"
                        } else if (state.isAlreadyRecorded) {
                            "Local register synchronized with central cloud"
                        } else {
                            "Local offline Room outbox active • No pending sync"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (state.pendingOutboxCount > 0) Color(0xFF1E40AF) else GovSlateMuted
                    )
                }

                if (state.pendingOutboxCount > 0 && !state.isSyncingOutbox) {
                    TextButton(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier.testTag("btn_trigger_outbox_sync")
                    ) {
                        Text("Sync Now", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                    }
                } else if (state.isSyncingOutbox) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color(0xFF1D4ED8))
                }
            }
        }

        // Validation Error Banner
        state.validationError?.let { vErr ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_validation_error"),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEE2E2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GovRedAlert, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = vErr, fontSize = 11.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Feedback Banner (Success / Error)
        state.feedbackMessage?.let { msg ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_feedback_success"),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFDCFCE7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = msg, fontSize = 11.5.sp, color = Color(0xFF166534), fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { viewModel.dismissFeedback() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF166534), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        state.errorMessage?.let { err ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_feedback_error"),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEE2E2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GovRedAlert, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = err, fontSize = 11.5.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.SemiBold)
                    }
                    IconButton(onClick = { viewModel.dismissFeedback() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF991B1B), modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Student Roster Cards
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GovNavyPrimary, modifier = Modifier.size(36.dp))
            }
        } else if (filteredRoster.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = PureWhite,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = GovSlateMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No students match '$searchQuery'" else "No students registered in Class ${state.selectedClass}",
                        fontSize = 13.sp,
                        color = GovSlateMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filteredRoster.forEach { item ->
                    StudentAttendanceCard(
                        item = item,
                        onStatusChange = { newStatus -> viewModel.setStudentStatus(item.studentId, newStatus) },
                        onRemarksChange = { remarks -> viewModel.setStudentRemarks(item.studentId, remarks) },
                        onOpenWhatsAppNotice = {
                            val pushInfo = AttendanceWhatsAppGenerator.buildPushInfo(
                                item = item,
                                className = state.selectedClass,
                                dateFormatted = state.selectedDate,
                                currentSession = state.selectedSession
                            )
                            singleStudentPreviewInfo = pushInfo
                        }
                    )
                }
            }
        }

        // Bottom Fixed / Floating Action: Save to Room DB & WhatsApp Push
        SaveAttendanceActionBar(
            state = state,
            onReview = { viewModel.setReviewMode(true) },
            onSave = {
                viewModel.saveDailyAttendance { success, msg ->
                    if (success) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onSaveSuccess(msg)
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onOpenWhatsAppPush = { viewModel.openWhatsAppPushSheet() }
        )
    }

    // Attendance Review Mode Modal
    if (state.isReviewMode) {
        AttendanceReviewDialog(
            state = state,
            onDismiss = { viewModel.setReviewMode(false) },
            onConfirm = {
                viewModel.saveDailyAttendance { success, msg ->
                    if (success) {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        onSaveSuccess(msg)
                    } else {
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Full Attendance WhatsApp Push Bottom Sheet
    if (state.showWhatsAppPushSheet) {
        AttendanceWhatsAppPushSheet(
            roster = state.roster,
            selectedClass = state.selectedClass,
            selectedDate = state.selectedDate,
            currentSession = state.selectedSession,
            onDismiss = { viewModel.closeWhatsAppPushSheet() },
            onAuditDispatch = { count ->
                viewModel.recordWhatsAppBatchAudit(count)
            }
        )
    }

    // Individual Student WhatsApp Preview Modal
    singleStudentPreviewInfo?.let { info ->
        IndividualStudentWhatsAppNoticeDialog(
            info = info,
            onDismiss = { singleStudentPreviewInfo = null },
            onSendWhatsApp = {
                launchWhatsAppMessage(context, info.linkedMobile, info.generatedMessage)
                singleStudentPreviewInfo = null
            }
        )
    }
}

@Composable
private fun ClassAndSessionRibbon(
    state: TeacherAttendanceUiState,
    onClassSelected: (String) -> Unit,
    onSessionSelected: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Class selection horizontal chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "Select Class Section:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )
                Spacer(modifier = Modifier.width(6.dp))
                GlassBadge(text = "${state.roster.size} SCHOLARS", color = GovNavyPrimary)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.availableClasses.forEach { cls ->
                    val isSelected = cls == state.selectedClass
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClassSelected(cls) }
                            .testTag("class_chip_$cls"),
                        color = if (isSelected) GovNavyPrimary else Color(0xFFF1F5F9),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GovNavyDark else Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = cls,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) PureWhite else GovSlateDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Session chips
            Text(
                text = "Academic Session:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GovNavyDark,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.availableSessions.forEach { sess ->
                    val isSelected = sess == state.selectedSession
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSessionSelected(sess) },
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) GovNavyPrimary else Color(0xFFE2E8F0)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isSelected) GovNavyPrimary else GovSlateMuted,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sess,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GovNavyPrimary else GovSlateText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateNavigatorBar(
    state: TeacherAttendanceUiState,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPreviousDay,
                modifier = Modifier.size(36.dp).testTag("btn_prev_day")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Day",
                    tint = GovNavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToday() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = null,
                        tint = GovNavyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = state.displayDate,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                }
                Text(
                    text = "Date Key: ${state.selectedDate} • Tap for Today",
                    fontSize = 10.sp,
                    color = GovSlateMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToday() },
                    color = Color(0xFFEFF6FF),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "Today",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier.size(36.dp).testTag("btn_next_day")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = GovNavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceStatsGauge(
    summary: com.example.ui.viewmodel.AttendanceSummary,
    isAlreadyRecorded: Boolean,
    lastSavedTimestamp: Long?
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A), // Dark slate container for high-contrast HUD
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Local Room DB Attendance Status",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAlreadyRecorded) Color(0xFF166534) else Color(0xFF854D0E)
                ) {
                    Text(
                        text = if (isAlreadyRecorded) "SAVED IN ROOM" else "UNSAVED DRAFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4 Stat Counters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCounterPill(label = "Total", count = summary.totalCount, color = Color(0xFF94A3B8))
                StatCounterPill(label = "Present", count = summary.presentCount, color = Color(0xFF4ADE80))
                StatCounterPill(label = "Absent", count = summary.absentCount, color = Color(0xFFF87171))
                StatCounterPill(label = "Late", count = summary.lateCount, color = Color(0xFFFBBF24))
                StatCounterPill(label = "Leave", count = summary.leaveCount, color = Color(0xFFA78BFA))
                StatCounterPill(
                    label = "Rate",
                    countText = "${summary.attendancePercentage}%",
                    color = if (summary.attendancePercentage >= 75f) Color(0xFF4ADE80) else Color(0xFFF87171)
                )
            }
        }
    }
}

@Composable
private fun StatCounterPill(
    label: String,
    count: Int = 0,
    countText: String? = null,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = countText ?: count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = label,
            fontSize = 9.5.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BatchOperationsBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onMarkAllAbsent: () -> Unit,
    onOpenWhatsAppPush: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fast Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search scholar or roll #...", fontSize = 11.5.sp, color = GovSlateMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GovSlateMuted, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = GovSlateMuted, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GovNavyPrimary,
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            // Mark All Present
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMarkAllPresent() }
                    .testTag("btn_mark_all_present"),
                color = Color(0xFFDCFCE7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = null, tint = GovGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All Present",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovGreen
                    )
                }
            }

            // Mark All Absent
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onMarkAllAbsent() }
                    .testTag("btn_mark_all_absent"),
                color = Color(0xFFFEE2E2),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = GovRedAlert, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "All Absent",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovRedAlert
                    )
                }
            }
        }

        // WhatsApp Push Trigger Ribbon
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onOpenWhatsAppPush() }
                .testTag("btn_open_whatsapp_push"),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "WhatsApp Attendance Push to Linked Mobiles",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
                Surface(
                    color = Color(0xFF25D366),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Open Broadcast Console",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StudentAttendanceCard(
    item: StudentAttendanceItem,
    onStatusChange: (String) -> Unit,
    onRemarksChange: (String) -> Unit,
    onOpenWhatsAppNotice: () -> Unit
) {
    var showRemarksInput by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (item.status) {
                "PRESENT" -> Color(0xFFBBF7D0)
                "ABSENT" -> Color(0xFFFECACA)
                "LATE" -> Color(0xFFFED7AA)
                "LEAVE" -> Color(0xFFDDD6FE)
                else -> GlassBorderLight
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${item.rollNumber}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Scholar Identity
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GovNavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${item.rollNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = PureWhite
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = item.studentName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Adm: ${item.admissionNumber}",
                                fontSize = 9.5.sp,
                                color = GovSlateMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Avg: ${item.historicalPercentage.toInt()}%",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (item.historicalPercentage >= 75f) GovGreen else GovRedAlert
                            )
                        }
                    }
                }

                // 4-Way Segmented Quick Status Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(
                        label = "P",
                        fullLabel = "Present",
                        isSelected = item.status == "PRESENT",
                        activeColor = GovGreen,
                        activeBg = Color(0xFFDCFCE7),
                        onClick = { onStatusChange("PRESENT") }
                    )
                    StatusPill(
                        label = "A",
                        fullLabel = "Absent",
                        isSelected = item.status == "ABSENT",
                        activeColor = GovRedAlert,
                        activeBg = Color(0xFFFEE2E2),
                        onClick = { onStatusChange("ABSENT") }
                    )
                    StatusPill(
                        label = "L",
                        fullLabel = "Late",
                        isSelected = item.status == "LATE",
                        activeColor = GovSaffron,
                        activeBg = Color(0xFFFEF3C7),
                        onClick = { onStatusChange("LATE") }
                    )
                    StatusPill(
                        label = "LV",
                        fullLabel = "Leave",
                        isSelected = item.status == "LEAVE",
                        activeColor = Color(0xFF7C3AED),
                        activeBg = Color(0xFFEDE9FE),
                        onClick = { onStatusChange("LEAVE") }
                    )
                }
            }

            // Linked Phone & WhatsApp Action Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Linked Mobile indicator
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Linked: ${item.studentPhone.ifBlank { item.guardianPhone.ifBlank { "9876543210" } }}",
                        fontSize = 10.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Direct WhatsApp Push trigger for this student
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenWhatsAppNotice() }
                            .testTag("btn_whatsapp_student_${item.rollNumber}"),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366), modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "WhatsApp",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    Text(
                        text = if (showRemarksInput) "Done" else if (item.remarks.isNotBlank()) "Edit Note" else "+ Note",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showRemarksInput = !showRemarksInput }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (item.remarks.isNotBlank() && !showRemarksInput) {
                Text(
                    text = "Note: ${item.remarks}",
                    fontSize = 10.sp,
                    color = GovNavyPrimary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            AnimatedVisibility(visible = showRemarksInput) {
                OutlinedTextField(
                    value = item.remarks,
                    onValueChange = onRemarksChange,
                    placeholder = { Text("Reason for absence, late time, or medical remark...", fontSize = 11.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    label: String,
    fullLabel: String,
    isSelected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .testTag("pill_${label}"),
        color = if (isSelected) activeBg else Color(0xFFF1F5F9),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) activeColor else Color(0xFFE2E8F0)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) activeColor else GovSlateMuted
            )
        }
    }
}

@Composable
private fun SaveAttendanceActionBar(
    state: TeacherAttendanceUiState,
    onReview: () -> Unit,
    onSave: () -> Unit,
    onOpenWhatsAppPush: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Commit Roll Call Register",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    Text(
                        text = "Atomic write to local Room DB & durable outbox queue",
                        fontSize = 10.sp,
                        color = GovSlateMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Review entries button
                    OutlinedButton(
                        onClick = onReview,
                        enabled = !state.isSaving && state.isAuthorizedToMark && state.roster.isNotEmpty(),
                        modifier = Modifier.testTag("btn_review_attendance")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Review", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (FeatureAvailability.isWhatsAppEnabled) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenWhatsAppPush() }
                                .testTag("btn_save_bar_whatsapp_push"),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "WhatsApp",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    GlassButton(
                        text = if (state.isSaving) "Saving..." else "Save Attendance",
                        onClick = onSave,
                        enabled = !state.isSaving && state.isAuthorizedToMark && state.roster.isNotEmpty(),
                        color = GovGreen,
                        leadingIcon = {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                            }
                        },
                        modifier = Modifier.testTag("btn_save_attendance")
                    )
                }
            }
        }
    }
}

@Composable
fun AttendanceReviewDialog(
    state: TeacherAttendanceUiState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val summary = state.summary
    val absentStudents = state.roster.filter { it.status == "ABSENT" }
    val lateStudents = state.roster.filter { it.status == "LATE" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Review Attendance Entries",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = GovNavyDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Class ${state.selectedClass} • ${state.selectedDate} • ${state.selectedSession}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GovSlateDark
                )

                state.duplicateWarning?.let { dup ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF3C7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = dup, fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Summary Counters
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${summary.presentCount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GovGreen)
                            Text(text = "Present", fontSize = 10.sp, color = GovSlateMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${summary.absentCount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GovRedAlert)
                            Text(text = "Absent", fontSize = 10.sp, color = GovSlateMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${summary.lateCount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GovSaffron)
                            Text(text = "Late", fontSize = 10.sp, color = GovSlateMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${summary.leaveCount}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF7C3AED))
                            Text(text = "Leave", fontSize = 10.sp, color = GovSlateMuted)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${summary.attendancePercentage}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = GovNavyPrimary)
                            Text(text = "Turnout", fontSize = 10.sp, color = GovSlateMuted)
                        }
                    }
                }

                // Callout for Absent Students
                if (absentStudents.isNotEmpty()) {
                    Text(
                        text = "Marked Absent (${absentStudents.size}):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovRedAlert
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        absentStudents.forEach { st ->
                            Text(
                                text = "• Roll #${st.rollNumber} - ${st.studentName}${if (st.remarks.isNotBlank()) " (${st.remarks})" else ""}",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }
                }

                // Callout for Late Students
                if (lateStudents.isNotEmpty()) {
                    Text(
                        text = "Marked Late (${lateStudents.size}):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSaffron
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        lateStudents.forEach { st ->
                            Text(
                                text = "• Roll #${st.rollNumber} - ${st.studentName}${if (st.remarks.isNotBlank()) " (${st.remarks})" else ""}",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                Text(
                    text = "Saving commits these records atomically to SQLite and enqueues a durable outbox event for cloud synchronization.",
                    fontSize = 10.5.sp,
                    color = GovSlateMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                modifier = Modifier.testTag("btn_review_confirm_save")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm & Save Attendance", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_review_dismiss")
            ) {
                Text("Edit Entries", fontSize = 11.5.sp)
            }
        }
    )
}

