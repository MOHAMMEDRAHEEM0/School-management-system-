package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.FeeRecordEntity
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =============================================================================
// 1. AUTOMATED FEE REMINDERS & RECEIPT GENERATOR
// =============================================================================

@Composable
fun AutomatedFeeRemindersAndReceipts(
    studentName: String = "Aarav Sharma",
    studentId: String = "ADM-2026-001",
    parentPhone: String = "+91 91234 56780",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedReceiptForDialog by remember { mutableStateOf<FeeRecordEntity?>(null) }

    val feeRecords = remember {
        mutableStateListOf(
            FeeRecordEntity(
                id = "FEE-2026-001",
                studentId = studentId,
                admissionNumber = studentId,
                studentName = studentName,
                gradeSection = "Grade 10-A",
                term = "Term 1 (Academic Year 2026-27)",
                feeType = "Composite Tuition & Advanced Science Lab",
                totalAmount = 6500000L, // 65,000.00
                paidAmount = 6500000L,
                dueAmount = 0L,
                status = "PAID",
                receiptNumber = "SXA-REC-2026-9901",
                paidDate = "2026-08-05",
                paymentMode = "UPI_RAZORPAY",
                verifiedByIncharge = true
            ),
            FeeRecordEntity(
                id = "FEE-2026-002",
                studentId = studentId,
                admissionNumber = studentId,
                studentName = studentName,
                gradeSection = "Grade 10-A",
                term = "Term 2 (Academic Year 2026-27)",
                feeType = "Composite Tuition & Digital Computer Academy",
                totalAmount = 6500000L, // 65,000.00
                paidAmount = 0L,
                dueAmount = 6500000L,
                status = "DUE",
                receiptNumber = "PENDING_REMITTANCE",
                paidDate = null,
                paymentMode = "PENDING",
                verifiedByIncharge = false
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("automated_fee_reminders_receipts")
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Automated Fee Ledger & Receipts",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernTextPrimary
                )
                Text(
                    text = "Fee compliance, WhatsApp reminder dispatch & PDF receipts",
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
                    text = "LEDGER V9.4",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernBlue700,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        feeRecords.forEach { record ->
            val isPaid = record.status == "PAID"
            val totalInr = record.totalAmount / 100L
            val dueInr = record.dueAmount / 100L

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureWhite,
                border = BorderStroke(1.dp, if (isPaid) GovGreen else GovRedAlert),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = record.term,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ModernTextPrimary
                            )
                            Text(
                                text = record.feeType,
                                fontSize = 11.sp,
                                color = ModernTextMuted
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPaid) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (isPaid) "CLEARED" else "DUE: ₹$dueInr",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaid) GovGreen else GovRedAlert,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Scheduled: ₹$totalInr",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GovSlateDark
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isPaid) {
                                Button(
                                    onClick = { selectedReceiptForDialog = record },
                                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_view_receipt_${record.id}")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Official Receipt", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val message = "Dear Parent of $studentName (Class 10-A), this is an official reminder from St. Xavier's International Academy. Your Term 2 tuition fee of ₹$dueInr is pending remittance. Kindly clear via our cloud portal or accounts desk to avoid late enrollment surcharge. Ref: ${record.id}"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = Uri.parse("https://api.whatsapp.com/send?phone=${parentPhone.replace(" ", "").replace("+", "")}&text=${Uri.encode(message)}")
                                        }
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("btn_send_fee_whatsapp")
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PureWhite, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Send WhatsApp Alert", fontSize = 11.sp, color = PureWhite)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Modal PDF Receipt Dialog with Verification QR Barcode
    selectedReceiptForDialog?.let { receipt ->
        AlertDialog(
            onDismissRequest = { selectedReceiptForDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Computerized Fee Receipt",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovNavyDark
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = "VERIFIED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovGreen,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "ST. XAVIER'S INTERNATIONAL ACADEMY",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = GovNavyPrimary
                    )
                    Text(
                        text = "Autonomous Institutional Board • CBSE Affil. 1130492",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Text(text = "Receipt No: ${receipt.receiptNumber}", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = "Date: ${receipt.paidDate ?: "2026-08-05"} | Mode: ${receipt.paymentMode ?: "UPI"}", fontSize = 10.5.sp, color = GovSlateDark)
                    Text(text = "Scholar: ${receipt.studentName} (${receipt.admissionNumber})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Class: ${receipt.gradeSection}", fontSize = 10.5.sp, color = GovSlateDark)

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, ModernBlue200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = receipt.feeType, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = ModernBlue800)
                            Text(text = "₹${receipt.paidAmount / 100L}.00", fontSize = 11.sp, fontWeight = FontWeight.Black, color = ModernBlue800)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // QR Barcode for Verification
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PureWhite,
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Verification QR Code",
                                    tint = GovNavyDark,
                                    modifier = Modifier.size(72.dp)
                                )
                                Text(
                                    text = "Scan to verify: ${receipt.receiptNumber}",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GovSlateMuted
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        Toast.makeText(context, "Downloading PDF Receipt: ${receipt.receiptNumber}.pdf", Toast.LENGTH_SHORT).show()
                        selectedReceiptForDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Print / Save PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedReceiptForDialog = null }) {
                    Text("Close", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// =============================================================================
// 2. GPS TRANSPORT ROUTE TRACKING (LIVE BUS & ETA FOR PARENTS)
// =============================================================================

data class BusRouteStop(
    val stopNumber: Int,
    val stopName: String,
    val scheduledTime: String,
    val status: String // COMPLETED, CURRENT, UPCOMING
)

@Composable
fun GpsTransportRouteTracker(
    routeNumber: String = "Route #4",
    routeName: String = "Civil Lines - Sector 5 Express",
    studentName: String = "Aarav Sharma",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var etaMinutes by remember { mutableIntStateOf(4) }
    var currentSpeedKmh by remember { mutableIntStateOf(34) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            etaMinutes = (etaMinutes - 1).coerceAtLeast(1)
            currentSpeedKmh = (30..42).random()
        }
    }

    val stops = remember {
        listOf(
            BusRouteStop(1, "Central Bus Depot", "07:15 AM", "COMPLETED"),
            BusRouteStop(2, "Civil Lines Circle", "07:28 AM", "COMPLETED"),
            BusRouteStop(3, "Sector 5 Metro Junction (Your Stop)", "07:38 AM", "CURRENT"),
            BusRouteStop(4, "Model Town Crossing", "07:48 AM", "UPCOMING"),
            BusRouteStop(5, "St. Xavier's Main Campus", "08:05 AM", "UPCOMING")
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.2.dp, ModernCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("gps_transport_route_tracker")
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(ModernBlue100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsBus,
                            contentDescription = "School Bus",
                            tint = ModernBlue700,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "$routeNumber: $routeName",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary
                        )
                        Text(
                            text = "Vehicle: MH-16-BX-4026 • Driver: Harish Chandra",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, GovGreen)
                ) {
                    Text(
                        text = "LIVE GPS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ETA & Speed Highlight Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, ModernBlue200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = ModernBlue700, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Estimated Arrival (ETA)",
                                fontSize = 10.sp,
                                color = ModernTextMuted
                            )
                            Text(
                                text = "Arriving in ~$etaMinutes mins",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ModernBlue800
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = GovGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Current Transit Speed",
                                fontSize = 10.sp,
                                color = ModernTextMuted
                            )
                            Text(
                                text = "$currentSpeedKmh km/h",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Boarding Push Alert
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(0.8.dp, Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GovGreen, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Boarding Alert: $studentName boarded at Stop #2 (07:29 AM)",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF166534)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Route Stops Stepper
            Text(
                text = "Transit Progress Timeline:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = ModernTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            stops.forEach { stop ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                when (stop.status) {
                                    "COMPLETED" -> GovGreen
                                    "CURRENT" -> ModernBlue700
                                    else -> Color(0xFFCBD5E1)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stop.status == "COMPLETED") {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(10.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = stop.stopName,
                        fontSize = 11.sp,
                        fontWeight = if (stop.status == "CURRENT") FontWeight.Bold else FontWeight.Normal,
                        color = if (stop.status == "CURRENT") ModernBlue800 else ModernTextPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = stop.scheduledTime,
                        fontSize = 10.sp,
                        color = ModernTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Driver Call Button
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:+919844455667")
                    }
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_call_bus_driver")
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Bus Driver (+91 98444 55667)", fontSize = 11.sp)
            }
        }
    }
}

// =============================================================================
// 3. DIGITAL VISITOR & GATE PASS SYSTEM (QR-BASED PRE-APPROVAL)
// =============================================================================

data class GatePassRecord(
    val passId: String,
    val studentName: String,
    val admissionNumber: String,
    val guardianName: String,
    val guardianPhone: String,
    val reason: String,
    val requestedTime: String,
    val approvalStatus: String = "APPROVED_BY_INCHARGE", // PENDING, APPROVED_BY_INCHARGE, SECURITY_CHECKED_OUT
    val qrCodeToken: String
)

@Composable
fun DigitalVisitorAndGatePassSystem(
    studentName: String = "Aarav Sharma",
    studentId: String = "ADM-2026-001",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showRequestDialog by remember { mutableStateOf(false) }
    var pickupReason by remember { mutableStateOf("Medical Consultation / Dental Checkup") }
    var pickupTime by remember { mutableStateOf("01:30 PM") }
    var authorizedGuardian by remember { mutableStateOf("Vikram Sharma (Father)") }

    val gatePasses = remember {
        mutableStateListOf(
            GatePassRecord(
                passId = "GP-2026-089",
                studentName = studentName,
                admissionNumber = studentId,
                guardianName = "Vikram Sharma (Father)",
                guardianPhone = "+91 91234 56780",
                reason = "Family Event / Early Departure Approved",
                requestedTime = "01:30 PM, 02-Oct-2026",
                approvalStatus = "APPROVED_BY_INCHARGE",
                qrCodeToken = "GATEPASS:GP-2026-089:ADM-2026-001:VERIFIED"
            )
        )
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = PureWhite,
        border = BorderStroke(1.2.dp, ModernCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("digital_gate_pass_system")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Digital Visitor & Student Gate Pass",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                    Text(
                        text = "Pre-approved early pickup & security desk QR pass",
                        fontSize = 11.sp,
                        color = ModernTextMuted
                    )
                }

                Button(
                    onClick = { showRequestDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_request_gate_pass")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("New Pass", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active Passes
            gatePasses.forEach { pass ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PASS #${pass.passId}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ModernBlue800
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (pass.approvalStatus.contains("APPROVED")) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = pass.approvalStatus.replace("_", " "),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pass.approvalStatus.contains("APPROVED")) GovGreen else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Authorized Pickup: ${pass.guardianName}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ModernTextPrimary
                        )
                        Text(
                            text = "Reason: ${pass.reason} • Expected: ${pass.requestedTime}",
                            fontSize = 10.5.sp,
                            color = ModernTextMuted
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Code & Security Verification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PureWhite,
                                    border = BorderStroke(0.8.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "Gate QR",
                                        tint = GovNavyDark,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = "Scan at Security Gate",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ModernTextPrimary
                                    )
                                    Text(
                                        text = "Validates identity instantly",
                                        fontSize = 9.sp,
                                        color = ModernTextMuted
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    Toast.makeText(context, "Gate Pass verified by Security Officer! Scholar checked out.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_security_verify_pass")
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gate Check-Out", fontSize = 10.5.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Modal to request Gate Pass
    if (showRequestDialog) {
        AlertDialog(
            onDismissRequest = { showRequestDialog = false },
            title = {
                Text(
                    text = "Request Early Pickup Gate Pass",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernTextPrimary
                )
            },
            text = {
                Column {
                    Text(text = "Authorized Guardian:", fontSize = 11.sp, color = ModernTextMuted)
                    OutlinedTextField(
                        value = authorizedGuardian,
                        onValueChange = { authorizedGuardian = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_guardian_name")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Pickup Reason:", fontSize = 11.sp, color = ModernTextMuted)
                    OutlinedTextField(
                        value = pickupReason,
                        onValueChange = { pickupReason = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_pickup_reason")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Expected Pickup Time:", fontSize = 11.sp, color = ModernTextMuted)
                    OutlinedTextField(
                        value = pickupTime,
                        onValueChange = { pickupTime = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_pickup_time")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newId = "GP-2026-${(100..999).random()}"
                        gatePasses.add(
                            0,
                            GatePassRecord(
                                passId = newId,
                                studentName = studentName,
                                admissionNumber = studentId,
                                guardianName = authorizedGuardian,
                                guardianPhone = "+91 91234 56780",
                                reason = pickupReason,
                                requestedTime = pickupTime,
                                approvalStatus = "APPROVED_BY_INCHARGE",
                                qrCodeToken = "GATEPASS:$newId:$studentId:VERIFIED"
                            )
                        )
                        Toast.makeText(context, "Gate Pass $newId generated and pre-approved!", Toast.LENGTH_SHORT).show()
                        showRequestDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700)
                ) {
                    Text("Generate Pass", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRequestDialog = false }) {
                    Text("Cancel", color = GovSlateMuted)
                }
            },
            containerColor = PureWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
