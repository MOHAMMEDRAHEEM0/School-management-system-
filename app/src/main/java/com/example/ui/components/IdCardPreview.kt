package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.ui.theme.GovBorderGrey
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovPageBg
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite

/**
 * Official Computer-Generated Digital Student ID Card (Forever Archive)
 * Clean, printable, official non-glass format for printing and download.
 */
@Composable
fun OfficialStudentIdCard(
    student: StudentEntity,
    schoolName: String = "ST. XAVIER'S INTERNATIONAL ACADEMY",
    academicYear: String = "2026-2027",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("official_student_id_card")
    ) {
        // ID Card Surface with 1.58 standard aspect ratio (credit card form factor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.58f)
                .shadow(4.dp, RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(PureWhite)
                .border(2.dp, GovNavyPrimary, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Bar: School Name & Crest
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(GovNavyPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = schoolName,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GovNavyPrimary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "OFFICIAL STUDENT IDENTITY CARD   SESSION $academicYear",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateMuted
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GovSaffron)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SCHOLAR",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                color = PureWhite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    // Academy Ribbon Divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .clip(RoundedCornerShape(1.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GovSaffron))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GovNavyPrimary))
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().background(GovGreen))
                    }
                }

                // Body: Photo & Credentials
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Frame
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF1F5F9))
                            .border(1.5.dp, GovNavyPrimary, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Student Photo",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Bio Details
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = student.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = GovSlateDark
                        )
                        Text(
                            text = "ADM NO: ${student.admissionNumber}   ROLL #${student.rollNumber}",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyPrimary
                        )
                        Text(
                            text = "CLASS: Grade ${student.grade} - Sec ${student.section}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateDark
                        )
                        Text(
                            text = "DOB: ${student.dateOfBirth}   BLOOD: ${student.bloodGroup}",
                            fontSize = 8.5.sp,
                            color = GovSlateText
                        )
                        Text(
                            text = "GUARDIAN: ${student.guardianName} (${student.guardianPhone})",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GovSlateMuted
                        )
                        Text(
                            text = "DIGITAL ID: ${student.convertedDigitalId}",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GovGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Official Principal Seal
                    OfficialSealStamp(
                        title = "PRINCIPAL",
                        code = "SEAL-AUTH",
                        modifier = Modifier.size(50.dp),
                        color = GovGreen
                    )
                }

                // Footer Bar: Security Barcode & QR Verification
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "|||| | |||| ||| ||||| || |||| |||||",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp,
                            color = GovSlateDark,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "VALIDITY: $academicYear (PERMANENT OFFICIAL RECORD)",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateMuted
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Verification QR",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "SEALED",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovGreen
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Printable & Download Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton(
                text = "Download PDF Card",
                onClick = {
                    Toast.makeText(
                        context,
                        "Downloading official digital PVC card: ${student.convertedDigitalId}.pdf",
                        Toast.LENGTH_LONG
                    ).show()
                },
                color = GovNavyPrimary,
                leadingIcon = {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                },
                modifier = Modifier.weight(1f)
            )

            GlassOutlinedButton(
                text = "Print Card",
                onClick = {
                    Toast.makeText(context, "Sending card to school network printer...", Toast.LENGTH_SHORT).show()
                },
                color = GovNavyPrimary,
                leadingIcon = {
                    Icon(Icons.Default.Print, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(14.dp))
                },
                modifier = Modifier.weight(0.8f)
            )
        }
    }
}
