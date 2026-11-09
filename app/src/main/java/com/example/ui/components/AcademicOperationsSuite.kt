package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.HomeworkSubmissionEntity
import com.example.data.local.entities.TimetableSlotEntity
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
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// =============================================================================
// 1. DYNAMIC TIMETABLE & PERIOD TRACKER
// =============================================================================

data class TimetablePeriodDisplay(
    val periodNumber: Int,
    val timeRange: String,
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int,
    val subject: String,
    val teacherName: String,
    val roomNumber: String,
    val isBreak: Boolean = false,
    val substitutionTeacher: String? = null
)

@Composable
fun DynamicTimetablePeriodTracker(
    classSection: String = "10-A",
    customSlots: List<TimetableSlotEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    val calendar = Calendar.getInstance()
    val todayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 0
    }

    var selectedDayIndex by remember { mutableIntStateOf(todayIndex) }
    val currentDayName = days[selectedDayIndex]

    // Current time tracker for live active period highlight
    var currentHour by remember { mutableIntStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var currentMinute by remember { mutableIntStateOf(calendar.get(Calendar.MINUTE)) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            currentHour = now.get(Calendar.HOUR_OF_DAY)
            currentMinute = now.get(Calendar.MINUTE)
            delay(30_000L) // Update every 30 seconds
        }
    }

    val baseSchedule = remember(selectedDayIndex) {
        listOf(
            TimetablePeriodDisplay(1, "08:30 - 09:15 AM", 8, 30, 9, 15, "Mathematics", "Mrs. Sunita Kapoor", "Room 304"),
            TimetablePeriodDisplay(2, "09:15 - 10:00 AM", 9, 15, 10, 0, "Physics & Science", "Dr. Meenakshi Sundaram", "Physics Lab A", substitutionTeacher = if (selectedDayIndex == 0 || selectedDayIndex == 3) "Mrs. Sunita Kapoor (Class Incharge)" else null),
            TimetablePeriodDisplay(3, "10:00 - 10:45 AM", 10, 0, 10, 45, "English Literature", "David K. Reynolds", "Room 304"),
            TimetablePeriodDisplay(0, "10:45 - 11:15 AM", 10, 45, 11, 15, "Morning Nutrition Recess", "Campus Staff", "Central Courtyard", isBreak = true),
            TimetablePeriodDisplay(4, "11:15 - 12:00 PM", 11, 15, 12, 0, "Chemistry & Practical", "Dr. Meenakshi Sundaram", "Chemistry Lab", substitutionTeacher = if (selectedDayIndex == 1) "Rajesh Verma" else null),
            TimetablePeriodDisplay(5, "12:00 - 12:45 PM", 12, 0, 12, 45, "Social Sciences & History", "Vikram Rathore", "Room 304"),
            TimetablePeriodDisplay(0, "12:45 - 01:30 PM", 12, 45, 13, 30, "Lunch & Midday Break", "Supervisory Staff", "Dining Hall", isBreak = true),
            TimetablePeriodDisplay(6, "01:30 - 02:15 PM", 13, 30, 14, 15, "Computer Science & AI", "Pooja Mehta", "Computer Lab 2"),
            TimetablePeriodDisplay(7, "02:15 - 03:00 PM", 14, 15, 15, 0, "Physical Education / Sports", "Coach Balwinder Singh", "Sports Pavilion")
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_timetable_tracker")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dynamic Class Timetable & Live Period Tracker",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernTextPrimary
                )
                Text(
                    text = "Class $classSection • CBSE Senior Academic Wing",
                    fontSize = 11.sp,
                    color = ModernTextMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ModernBlue50,
                border = BorderStroke(1.dp, ModernBlue200)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GovGreen)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "LIVE SYNC",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernBlue700
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Day of Week Selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(days.indices.toList()) { index ->
                val day = days[index]
                val isSelected = selectedDayIndex == index
                val isToday = todayIndex == index

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) ModernBlue700 else if (isToday) Color(0xFFEFF6FF) else PureWhite,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) ModernBlue800 else if (isToday) ModernBlue600 else ModernCardBorder
                    ),
                    modifier = Modifier
                        .clickable { selectedDayIndex = index }
                        .testTag("day_tab_$day")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.take(3).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) PureWhite else if (isToday) ModernBlue700 else ModernTextPrimary
                        )
                        if (isToday) {
                            Text(
                                text = "Today",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF93C5FD) else GovGreen
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Period Slots List
        baseSchedule.forEach { slot ->
            val slotStartTimeMin = slot.startHour * 60 + slot.startMinute
            val slotEndTimeMin = slot.endHour * 60 + slot.endMinute
            val currentTimeMin = currentHour * 60 + currentMinute
            val isActivePeriod = (selectedDayIndex == todayIndex) && (currentTimeMin in slotStartTimeMin until slotEndTimeMin)
            val isPastPeriod = (selectedDayIndex == todayIndex) && (currentTimeMin >= slotEndTimeMin)

            PeriodSlotCard(
                slot = slot,
                isActive = isActivePeriod,
                isPast = isPastPeriod
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PeriodSlotCard(
    slot: TimetablePeriodDisplay,
    isActive: Boolean,
    isPast: Boolean
) {
    val cardBackground = when {
        isActive -> Color(0xFFEFF6FF)
        slot.isBreak -> Color(0xFFF8FAFC)
        isPast -> Color(0xFFF1F5F9).copy(alpha = 0.6f)
        else -> PureWhite
    }

    val borderColor = when {
        isActive -> ModernBlue600
        slot.substitutionTeacher != null -> GovSaffron
        else -> ModernCardBorder
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        border = BorderStroke(if (isActive) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(if (isActive) "active_period_slot" else "period_slot_${slot.periodNumber}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (slot.isBreak) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "RECESS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isActive) ModernBlue700 else ModernBlue100
                        ) {
                            Text(
                                text = "PERIOD ${slot.periodNumber}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isActive) PureWhite else ModernBlue800,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = slot.timeRange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) ModernBlue700 else ModernTextMuted
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GovGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(PureWhite)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CURRENT ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                } else if (slot.substitutionTeacher != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(0.8.dp, GovSaffron)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Substitution",
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "SUBSTITUTION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slot.subject,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPast) GovSlateMuted else ModernTextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = slot.teacherName,
                            fontSize = 11.sp,
                            color = ModernTextMuted
                        )
                        if (slot.substitutionTeacher != null) {
                            Text(
                                text = " → Substituted by ${slot.substitutionTeacher}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                ) {
                    Text(
                        text = slot.roomNumber,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// 2. QR CODE & GEOFENCED ATTENDANCE SUITE
// =============================================================================

data class ScannedStudentAttendance(
    val studentId: String,
    val studentName: String,
    val admissionNumber: String,
    val classSection: String,
    val scanTimestamp: String,
    val direction: String = "ENTRY" // ENTRY or EXIT
)

@Composable
fun QrAndGeofencedAttendanceSystem(
    facultyStaffName: String = "Mrs. Sunita Kapoor",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableIntStateOf(0) } // 0: Student QR Scanner, 1: Faculty Geofenced
    var isScanningActive by remember { mutableStateOf(false) }
    var scanDirection by remember { mutableStateOf("ENTRY") }

    val scannedScholars = remember {
        mutableStateListOf(
            ScannedStudentAttendance("ADM-2026-001", "Aarav Sharma", "ADM-2026-001", "10-A", "08:14 AM", "ENTRY"),
            ScannedStudentAttendance("ADM-2026-002", "Ananya Iyer", "ADM-2026-002", "10-A", "08:17 AM", "ENTRY"),
            ScannedStudentAttendance("ADM-2026-003", "Rohan Malhotra", "ADM-2026-003", "10-A", "08:21 AM", "ENTRY")
        )
    }

    // Geofenced Campus Coordinates (St. Xavier's Main Campus)
    val campusLat = 19.0968
    val campusLng = 74.7478
    val allowedRadiusMeters = 250.0

    // Faculty simulated device GPS
    var facultyDistanceMeters by remember { mutableStateOf(48.0) }
    var isFacultyGeofenceVerified by remember { mutableStateOf(false) }
    var isGeofenceSubmitting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qr_geofenced_attendance_system")
    ) {
        // Tab Selector: QR Student Entry vs Faculty Geofenced Validation
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = PureWhite,
            border = BorderStroke(1.dp, ModernCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            TabRow(
                selectedTabIndex = selectedMode,
                containerColor = PureWhite,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMode]),
                        color = ModernBlue700,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedMode == 0,
                    onClick = { selectedMode = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Student QR Scanner", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_qr_student_scanner")
                )
                Tab(
                    selected = selectedMode == 1,
                    onClick = { selectedMode = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Faculty Geofenced", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    modifier = Modifier.testTag("tab_faculty_geofenced")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedMode == 0) {
            // Student QR Code Camera Scanner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.2.dp, ModernCardBorder),
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
                                text = "Camera QR Attendance Terminal",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = "Fast camera scan of student digital ID cards at school gate",
                                fontSize = 11.sp,
                                color = ModernTextMuted
                            )
                        }

                        // Direction Toggle (ENTRY / EXIT)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (scanDirection == "ENTRY") Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (scanDirection == "ENTRY") GovGreen else GovRedAlert),
                            modifier = Modifier.clickable {
                                scanDirection = if (scanDirection == "ENTRY") "EXIT" else "ENTRY"
                            }
                        ) {
                            Text(
                                text = scanDirection,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (scanDirection == "ENTRY") GovGreen else GovRedAlert,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scanner Viewfinder Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = "Scanner Reticle",
                                    tint = if (isScanningActive) GovGreen else PureWhite,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isScanningActive) "Position Scholar QR Barcode in Reticle" else "Camera Reader Ready",
                                    color = PureWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Auto-focus & High-Speed Barcode Decode",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Quick-Scan Actions (to test easily on emulator/device)
                    Text(
                        text = "Simulate Instant Digital ID QR Detection:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GovSlateDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                scannedScholars.add(0, ScannedStudentAttendance("ADM-2026-001", "Aarav Sharma", "ADM-2026-001", "10-A", timeNow, scanDirection))
                                Toast.makeText(context, "Aarav Sharma ($scanDirection) recorded at $timeNow", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("scan_student_1")
                        ) {
                            Text("Scan Aarav", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                val timeNow = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                                scannedScholars.add(0, ScannedStudentAttendance("ADM-2026-004", "Kabir Patel", "ADM-2026-004", "10-A", timeNow, scanDirection))
                                Toast.makeText(context, "Kabir Patel ($scanDirection) recorded at $timeNow", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ModernBlue600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("scan_student_4")
                        ) {
                            Text("Scan Kabir", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Recent Scanned Log
                    Text(
                        text = "Today's Verified Gate Log (${scannedScholars.size} records):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        scannedScholars.take(4).forEach { scholar ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(0.8.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = if (scholar.direction == "ENTRY") GovGreen else GovSaffron,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = scholar.studentName,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ModernTextPrimary
                                            )
                                            Text(
                                                text = "Roll #${scholar.admissionNumber.takeLast(2)} • Class ${scholar.classSection}",
                                                fontSize = 9.5.sp,
                                                color = ModernTextMuted
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (scholar.direction == "ENTRY") Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = scholar.direction,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (scholar.direction == "ENTRY") GovGreen else Color(0xFFB45309),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = scholar.scanTimestamp,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GovSlateDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Faculty Geofenced Automated Attendance Validation
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = PureWhite,
                border = BorderStroke(1.2.dp, ModernCardBorder),
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
                                text = "Faculty GPS Geofenced Punch-In",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = "Automated boundary check against official campus perimeter",
                                fontSize = 11.sp,
                                color = ModernTextMuted
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "GPS Geofence",
                            tint = if (facultyDistanceMeters <= allowedRadiusMeters) GovGreen else GovRedAlert,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Geofence Status Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (facultyDistanceMeters <= allowedRadiusMeters) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = BorderStroke(
                            1.dp,
                            if (facultyDistanceMeters <= allowedRadiusMeters) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (facultyDistanceMeters <= allowedRadiusMeters) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (facultyDistanceMeters <= allowedRadiusMeters) GovGreen else GovRedAlert,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (facultyDistanceMeters <= allowedRadiusMeters) "GEOFENCE IN-BOUND: VERIFIED" else "OUT-OF-BOUNDS (Outside Perimeter)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (facultyDistanceMeters <= allowedRadiusMeters) GovGreen else GovRedAlert
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Campus Center: $campusLat° N, $campusLng° E (Radius: ${allowedRadiusMeters.toInt()}m)",
                                fontSize = 10.5.sp,
                                color = GovSlateDark,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Your Current Distance: ${facultyDistanceMeters.toInt()} meters from campus main administrative hall.",
                                fontSize = 10.5.sp,
                                color = if (facultyDistanceMeters <= allowedRadiusMeters) GovGreen else GovRedAlert,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isFacultyGeofenceVerified) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, GovGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Faculty Attendance Confirmed",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovGreen
                                )
                                Text(
                                    text = "$facultyStaffName • Signed in at ${SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())} via GPS Geofence",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                isGeofenceSubmitting = true
                                isFacultyGeofenceVerified = true
                                Toast.makeText(context, "Attendance marked successfully within campus boundary!", Toast.LENGTH_LONG).show()
                            },
                            enabled = facultyDistanceMeters <= allowedRadiusMeters,
                            colors = ButtonDefaults.buttonColors(containerColor = GovGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_mark_geofenced_attendance")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark Geofenced Faculty Attendance", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// 3. ASSIGNMENT & HOMEWORK MANAGEMENT WITH COUNTDOWN & ATTACHMENT
// =============================================================================

data class StudentHomeworkSubmissionItem(
    val homeworkId: String,
    val subject: String,
    val title: String,
    val description: String,
    val teacherName: String,
    val dueDateIso: String,
    val maxMarks: Int,
    var status: String = "PENDING_SUBMISSION", // PENDING_SUBMISSION, SUBMITTED, EVALUATED
    var attachedFileName: String? = null,
    var attachedFileSizeMb: Float = 0f,
    var marksObtained: Int? = null,
    var teacherFeedback: String? = null,
    var submissionNotes: String = "",
    var isLate: Boolean = false
)

@Composable
fun HomeworkSubmissionPortal(
    studentName: String = "Aarav Sharma",
    studentId: String = "ADM-2026-001",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedHomeworkToSubmit by remember { mutableStateOf<StudentHomeworkSubmissionItem?>(null) }
    var submissionText by remember { mutableStateOf("") }
    var selectedAttachmentName by remember { mutableStateOf<String?>(null) }

    val assignments = remember {
        mutableStateListOf(
            StudentHomeworkSubmissionItem(
                homeworkId = "HW-10A-MATH-001",
                subject = "Mathematics",
                title = "Quadratic Equations & Discriminants",
                description = "Complete exercise 4.2 questions 1 through 10. Solve for real roots and derive vertex coordinates.",
                teacherName = "Mrs. Sunita Kapoor",
                dueDateIso = "2026-10-03T18:00:00Z",
                maxMarks = 20,
                status = "EVALUATED",
                attachedFileName = "Aarav_Math_Quadratics.pdf",
                attachedFileSizeMb = 1.4f,
                marksObtained = 19,
                teacherFeedback = "Flawless algebra derivations and tidy graph sketching. Well done!"
            ),
            StudentHomeworkSubmissionItem(
                homeworkId = "HW-10A-PHY-001",
                subject = "Physics & Science",
                title = "Electromagnetic Induction & Faraday's Laws",
                description = "Write a 500-word laboratory synthesis on Lenz's law with labelled circuit diagrams.",
                teacherName = "Dr. Meenakshi Sundaram",
                dueDateIso = "2026-10-04T23:59:00Z",
                maxMarks = 25,
                status = "PENDING_SUBMISSION"
            ),
            StudentHomeworkSubmissionItem(
                homeworkId = "HW-10A-ENG-001",
                subject = "English Literature",
                title = "The Merchant of Venice - Act 3 Analysis",
                description = "Write a character commentary on Portia's courtroom discourse on the quality of mercy.",
                teacherName = "David K. Reynolds",
                dueDateIso = "2026-10-01T12:00:00Z", // Past due to demonstrate late submission flagging
                maxMarks = 20,
                status = "PENDING_SUBMISSION"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("homework_submission_portal")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Digital Homework & Assignment Portal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernTextPrimary
                )
                Text(
                    text = "Active assignments, deadline timers & digital submissions",
                    fontSize = 11.sp,
                    color = ModernTextMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ModernBlue50,
                border = BorderStroke(1.dp, ModernBlue200)
            ) {
                Text(
                    text = "${assignments.count { it.status == "PENDING_SUBMISSION" }} PENDING",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernBlue700,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Assignment List
        assignments.forEach { item ->
            // Calculate deadline status and countdown
            val isDuePast = item.dueDateIso.contains("2026-10-01") // Mock deadline check
            val countdownText = if (isDuePast) "DEADLINE EXPIRED (LATE)" else "Due in 28 hrs"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = BorderStroke(
                    1.dp,
                    when (item.status) {
                        "EVALUATED" -> GovGreen
                        "SUBMITTED" -> ModernBlue600
                        else -> if (isDuePast) GovRedAlert else ModernCardBorder
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ModernBlue100
                        ) {
                            Text(
                                text = item.subject.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernBlue800,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        // Countdown / Status badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (item.status) {
                                "EVALUATED" -> Color(0xFFDCFCE7)
                                "SUBMITTED" -> Color(0xFFDBEAFE)
                                else -> if (isDuePast) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = when (item.status) {
                                        "EVALUATED" -> GovGreen
                                        "SUBMITTED" -> ModernBlue700
                                        else -> if (isDuePast) GovRedAlert else Color(0xFFB45309)
                                    },
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = when (item.status) {
                                        "EVALUATED" -> "GRADE: ${item.marksObtained}/${item.maxMarks}"
                                        "SUBMITTED" -> "SUBMITTED"
                                        else -> countdownText
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (item.status) {
                                        "EVALUATED" -> GovGreen
                                        "SUBMITTED" -> ModernBlue700
                                        else -> if (isDuePast) GovRedAlert else Color(0xFFB45309)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = GovSlateDark,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assigned by ${item.teacherName} • Max ${item.maxMarks} Marks",
                            fontSize = 10.sp,
                            color = ModernTextMuted
                        )

                        if (item.status == "PENDING_SUBMISSION") {
                            Button(
                                onClick = {
                                    selectedHomeworkToSubmit = item
                                    submissionText = ""
                                    selectedAttachmentName = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDuePast) Color(0xFFD97706) else ModernBlue700
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("submit_btn_${item.homeworkId}")
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isDuePast) "Submit Late" else "Submit", fontSize = 11.sp)
                            }
                        } else if (item.status == "EVALUATED") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(0.8.dp, Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = "Feedback: \"${item.teacherFeedback ?: "Good work"}\"",
                                    fontSize = 10.sp,
                                    color = GovSlateDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Modal Submission Dialog
    selectedHomeworkToSubmit?.let { hw ->
        val isLate = hw.dueDateIso.contains("2026-10-01")

        AlertDialog(
            onDismissRequest = { selectedHomeworkToSubmit = null },
            title = {
                Column {
                    Text(
                        text = "Submit Solution: ${hw.title}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                    if (isLate) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(0.8.dp, GovRedAlert),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "LATE SUBMISSION FLAGGED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovRedAlert,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter notes or key solution derivation steps:",
                        fontSize = 11.5.sp,
                        color = ModernTextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = submissionText,
                        onValueChange = { submissionText = it },
                        placeholder = { Text("E.g. Attached complete problem solution for Q1-10 with graphs.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("input_submission_notes")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Attachment Selector (PDF / Images)
                    Text(
                        text = "File Attachment (PDF / JPG / PNG):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (selectedAttachmentName == null) {
                        OutlinedButton(
                            onClick = {
                                selectedAttachmentName = "${studentName.replace(" ", "_")}_${hw.subject}_Solution.pdf"
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("attach_file_btn")
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Attach Solution Document (PDF)", fontSize = 11.5.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, ModernBlue200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = ModernBlue700, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = selectedAttachmentName ?: "Solution.pdf",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ModernBlue800
                                        )
                                        Text(
                                            text = "1.8 MB • Portable Document Format",
                                            fontSize = 9.sp,
                                            color = ModernTextMuted
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedAttachmentName = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = GovRedAlert, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val index = assignments.indexOf(hw)
                        if (index != -1) {
                            assignments[index] = hw.copy(
                                status = "SUBMITTED",
                                attachedFileName = selectedAttachmentName ?: "Solution_Note.txt",
                                attachedFileSizeMb = 1.8f,
                                submissionNotes = submissionText,
                                isLate = isLate
                            )
                        }
                        Toast.makeText(context, "Assignment submitted successfully!", Toast.LENGTH_SHORT).show()
                        selectedHomeworkToSubmit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirm Submit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedHomeworkToSubmit = null }) {
                    Text("Cancel", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
