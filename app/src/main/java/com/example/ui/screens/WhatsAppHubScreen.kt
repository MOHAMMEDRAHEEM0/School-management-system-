package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AttendancePushCategory
import com.example.ui.components.AttendanceWhatsAppGenerator
import com.example.ui.components.IndividualStudentWhatsAppNoticeDialog
import com.example.ui.components.ModernBlueCard
import com.example.ui.components.StudentAttendancePushInfo
import com.example.ui.components.WhatsAppBrandGreen
import com.example.ui.components.WhatsAppBubbleBg
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.ModernBlue50
import com.example.ui.theme.ModernBlue600
import com.example.ui.theme.ModernBlue700
import com.example.ui.theme.ModernCardBorder
import com.example.ui.theme.ModernSurfaceSubtle
import com.example.ui.theme.ModernTextMuted
import com.example.ui.theme.ModernTextPrimary
import com.example.ui.theme.ModernTextSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel
import com.example.ui.viewmodel.StudentAttendanceItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Institutional WhatsApp Hub & Attendance Push System.
 * Merged from the blue interface version with full WhatsApp push capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppHubScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    val students by viewModel.students.collectAsState()
    val allClasses by viewModel.allClasses.collectAsState()
    val attendanceRecords by viewModel.attendanceRecords.collectAsState()

    var selectedClass by remember { mutableStateOf("10-A") }
    val todayDateFormatted = remember {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
    }

    // Convert students of selectedClass to StudentAttendanceItem roster
    val classStudents = remember(students, selectedClass) {
        val grade = selectedClass.substringBefore("-")
        val section = selectedClass.substringAfter("-", "A")
        students.filter { it.grade == grade && it.section == section }
            .ifEmpty { students.take(15) }
    }

    val roster = remember(classStudents, attendanceRecords) {
        classStudents.mapIndexed { idx, stu ->
            val existing = attendanceRecords.firstOrNull { it.personId == stu.id }
            val status = existing?.status ?: if (idx % 7 == 2) "ABSENT" else "PRESENT"
            StudentAttendanceItem(
                studentId = stu.id,
                studentName = stu.name,
                rollNumber = stu.rollNumber,
                admissionNumber = stu.admissionNumber,
                status = status,
                remarks = existing?.remarks ?: "",
                studentPhone = stu.studentPhone,
                guardianPhone = stu.guardianPhone
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .testTag("whatsapp_hub_screen")
    ) {
        // Header
        Surface(
            color = PureWhite,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, ModernCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = "WhatsApp Hub",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WhatsApp Hub & Push Directorate",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = "Automated Student & Guardian Notices • Direct Deep-Linking",
                                fontSize = 11.sp,
                                color = ModernTextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Text(
                            text = "DISPATCH ONLINE",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PureWhite,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF16A34A),
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Attendance Push Roster",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color(0xFF15803D) else ModernTextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Emergency & Broadcast Notices",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color(0xFF15803D) else ModernTextSecondary
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AttendancePushRosterTab(
                    roster = roster,
                    selectedClass = selectedClass,
                    onClassSelected = { selectedClass = it },
                    classesList = if (allClasses.isNotEmpty()) allClasses.map { "${it.className}-${it.section}" } else listOf("10-A", "10-B", "9-A", "9-B"),
                    todayDate = todayDateFormatted
                )
                1 -> ParentBroadcastNoticeTab()
            }
        }
    }
}

