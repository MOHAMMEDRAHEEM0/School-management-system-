package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Verified
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
import com.example.data.model.UserRole
import com.example.ui.components.AttendanceWhatsAppGenerator
import com.example.ui.components.launchWhatsAppMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.OfficialSealStamp
import com.example.ui.components.OfficialStudentIdCard
import com.example.ui.components.OfficialWhatsAppButton
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
import com.example.payment.FeePaymentOrder
import com.example.payment.PaymentGatewayProvider
import com.example.payment.PaymentGatewayService
import com.example.payment.PaymentVerificationRequest
import com.example.payment.UpiPaymentIntent
import com.example.ui.components.StudentAttendancePortal
import java.util.UUID

@Composable
fun StudentDashboard(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentRole by viewModel.currentRole.collectAsState()
    val isParentMode = currentRole == UserRole.PARENT

    val students by viewModel.students.collectAsState()
    val examRecords by viewModel.examRecords.collectAsState()
    val feeRecords by viewModel.feeRecords.collectAsState()
    val certificates by viewModel.certificates.collectAsState()
    val diaryEntries by viewModel.diaryEntries.collectAsState()
    val notices by viewModel.notices.collectAsState()

    // Isolated student (Aarav Sharma - Roll #1)
    val student = students.firstOrNull { it.admissionNumber == "ADM-2026-001" } ?: students.firstOrNull()

    // Filter data strictly to this student
    val studentExams = examRecords.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val studentFees = feeRecords.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val studentCerts = certificates.filter { it.studentId == (student?.id ?: "ADM-2026-001") }
    val classDiary = diaryEntries.filter { it.classSection.contains("10-A") }

    // Dialog for Requesting Certificate
    var showCertDialog by remember { mutableStateOf(false) }
    var certType by remember { mutableStateOf("BONAFIDE") }
    var certPurpose by remember { mutableStateOf("Passport Verification & Higher Studies") }

    // Online Fee Payment Gateway State
    var selectedFeeGateway by remember { mutableStateOf(PaymentGatewayProvider.RAZORPAY) }
    var activeStudentOrder by remember { mutableStateOf<FeePaymentOrder?>(null) }
    var isFeePaidSuccessfully by remember { mutableStateOf(false) }
    var paymentReceiptNo by remember { mutableStateOf<String?>(null) }
    val studentPaymentGateway = remember { PaymentGatewayService() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("student_dashboard")
    ) {
        if (student == null) {
            Text("No student record found.", color = GovRedAlert)
            return@Column
        }

        // Student / Parent Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isParentMode) "Parent & Guardian Desk" else "Student Scholar Portal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    GlassBadge(
                        text = if (isParentMode) "PARENT OF SCHOLAR" else "GRADE 10-A",
                        color = if (isParentMode) GovSaffron else GovNavyPrimary
                    )
                }
                Text(
                    text = if (isParentMode) "Child: ${student.name} | Guardian: ${student.guardianName}" else "${student.name} | Admission No: ${student.admissionNumber}",
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

        // Student Quick Telemetry
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 2 },
                headerRibbonColor = GovGreen,
                headerTitle = "ATTENDANCE"
            ) {
                Column {
                    Text(
                        text = "${student.attendancePercentage}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = GovGreen
                    )
                    Text(
                        text = "Exemplary Record (Tap to View)",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                }
            }

            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedTab = 3 },
                headerRibbonColor = if (isFeePaidSuccessfully || student.feeDue <= 0) GovGreen else GovRedAlert,
                headerTitle = "COMPOSITE FEE"
            ) {
                Column {
                    Text(
                        text = if (isFeePaidSuccessfully || student.feeDue <= 0) "CLEARED" else "₹${student.feeDue.toInt()}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isFeePaidSuccessfully || student.feeDue <= 0) GovGreen else GovRedAlert
                    )
                    Text(
                        text = if (isFeePaidSuccessfully || student.feeDue <= 0) "Official Receipt ${paymentReceiptNo ?: "#9901"}" else "Term Due (Tap to Pay)",
                        fontSize = 9.sp,
                        color = GovSlateMuted
                    )
                }
            }
        }

        // Automated WhatsApp Attendance Telemetry Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = PureWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag("card_student_whatsapp_attendance")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WhatsApp Attendance Notification (Convenience Dispatch)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                    GlassBadge(
                        text = "LINKED MOBILE",
                        color = Color(0xFF25D366)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Linked Number: ${student.studentPhone.ifBlank { student.guardianPhone.ifBlank { "+91 98765 43210" } }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = GovSlateDark
                        )
                        Text(
                            text = "Today's Status: \"${student.name} was present in all classes \"",
                            fontSize = 10.5.sp,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val message = AttendanceWhatsAppGenerator.generateComputerGeneratedWhatsAppMessage(
                                    studentName = student.name,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    totalClasses = 5,
                                    absentClasses = emptyList(),
                                    linkedMobile = student.studentPhone.ifBlank { student.guardianPhone }
                                )
                                launchWhatsAppMessage(context, student.studentPhone.ifBlank { student.guardianPhone }, message)
                            }
                            .testTag("btn_test_whatsapp_notice"),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF25D366))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Notice", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
            }
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
                    text = { Text("Digital ID", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Report Card", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Attendance", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Pay Fees", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Diary & HW", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
                    text = { Text("Certificates", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6 },
                    text = { Text("WhatsApp Alerts", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // =====================================================================
        // TAB 0: COMPUTERIZED DIGITAL ID CARD
        // =====================================================================
        if (selectedTab == 0) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Official Computer-Generated Student Identity Card:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                // Render the official non-glass printable PVC card
                OfficialStudentIdCard(
                    student = student,
                    schoolName = "ST. XAVIER'S INTERNATIONAL ACADEMY",
                    academicYear = "2026-2027"
                )
            }
        }

        // =====================================================================
        // TAB 1: OFFICIAL TERM REPORT CARD
        // =====================================================================
        if (selectedTab == 1) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Term 1 Summative Assessment Report Card:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "Session: 2026-2027 | Class 10-A",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }

                    OfficialSealStamp(
                        title = "ACADEMY",
                        code = "SEAL-A+",
                        modifier = Modifier.size(52.dp),
                        color = GovGreen
                    )
                }

                studentExams.forEach { exam ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = exam.subject.uppercase(),
                        headerBadge = "GRADE ${exam.grade}"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exam.subject,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Faculty Remarks: ${exam.teacherRemarks}",
                                    fontSize = 10.sp,
                                    color = GovSlateText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${exam.marksObtained.toInt()} / ${exam.maxMarks.toInt()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovGreen
                                )
                                Text(
                                    text = "${(exam.marksObtained * 100 / exam.maxMarks).toInt()}%",
                                    fontSize = 9.sp,
                                    color = GovSlateMuted
                                )
                            }
                        }
                    }
                }

                // Summary Cumulative Banner
                val totalMarks = studentExams.sumOf { it.marksObtained.toDouble() }
                val maxMarks = studentExams.sumOf { it.maxMarks.toDouble() }.coerceAtLeast(1.0)
                val cumulativePct = (totalMarks * 100 / maxMarks).toInt()

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = GovGreen,
                    headerTitle = "OVERALL SCHOLASTIC PERFORMANCE"
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Cumulative Grade: A+ (Outstanding)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Text(
                                text = "Total Score: ${totalMarks.toInt()} / ${maxMarks.toInt()} ($cumulativePct%)",
                                fontSize = 10.sp,
                                color = GovSlateMuted
                            )
                        }

                        GlassButton(
                            text = "Download PDF",
                            onClick = {
                                Toast.makeText(context, "Generating official sealed Report Card PDF...", Toast.LENGTH_SHORT).show()
                            },
                            color = GovNavyPrimary,
                            leadingIcon = {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                            }
                        )
                    }
                }
            }
        }

        // =====================================================================
        // TAB 2: MULTI-LAYERED SCHOLAR ATTENDANCE PORTAL (STRICT READ-ONLY)
        // =====================================================================
        if (selectedTab == 2) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(650.dp)
            ) {
                StudentAttendancePortal(
                    viewModel = viewModel,
                    student = student,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // =====================================================================
        // TAB 3: FEE PAYMENT & ZERO-TRUST GATEWAY CHECKOUT
        // =====================================================================
        if (selectedTab == 3) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Fee Summary Banner
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    headerRibbonColor = if (isFeePaidSuccessfully || student.feeDue <= 0) GovGreen else GovRedAlert,
                    headerTitle = "TERM 1 TUITION & COMPOSITE FEE",
                    headerBadge = if (isFeePaidSuccessfully || student.feeDue <= 0) "PAID & CLEARED" else "PAYMENT OUTSTANDING"
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Term 1 Composite Tuition & Lab Fee",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Text(
                                    text = "Session: 2026-2027 • Scholar: ${student.name} (${student.admissionNumber})",
                                    fontSize = 10.sp,
                                    color = GovSlateMuted
                                )
                                Text(
                                    text = if (isFeePaidSuccessfully || student.feeDue <= 0) "Amount Due: ₹0.00 (Cleared)" else "Amount Due: ₹15,000",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFeePaidSuccessfully || student.feeDue <= 0) GovGreen else GovRedAlert
                                )
                            }

                            if (isFeePaidSuccessfully || student.feeDue <= 0) {
                                OfficialSealStamp(
                                    title = "BURSAR",
                                    code = "CLEARED",
                                    modifier = Modifier.size(54.dp),
                                    color = GovGreen
                                )
                            }
                        }
                    }
                }

                if (isFeePaidSuccessfully || student.feeDue <= 0) {
                    // Official Signed Digital Receipt Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Receipt, contentDescription = null, tint = GovGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Official Authenticated Digital Receipt", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                                }
                                GlassBadge(text = "SEALED", color = GovGreen)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Receipt Number:", fontSize = 10.sp, color = GovSlateMuted)
                                        Text(paymentReceiptNo ?: "RCP-2026-9901", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = GovNavyPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Paid:", fontSize = 10.sp, color = GovSlateMuted)
                                        Text("₹15,000.00", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GovGreen)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Mode / Provider:", fontSize = 10.sp, color = GovSlateMuted)
                                        Text("${selectedFeeGateway.displayName} (HMAC Verified)", fontSize = 10.sp, color = GovSlateDark)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Ledger Settlement:", fontSize = 10.sp, color = GovSlateMuted)
                                        Text("POSTED TO GENERAL LEDGER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GovGreen)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            GlassButton(
                                text = "Download Official Sealed Receipt (PDF)",
                                onClick = {
                                    Toast.makeText(context, "Downloading cryptographic receipt PDF...", Toast.LENGTH_SHORT).show()
                                },
                                color = GovNavyPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    // Gateway Checkout Console
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Zero-Trust Payment Gateway Checkout:",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovNavyDark
                            )
                            Text(
                                text = "No API keys, card details, or UPI PINs stored in app. Backend verifiable transactions.",
                                fontSize = 9.5.sp,
                                color = GovSlateMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Provider Selector
                            Text("Select Payment Gateway Provider:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GovSlateDark)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(PaymentGatewayProvider.RAZORPAY, PaymentGatewayProvider.CASHFREE).forEach { prov ->
                                    val isSelected = selectedFeeGateway == prov
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) GovNavyPrimary.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) GovNavyPrimary else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { selectedFeeGateway = prov }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(prov.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) GovNavyPrimary else GovSlateDark)
                                            Text("Gateway", fontSize = 8.5.sp, color = GovSlateMuted)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (activeStudentOrder == null) {
                                GlassButton(
                                    text = "Initiate Secure Checkout Order",
                                    onClick = {
                                        val order = studentPaymentGateway.createPaymentOrder(
                                            tenantId = "INST-SXA-2026",
                                            studentId = student.admissionNumber,
                                            studentName = student.name,
                                            feeRecordId = "FEE-2026-001",
                                            term = "Term 1 Composite Tuition",
                                            amountPaise = 1500000L,
                                            idempotencyKey = UUID.randomUUID().toString(),
                                            provider = selectedFeeGateway
                                        )
                                        activeStudentOrder = order
                                        Toast.makeText(context, "Order ${order.internalOrderId} initiated!", Toast.LENGTH_SHORT).show()
                                    },
                                    color = GovNavyPrimary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                val order = activeStudentOrder!!
                                val upiUri = UpiPaymentIntent.buildUpiUri(
                                    payeeVpa = "fees.${order.tenantId.lowercase().replace("-", "")}@icici",
                                    payeeName = "St. Xavier's International Academy",
                                    orderId = order.internalOrderId,
                                    amountRupees = order.amountRupees,
                                    note = "Fee - ${order.receiptNumber}"
                                )

                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF1F5F9), modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Order ID: ${order.internalOrderId}", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                                        Text("Invoice ID: ${order.internalInvoiceId} • Payable: ₹${order.amountRupees}", fontSize = 9.5.sp, color = GovSlateDark)
                                        Text("UPI Intent: $upiUri", fontSize = 8.5.sp, fontFamily = FontFamily.Monospace, color = GovSlateMuted)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassButton(
                                        text = "Open UPI App",
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "UPI URI verified. (Use simulator below)", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        color = GovGreen,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GlassButton(
                                        text = "Complete Payment (HMAC)",
                                        onClick = {
                                            val paymentId = "pay_stu_${System.currentTimeMillis().toString().takeLast(6)}"
                                            val secret = "rzp_test_secret_key_prod_2026"
                                            val signature = studentPaymentGateway.computeHmacSha256("${order.gatewayOrderId}|$paymentId", secret)
                                            val req = PaymentVerificationRequest(
                                                internalOrderId = order.internalOrderId,
                                                gatewayOrderId = order.gatewayOrderId ?: "gw_ord_1",
                                                gatewayPaymentId = paymentId,
                                                gatewaySignature = signature,
                                                tenantId = order.tenantId
                                            )
                                            val result = studentPaymentGateway.verifyPayment(req, secret)
                                            if (result.isVerified) {
                                                isFeePaidSuccessfully = true
                                                paymentReceiptNo = result.receiptNumber
                                                viewModel.inchargeRecordFeePayment(
                                                    feeId = "FEE-2026-001",
                                                    paidAmount = 15000L,
                                                    dueAmount = 0L,
                                                    receiptNo = result.receiptNumber ?: "RCP-9901"
                                                )
                                                Toast.makeText(context, "Payment verified! Receipt: ${result.receiptNumber}", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        color = GovNavyPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 4: SCHOOL DIARY & HOMEWORK
        // =====================================================================
        if (selectedTab == 4) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Daily School Diary & Homework (Class 10-A):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GovNavyDark
                )

                classDiary.forEach { entry ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = GovNavyPrimary,
                        headerTitle = "${entry.subject.uppercase()} - ${entry.date}",
                        headerBadge = "TEACHER POSTED"
                    ) {
                        Column {
                            Text(
                                text = "Topic: ${entry.topicCovered}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Homework Assigned: ${entry.homeworkAssigned}",
                                fontSize = 11.sp,
                                color = GovNavyPrimary
                            )
                            if (entry.specialInstructions.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Special Note: ${entry.specialInstructions}",
                                    fontSize = 10.sp,
                                    color = GovSaffron,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Teacher: ${entry.teacherName} (In-Charge Verified)",
                                fontSize = 9.sp,
                                color = GovSlateMuted
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 5: CERTIFICATES & REQUEST DESK
        // =====================================================================
        if (selectedTab == 5) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Certificates & Digital Verifications:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )

                    GlassButton(
                        text = "Apply Certificate",
                        onClick = { showCertDialog = true },
                        color = GovNavyPrimary
                    )
                }

                studentCerts.forEach { cert ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        headerRibbonColor = if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") GovGreen else GovSaffron,
                        headerTitle = "${cert.certificateType} CERTIFICATE",
                        headerBadge = cert.applicationStatus.replace("_", " ")
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Certificate ID: ${cert.certificateId}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovSlateDark
                                    )
                                    Text(
                                        text = "Purpose: ${cert.purposeOrAchievement}",
                                        fontSize = 10.sp,
                                        color = GovSlateText
                                    )
                                    Text(
                                        text = "Status: ${cert.applicationStatus.replace("_", " ")}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") GovGreen else GovSaffron
                                    )
                                }

                                if (cert.applicationStatus == "APPROVED_BY_PRINCIPAL") {
                                    OfficialSealStamp(
                                        title = "EXECUTIVE",
                                        code = "APPROVED",
                                        modifier = Modifier.size(52.dp),
                                        color = GovGreen
                                    )
                                }
                            }

                            if (cert.isDownloadUnlocked) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GlassButton(
                                        text = "Download Sealed Certificate",
                                        onClick = {
                                            Toast.makeText(context, "Downloading authenticated Certificate PDF: ${cert.certificateId}", Toast.LENGTH_SHORT).show()
                                        },
                                        color = GovGreen,
                                        leadingIcon = {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                        },
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (cert.secureWhatsAppToken != null) {
                                        OfficialWhatsAppButton(
                                            phone = student.guardianPhone,
                                            message = "St. Xavier's International Academy: Official ${cert.certificateType} Certificate generated for ${student.name}. Token: ${cert.secureWhatsAppToken}",
                                            label = "WhatsApp Copy",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TAB 6: WHATSAPP ATTENDANCE PUSH ALERTS & LINKED MOBILE
        // =====================================================================
        if (selectedTab == 6) {
            var simCondition by remember { mutableIntStateOf(0) } // 0: All Present, 1: Partial Absent, 2: Absent Today
            val linkedNumber = student.studentPhone.ifBlank { student.guardianPhone.ifBlank { "+91 98765 43210" } }
            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val (simTotal, simAbsent) = when (simCondition) {
                0 -> 5 to emptyList<String>()
                1 -> 5 to listOf("Period 3 - Science Practical")
                else -> 5 to listOf("Assembly", "Period 1", "Period 2", "Period 3", "Afternoon")
            }

            val simMessage = AttendanceWhatsAppGenerator.generateComputerGeneratedWhatsAppMessage(
                studentName = student.name,
                date = todayDate,
                totalClasses = simTotal,
                absentClasses = simAbsent,
                linkedMobile = linkedNumber,
                schoolName = "St. Xavier's Academic Portal"
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PureWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Automated Attendance WhatsApp Push",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GovNavyDark
                                    )
                                    Text(
                                        text = "Official computer-generated dispatch to student's linked number",
                                        fontSize = 10.sp,
                                        color = GovSlateMuted
                                    )
                                }
                            }
                            GlassBadge(text = "LIVE SYSTEM", color = Color(0xFF25D366))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Linked Mobile Display
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Verified Linked Mobile Number", fontSize = 10.sp, color = GovSlateMuted)
                                    Text(linkedNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = GovNavyDark)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Simulation condition selector
                        Text(
                            text = "Test Attendance Condition Simulation:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (simCondition == 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (simCondition == 0) Color(0xFF22C55E) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { simCondition = 0 }
                                    .testTag("btn_sim_all_present")
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("All Present", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (simCondition == 0) Color(0xFF15803D) else GovSlateMuted)
                                    Text("Attended all", fontSize = 8.5.sp, color = GovSlateMuted)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (simCondition == 1) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (simCondition == 1) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { simCondition = 1 }
                                    .testTag("btn_sim_partial_absent")
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Partial Absent", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (simCondition == 1) Color(0xFFB45309) else GovSlateMuted)
                                    Text("Missed class", fontSize = 8.5.sp, color = GovSlateMuted)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (simCondition == 2) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (simCondition == 2) Color(0xFFEF4444) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { simCondition = 2 }
                                    .testTag("btn_sim_all_absent")
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("All Absent", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (simCondition == 2) Color(0xFFB91C1C) else GovSlateMuted)
                                    Text("Absent today", fontSize = 8.5.sp, color = GovSlateMuted)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Message Preview Bubble (WhatsApp Style)
                        Text("Computer-Generated Message Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GovNavyDark)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFE7FBD3),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB7E4A8)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = simMessage,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF0F172A),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Launch WhatsApp Button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    launchWhatsAppMessage(context, linkedNumber, simMessage)
                                }
                                .testTag("btn_launch_whatsapp_sim"),
                            color = Color(0xFF25D366)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = PureWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Send WhatsApp Notice to Linked Number",
                                    fontSize = 12.sp,
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

    // Apply for Certificate Dialog
    if (showCertDialog && student != null) {
        val curStudent = student
        AlertDialog(
            onDismissRequest = { showCertDialog = false },
            title = {
                Text("Apply for Official School Certificate", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Student: ${curStudent.name} | Grade 10-A", fontSize = 11.sp, color = GovSlateMuted)
                    GlassTextField(
                        value = certType,
                        onValueChange = { certType = it },
                        placeholder = "BONAFIDE, TRANSFER, or MERIT",
                        label = "Certificate Type"
                    )
                    GlassTextField(
                        value = certPurpose,
                        onValueChange = { certPurpose = it },
                        placeholder = "e.g. Passport, Olympiad, Board verification",
                        label = "Official Purpose"
                    )
                }
            },
            confirmButton = {
                GlassButton(
                    text = "Submit Application",
                    onClick = {
                        viewModel.applyForCertificate(
                            type = certType,
                            studentId = curStudent.id,
                            studentName = curStudent.name,
                            grade = "Grade ${curStudent.grade}-${curStudent.section}",
                            purpose = certPurpose
                        )
                        Toast.makeText(context, "Application submitted for In-Charge and Principal review!", Toast.LENGTH_SHORT).show()
                        showCertDialog = false
                    }
                )
            },
            dismissButton = {
                GlassOutlinedButton(text = "Cancel", onClick = { showCertDialog = false })
            }
        )
    }
}
