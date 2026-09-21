package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassCard
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite
import com.example.ui.viewmodel.SchoolViewModel

@Composable
fun AuditLogsScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit
) {
    val auditLogs by viewModel.auditLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("audit_logs_screen")
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
                    text = "Institutional Audit Ledger",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = GovNavyDark
                )
                Text(
                    text = "Immutable Chronological Record of All Academy Operations",
                    fontSize = 10.sp,
                    color = GovSlateMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Notice
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerRibbonColor = GovNavyDark,
            headerTitle = "NON-REPUDIATION PROTOCOL"
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = GovGreen, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Cryptographically Anchored Audit Stream",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateDark
                    )
                    Text(
                        text = "Every transaction, verification, seal, and mark modification is permanently logged with actor credentials and cannot be purged or modified.",
                        fontSize = 9.5.sp,
                        color = GovSlateMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chronological Log List
        auditLogs.reversed().forEach { log ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerRibbonColor = when {
                    log.action.contains("APPROVE") -> GovGreen
                    log.action.contains("VERIFY") -> GovSaffron
                    log.action.contains("LOGIN") -> GovNavyPrimary
                    else -> GovSlateDark
                },
                headerTitle = log.action,
                headerBadge = "#${log.logId}"
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Actor: ${log.actorName} (${log.actorRole})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GovSlateDark
                            )
                            Text(
                                text = "Entity: ${log.entityType} [${log.entityId}]",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GovNavyPrimary
                            )
                        }
                        Text(
                            text = log.formattedTime.take(19).replace("T", " "),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = GovSlateMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (!log.previousState.isNullOrBlank() || !log.newState.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "State: ${log.previousState ?: "NONE"} -> ${log.newState}",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = GovSlateDark
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = log.remarks,
                        fontSize = 10.5.sp,
                        color = GovSlateText
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