@Composable
private fun AttendancePushRosterTab(
    roster: List<StudentAttendanceItem>,
    selectedClass: String,
    onClassSelected: (String) -> Unit,
    classesList: List<String>,
    todayDate: String
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPreviewInfo by remember { mutableStateOf<StudentAttendancePushInfo?>(null) }
    var isBatchDispatching by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableIntStateOf(0) }
    val dispatchedStatusMap = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Class Selector Chip Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Class Roster:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = ModernTextMuted
            )
            classesList.forEach { cls ->
                val isSelected = cls == selectedClass
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ModernBlue600 else ModernSurfaceSubtle,
                    border = BorderStroke(1.dp, if (isSelected) ModernBlue600 else ModernCardBorder),
                    modifier = Modifier.clickable { onClassSelected(cls) }
                ) {
                    Text(
                        text = "Class $cls",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PureWhite else ModernTextPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Batch Dispatch Action Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
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
                        text = "Class $selectedClass Attendance Notice Batch",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        text = "${roster.size} Scholars • ${roster.count { it.status == "PRESENT" }} Present, ${roster.count { it.status == "ABSENT" }} Absent",
                        fontSize = 10.5.sp,
                        color = Color(0xFF166534)
                    )
                }

                Button(
                    onClick = {
                        isBatchDispatching = true
                        batchProgress = 0
                        coroutineScope.launch {
                            roster.forEachIndexed { i, item ->
                                delay(120)
                                dispatchedStatusMap[item.studentId] = true
                                batchProgress = i + 1
                            }
                            isBatchDispatching = false
                            Toast.makeText(context, "Successfully dispatched attendance notices to Class $selectedClass!", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isBatchDispatching,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBrandGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Batch Send",
                        tint = PureWhite,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBatchDispatching) "Sending ($batchProgress/${roster.size})" else "Dispatch All",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureWhite
                    )
                }
            }
        }

        if (isBatchDispatching) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { batchProgress.toFloat() / roster.size.coerceAtLeast(1) },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = WhatsAppBrandGreen
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Student Roster
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(roster) { item ->
                val pushInfo = remember(item, selectedClass, todayDate) {
                    AttendanceWhatsAppGenerator.buildPushInfo(
                        item = item,
                        className = selectedClass,
                        dateFormatted = todayDate,
                        currentSession = "Morning Roll Call"
                    )
                }
                val isDispatched = dispatchedStatusMap[item.studentId] == true

                ModernBlueCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.status) {
                                            "PRESENT" -> Color(0xFFDCFCE7)
                                            "ABSENT" -> Color(0xFFFEE2E2)
                                            else -> Color(0xFFFEF3C7)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${item.rollNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.status) {
                                        "PRESENT" -> Color(0xFF16A34A)
                                        "ABSENT" -> Color(0xFFDC2626)
                                        else -> Color(0xFFD97706)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = item.studentName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ModernTextPrimary
                                )
                                Text(
                                    text = "Mobile: ${pushInfo.linkedMobile} • ${pushInfo.statusCategory.title}",
                                    fontSize = 10.sp,
                                    color = ModernTextMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDispatched) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Sent",
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Sent",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { selectedPreviewInfo = pushInfo },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, WhatsAppBrandGreen),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Preview",
                                        tint = WhatsAppBrandGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Send",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPreviewInfo?.let { info ->
        IndividualStudentWhatsAppNoticeDialog(
            info = info,
            onDismiss = { selectedPreviewInfo = null },
            onSendWhatsApp = {
                dispatchedStatusMap[info.studentId] = true
                selectedPreviewInfo = null
                Toast.makeText(context, "Dispatched WhatsApp message to ${info.studentName}!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun ParentBroadcastNoticeTab() {
    val context = LocalContext.current
    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var targetGroup by remember { mutableStateOf("ALL_PARENTS") }
    var isSending by remember { mutableStateOf(false) }

    val quickTemplates = listOf(
        "Severe Rain Alert: School Holiday Declared for Tomorrow" to "Weather Alert",
        "Quarterly Parent-Teacher Meeting (PTM) Scheduled for Saturday 9:00 AM" to "PTM Meeting",
        "Annual Sports Day Practice Timetable Update - Pick-up at 4:30 PM" to "Sports Update",
        "Reminder: Term 2 Fee Payment Due Date Approaching" to "Fee Notice"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ModernBlueCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Quick Announcement Templates:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernTextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))
                quickTemplates.forEach { (msg, title) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ModernSurfaceSubtle,
                        border = BorderStroke(1.dp, ModernCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                broadcastTitle = title
                                broadcastMessage = msg
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = ModernBlue600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                fontSize = 11.5.sp,
                                color = ModernTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = broadcastTitle,
            onValueChange = { broadcastTitle = it },
            label = { Text("Broadcast Subject / Title") },
            placeholder = { Text("e.g. Weather Alert, Bus Delay, Notice") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = broadcastMessage,
            onValueChange = { broadcastMessage = it },
            label = { Text("WhatsApp Message Text") },
            placeholder = { Text("Enter formal circular or notice to send to all linked parent WhatsApp numbers...") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (broadcastMessage.isBlank()) {
                    Toast.makeText(context, "Please enter message text", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                Toast.makeText(context, "Broadcasting WhatsApp notice to 420 linked parent numbers...", Toast.LENGTH_LONG).show()
                broadcastTitle = ""
                broadcastMessage = ""
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBrandGreen),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Broadcast Notice to Linked WhatsApp Numbers", fontWeight = FontWeight.Bold, color = PureWhite)
        }
    }
}
