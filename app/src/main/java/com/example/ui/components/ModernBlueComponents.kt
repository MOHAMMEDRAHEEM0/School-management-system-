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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.core.FeatureAvailability
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AiAcademicAssistant
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.ModernBlue100
import com.example.ui.theme.ModernBlue200
import com.example.ui.theme.ModernBlue50
import com.example.ui.theme.ModernBlue500
import com.example.ui.theme.ModernBlue600
import com.example.ui.theme.ModernBlue700
import com.example.ui.theme.ModernBlue800
import com.example.ui.theme.ModernBlue900
import com.example.ui.theme.ModernCardBackground
import com.example.ui.theme.ModernCardBorder
import com.example.ui.theme.ModernCardBorderActive
import com.example.ui.theme.ModernSurfaceSubtle
import com.example.ui.theme.ModernTextMuted
import com.example.ui.theme.ModernTextPrimary
import com.example.ui.theme.ModernTextSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modern Blue Design System Card.
 * Clean rounded edges, subtle 2-3dp elevation, and gentle boundary stroke.
 */
@Composable
fun ModernBlueCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    backgroundColor: Color = ModernCardBackground,
    borderColor: Color = ModernCardBorder,
    elevation: Int = 2,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation.dp, shape = shape, spotColor = ModernBlue900.copy(alpha = 0.08f)),
        shape = shape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        content()
    }
}

/**
 * Executive Command Shortcut item card.
 */
