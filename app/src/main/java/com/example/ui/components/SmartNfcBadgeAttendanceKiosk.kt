package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.PureWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GatePunchRecord(
    val punchId: String,
    val studentName: String,
    val admissionNo: String,
    val rollNo: Int,
    val timeFormatted: String,
    val terminalName: String = "GATE-EAST-KIOSK #01",
    val status: String = "PRESENT_VERIFIED"
)

/**
 * Smart NFC / RFID Digital Badge Attendance Kiosk Terminal.
 * Provides rapid tap-to-punch gate logging with visual antenna waveform,
 * real-time timestamping, and local Outbox sync.
 */
@Composable
fun SmartNfcBadgeAttendanceKiosk(
    students: List<StudentEntity>,
    onLogPunch: (StudentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var lastPunchedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    val punchFeed = remember {
        mutableStateListOf(
            GatePunchRecord(
                punchId = "PUNCH-101",
                studentName = "Aarav Sharma",
                admissionNo = "ADM-2026-001",
                rollNo = 1,
                timeFormatted = "07:58:22 AM"
            )
        )
    }

    // Infinite radar pulse for NFC terminal
    val infiniteTransition = rememberInfiniteTransition(label = "NfcPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "NfcScale"
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // NFC Terminal Scanner Deck
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.2.dp, GovNavyPrimary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                .testTag("nfc_kiosk_terminal"),
            color = PureWhite,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(GovGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TERMINAL ONLINE • GATE EAST #01",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovGreen,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GovNavyPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "NFC / RFID READY",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pulsing NFC Antenna Target Circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    GovNavyPrimary.copy(alpha = 0.15f),
                                    GovNavyPrimary.copy(alpha = 0.04f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, GovNavyPrimary.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(GovNavyPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "NFC Sensor",
                            tint = PureWhite,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tap Student ID Card to Terminal",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = GovNavyDark
                )
                Text(
                    text = "Supports 13.56 MHz RFID / Digital Mobile Wallet Badges",
                    fontSize = 11.sp,
                    color = GovSlateMuted
                )

                // Last Punched Banner
                AnimatedVisibility(
                    visible = lastPunchedStudent != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    lastPunchedStudent?.let { stu ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = GovGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "VERIFIED & LOGGED: ${stu.name}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovGreen
                                    )
                                    Text(
                                        text = "Admission: ${stu.admissionNumber} • Roll #${stu.rollNumber} • Marked Present",
                                        fontSize = 10.sp,
                                        color = GovSlateDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tap Student Digital Badges Panel
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerTitle = "Class 10-A Scholar Badges (Tap to Punch)",
            headerBadge = "${students.size} REGISTERED",
            headerRibbonColor = GovNavyPrimary
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Click any student's digital NFC badge to simulate an instant morning gate scan:",
                    fontSize = 11.5.sp,
                    color = GovSlateDark
                )

                val displayStudents = if (students.isNotEmpty()) students else listOf(
                    StudentEntity(
                        id = "ADM-2026-001",
                        name = "Aarav Sharma",
                        grade = "10",
                        section = "A",
                        rollNumber = 1
                    ),
                    StudentEntity(
                        id = "ADM-2026-002",
                        name = "Ananya Iyer",
                        grade = "10",
                        section = "A",
                        rollNumber = 2
                    )
                )

                displayStudents.forEach { stu ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                            .clickable {
                                val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                                val currentTime = timeFormat.format(Date())

                                lastPunchedStudent = stu
                                punchFeed.add(
                                    0,
                                    GatePunchRecord(
                                        punchId = "PUNCH-${System.currentTimeMillis() % 10000}",
                                        studentName = stu.name,
                                        admissionNo = stu.admissionNumber,
                                        rollNo = stu.rollNumber,
                                        timeFormatted = currentTime
                                    )
                                )
                                onLogPunch(stu)
                                Toast.makeText(context, "Beep! ${stu.name} checked in at $currentTime", Toast.LENGTH_SHORT).show()
                            },
                        color = Color(0xFFF8FAFC)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(GovNavyPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${stu.rollNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = stu.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovNavyDark
                                    )
                                    Text(
                                        text = "ID: ${stu.admissionNumber} • Section ${stu.grade}-${stu.section}",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GovNavyPrimary
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Nfc,
                                        contentDescription = "Tap",
                                        tint = PureWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap NFC",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Gate Punch Stream
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerTitle = "Live Assembly Gate Punch Stream",
            headerBadge = "${punchFeed.size} LOGGED TODAY",
            headerRibbonColor = GovGreen
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (punchFeed.isEmpty()) {
                    Text("No gate punch records yet today.", fontSize = 11.sp, color = GovSlateMuted)
                } else {
                    punchFeed.take(6).forEach { punch ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = punch.studentName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovNavyDark
                                )
                                Text(
                                    text = "Roll #${punch.rollNo} • ${punch.admissionNo} • ${punch.terminalName}",
                                    fontSize = 9.5.sp,
                                    color = GovSlateMuted
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = punch.timeFormatted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = GovGreen
                                )
                                Text(
                                    text = "Present",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
