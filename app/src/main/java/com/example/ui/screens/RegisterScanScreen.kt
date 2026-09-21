package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RegisterScanScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by viewModel.students.collectAsState()

    // 10-A scholars
    val classStudents = students.filter { it.section == "A" }

    var isScanning by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(true) } // Pre-scanned preview ready for teacher
    var isSubmitting by remember { mutableStateOf(false) }

    // Roster map of Student ID to Status ("PRESENT", "ABSENT", "LATE")
    val attendanceMap = remember {
        mutableStateMapOf<String, String>().apply {
            classStudents.forEach { stu ->
                this[stu.id] = if (stu.rollNumber == 4) "ABSENT" else "PRESENT"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("register_scan_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GovNavyPrimary.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = GovNavyPrimary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "Physical Register OCR Scanner",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = GovNavyDark
                )
                Text(
                    text = "Class 10-A | Morning Assembly Session",
                    fontSize = 10.sp,
                    color = GovSlateMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simulated Optical Camera Viewfinder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, if (isScanning) GovGreen else GovNavyPrimary, RoundedCornerShape(12.dp))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isScanning) {
                    CircularProgressIndicator(color = GovGreen, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Analyzing physical attendance register sheet...",
                        fontSize = 11.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Recognizing handwritten marks & checkmarks",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "OCR Scanner",
                        tint = if (hasScanned) GovGreen else PureWhite,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (hasScanned) "Register Optical OCR Parsed Successfully" else "Align physical ledger within scanner frame",
                        fontSize = 12.sp,
                        color = PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "High-precision character recognition (99.8% institutional accuracy)",
                        fontSize = 9.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Top-right optical scan trigger
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PureWhite.copy(alpha = 0.15f))
                    .clickable {
                        coroutineScope.launch {
                            isScanning = true
                            delay(1000)
                            isScanning = false
                            hasScanned = true
                            Toast.makeText(context, "Register scan completed! 5 students parsed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Retake", fontSize = 9.5.sp, color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Parsed Attendance Summary Bar
        val presentCount = attendanceMap.values.count { it == "PRESENT" }
        val absentCount = attendanceMap.values.count { it == "ABSENT" }
        val lateCount = attendanceMap.values.count { it == "LATE" }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerRibbonColor = GovNavyPrimary,
            headerTitle = "PARSED ROSTER VERIFICATION (TAP CHIP TO FLIP STATUS)"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassBadge(text = "$presentCount PRESENT", color = GovGreen)
                    GlassBadge(text = "$absentCount ABSENT", color = GovRedAlert)
                    if (lateCount > 0) GlassBadge(text = "$lateCount LATE", color = GovSaffron)
                }

                Text(
                    text = "Total: ${classStudents.size}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovSlateDark
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Student Roster Grid with 1-Tap Toggle
        classStudents.forEach { student ->
            val status = attendanceMap[student.id] ?: "PRESENT"

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerRibbonColor = when (status) {
                    "PRESENT" -> GovGreen
                    "ABSENT" -> GovRedAlert
                    else -> GovSaffron
                },
                headerTitle = "ROLL #${student.rollNumber} - ${student.name.uppercase()}",
                headerBadge = status
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = student.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateDark
                        )
                        Text(
                            text = "Adm No: ${student.admissionNumber} | Guardian: ${student.guardianPhone}",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }

                    // Toggle Chips: P / A / L
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (status == "PRESENT") GovGreen else Color(0xFFE2E8F0))
                                .clickable { attendanceMap[student.id] = "PRESENT" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "P",
                                color = if (status == "PRESENT") PureWhite else GovSlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (status == "ABSENT") GovRedAlert else Color(0xFFE2E8F0))
                                .clickable { attendanceMap[student.id] = "ABSENT" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "A",
                                color = if (status == "ABSENT") PureWhite else GovSlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (status == "LATE") GovSaffron else Color(0xFFE2E8F0))
                                .clickable { attendanceMap[student.id] = "LATE" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "L",
                                color = if (status == "LATE") PureWhite else GovSlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Batch Submission Action
        GlassButton(
            text = if (isSubmitting) "Submitting to In-Charge..." else "Commit Batch & Transmit to Class In-Charge",
            onClick = {
                coroutineScope.launch {
                    isSubmitting = true
                    val batchId = "BATCH-OCR-10A-${System.currentTimeMillis() % 10000}"
                    val batch = RegisterScanBatchEntity(
                        batchId = batchId,
                        date = "2026-09-15",
                        classSection = "10-A",
                        session = "Morning Assembly Roll Call",
                        teacherName = "Dr. Meenakshi Sundaram",
                        totalStudents = classStudents.size,
                        presentCount = presentCount,
                        absentCount = absentCount,
                        absentRolls = if (absentCount > 0) "Roll #4 (Kabir Patel)" else "None",
                        inchargeVerified = false,
                        principalApproved = false,
                        status = "PENDING_INCHARGE_VERIFICATION"
                    )

                    val records = classStudents.map { stu ->
                        AttendanceRecordEntity(
                            personId = stu.id,
                            personName = stu.name,
                            roleType = "STUDENT",
                            date = "2026-09-15",
                            status = attendanceMap[stu.id] ?: "PRESENT",
                            timeMarked = "08:15 AM",
                            markedByRole = "TEACHER",
                            markedByName = "Dr. Meenakshi Sundaram",
                            classSection = "10-A",
                            sourceMethod = "CAMERA_REGISTER_OCR"
                        )
                    }

                    viewModel.commitRegisterScanBatch(batch, records)
                    delay(500)
                    isSubmitting = false
                    Toast.makeText(context, "Batch $batchId transmitted to In-Charge for cross-verification!", Toast.LENGTH_LONG).show()
                    onNavigateBack()
                }
            },
            enabled = !isSubmitting,
            color = GovNavyPrimary,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.Send, contentDescription = null, tint = PureWhite, modifier = Modifier.size(15.dp))
            },
            testTag = "commit_register_batch_btn"
        )
    }
}