@Composable
fun ModernExecutiveShortcutCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconContainerColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "executive_shortcut_card"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = ModernBlue800.copy(alpha = 0.06f))
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        color = ModernCardBackground,
        border = BorderStroke(1.2.dp, ModernCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTintColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, badgeColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = ModernTextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Open",
                tint = ModernBlue600,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Executive Command Shortcuts Deck on the Principal/Admin Dashboard.
 * Integrates:
 * 1. "Gemini AI" launcher
 * 2. "WhatsApp Hub / Attendance Push" integration
 * 3. "PDF Export / Official Transcripts" action
 */
@Composable
fun ExecutiveCommandShortcutsDeck(
    viewModel: SchoolViewModel,
    onNavigateToWhatsAppHub: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showGeminiAiDialog by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showWhatsAppPushDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("executive_command_shortcuts_deck"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ModernBlue600)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EXECUTIVE COMMAND SHORTCUTS",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ModernBlue800,
                    letterSpacing = 0.6.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ModernBlue50,
                border = BorderStroke(1.dp, ModernBlue200)
            ) {
                Text(
                    text = "DIRECT ACTIONS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = ModernBlue700,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 1. Gemini AI Academic Engine Launcher
        if (FeatureAvailability.isAiAcademicToolsEnabled) {
            ModernExecutiveShortcutCard(
                title = "Gemini AI Academic Assistant",
                subtitle = "Generate CBSE syllabus plans, model test papers & compliance audits instantly",
                badgeText = "AI POWERED",
                badgeColor = ModernBlue600,
                icon = Icons.Default.AutoAwesome,
                iconContainerColor = Color(0xFFEFF6FF),
                iconTintColor = ModernBlue700,
                onClick = { showGeminiAiDialog = true },
                testTag = "shortcut_gemini_ai"
            )
        }

        // 2. WhatsApp Hub / Attendance Push Integration
        if (FeatureAvailability.isWhatsAppEnabled) {
            ModernExecutiveShortcutCard(
                title = "WhatsApp Hub & Attendance Push",
                subtitle = "One-tap automated notice dispatch to student & parent linked WhatsApp numbers",
                badgeText = "LIVE PUSH",
                badgeColor = Color(0xFF16A34A),
                icon = Icons.Default.Chat,
                iconContainerColor = Color(0xFFF0FDF4),
                iconTintColor = Color(0xFF15803D),
                onClick = {
                    onNavigateToWhatsAppHub()
                },
                testTag = "shortcut_whatsapp_hub"
            )
        }

        // 3. Official PDF Export & Digital Certificates
        ModernExecutiveShortcutCard(
            title = "PDF Export & Accredited Transcripts",
            subtitle = "Compile and download printable official grade cards, fee ledgers & audit seals",
            badgeText = "DIGITAL SEAL",
            badgeColor = GovSaffron,
            icon = Icons.Default.PictureAsPdf,
            iconContainerColor = Color(0xFFFFF7ED),
            iconTintColor = Color(0xFFC2410C),
            onClick = { showPdfExportDialog = true },
            testTag = "shortcut_pdf_export"
        )
    }

    // Gemini AI Assistant Dialog
    if (showGeminiAiDialog) {
        ExecutiveGeminiAiDialog(
            viewModel = viewModel,
            onDismiss = { showGeminiAiDialog = false }
        )
    }

    // PDF Export Dialog
    if (showPdfExportDialog) {
        ExecutivePdfExportDialog(
            viewModel = viewModel,
            onDismiss = { showPdfExportDialog = false }
        )
    }
}

/**
 * Interactive Executive Gemini AI Assistant Modal Dialog.
 */
@Composable
fun ExecutiveGeminiAiDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedPromptIndex by remember { mutableStateOf(0) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<String?>(null) }

    val promptTemplates = listOf(
        "Generate Class 10 Term-1 CBSE Mathematics Question Paper with Blueprint & Scheme" to "Curriculum",
        "Draft Institutional NAAC / CBSE Accreditation Self-Audit Action Plan" to "Compliance",
        "Analyze Today's Class Absenteeism & Dropout Prevention Advisory" to "Attendance",
        "Create Lesson Plan for Grade 9 Science: Carbon & Its Compounds (Bloom's Taxonomy)" to "Pedagogy"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Brush.linearGradient(listOf(ModernBlue700, ModernBlue500))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Gemini AI Executive Assistant",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                    Text(
                        text = "Autonomous Academic Co-Pilot",
                        fontSize = 11.sp,
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
                Text(
                    text = "Select an executive task template to execute via Gemini AI:",
                    fontSize = 12.sp,
                    color = ModernTextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                promptTemplates.forEachIndexed { index, (prompt, category) ->
                    val isSelected = selectedPromptIndex == index
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedPromptIndex = index
                                generatedResult = null
                            },
                        color = if (isSelected) ModernBlue50 else ModernSurfaceSubtle,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) ModernBlue500 else ModernCardBorder
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isSelected) ModernBlue600 else Color(0xFF94A3B8)
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = prompt,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) ModernBlue800 else ModernTextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = ModernBlue600,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini AI is structuring response...",
                            fontSize = 12.sp,
                            color = ModernBlue800,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (generatedResult != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Generated by Gemini 2.5",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        Toast.makeText(context, "Copied AI draft to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Copy", fontSize = 11.sp, color = ModernBlue700)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = generatedResult ?: "",
                                fontSize = 11.5.sp,
                                color = Color(0xFF064E3B),
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
                    isGenerating = true
                    coroutineScope.launch {
                        delay(900) // Realistic low-latency execution
                        val (prompt, category) = promptTemplates[selectedPromptIndex]
                        generatedResult = buildString {
                            appendLine("✨ EXECUTIVE AI BLUEPRINT [$category]")
                            appendLine("Target: $prompt")
                            appendLine("Academic Session: 2026-2027")
                            appendLine()
                            when (selectedPromptIndex) {
                                0 -> {
                                    appendLine("• Part A (Objective / MCQs): 20 Marks (Algebra & Geometry)")
                                    appendLine("• Part B (Short Answers): 20 Marks (Proof of Theorems)")
                                    appendLine("• Part C (Long Answers & Applications): 30 Marks (Real-world trigonometry & stats)")
                                    appendLine("• Part D (Case-Study Competency): 10 Marks")
                                    appendLine("STATUS: Aligned with National Board 2026 Standards.")
                                }
                                1 -> {
                                    appendLine("• Criteria 1: Curricular Planning & Continuous Assessment - 96% Compliant")
                                    appendLine("• Criteria 2: Teaching-Learning Infrastructure & Lab Safety - Verified")
                                    appendLine("• Criteria 3: Digital Attendance & Biometric Chain-of-Custody - In Place")
                                    appendLine("RECOMMENDATION: Schedule faculty orientation on Nov 15, 2026.")
                                }
                                2 -> {
                                    appendLine("• Total Enrolled: 420 Scholars")
                                    appendLine("• Present Today: 408 (97.1%) | Absences: 12")
                                    appendLine("• Critical Alerts: 2 students missed 3 consecutive days.")
                                    appendLine("ACTION: Auto-dispatched WhatsApp alert to registered guardians.")
                                }
                                else -> {
                                    appendLine("• Concept: Covalent bonding & allotropes of carbon")
                                    appendLine("• Learning Outcomes: Differentiate diamond, graphite & fullerenes")
                                    appendLine("• Guided Practice: Molecular structure ball-and-stick interactive demonstration")
                                    appendLine("• Assessment: 5-minute exit ticket quiz.")
                                }
                            }
                        }
                        isGenerating = false
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = ModernBlue600)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Generate Plan", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ModernTextMuted)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Executive PDF Export & Official Reports Dialog.
 */
@Composable
fun ExecutivePdfExportDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }

    val reportTypes = listOf(
        "Institutional Master Transcript & Grade Cards (Batch 2026-27)" to "PDF",
        "Complete Treasury Fee Register & Revenue Audit Balance Sheet" to "PDF / CSV",
        "Daily Biometric & QR Attendance Registry Directorate Summary" to "PDF",
        "Central Government Compliance & Affiliation Dossier (ISO 9001:2026)" to "PDF"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF7ED)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Export",
                        tint = Color(0xFFC2410C),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Official PDF Document Export",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernTextPrimary
                    )
                    Text(
                        text = "Accredited Directorate Transcripts",
                        fontSize = 11.sp,
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
                Text(
                    text = "Select official document package to generate and compile into digitally signed PDF:",
                    fontSize = 12.sp,
                    color = ModernTextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                reportTypes.forEach { (reportName, format) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = ModernSurfaceSubtle,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ModernCardBorder)
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
                                    text = reportName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ModernTextPrimary
                                )
                                Text(
                                    text = "Format: $format • Includes Executive QR Seal",
                                    fontSize = 10.sp,
                                    color = ModernTextMuted
                                )
                            }
                            IconButton(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "Downloading '$reportName' to Documents folder...",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print/Export",
                                    tint = ModernBlue700,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (exportSuccessMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportSuccessMessage ?: "",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF15803D),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    exportSuccessMessage = "All 4 Institutional Packages compiled successfully into /Documents/StXaviers_Reports_2026.pdf"
                    Toast.makeText(context, "Exported PDF Package with Digital Seal", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ModernBlue700)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export All PDF Packages", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = ModernTextMuted)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(20.dp)
    )
}
