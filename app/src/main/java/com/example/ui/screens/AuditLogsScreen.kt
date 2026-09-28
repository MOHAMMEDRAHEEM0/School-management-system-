package com.example.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
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

@Composable
fun AuditLogsScreen(
    viewModel: SchoolViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val auditLogs by viewModel.auditLogs.collectAsState()
    val isSyncing by viewModel.isAuditSyncing.collectAsState()
    val pendingSyncCount by viewModel.pendingAuditSyncCount.collectAsState()
    val chainReport by viewModel.auditChainReport.collectAsState()

    var filterType by remember { mutableIntStateOf(0) } // 0 = All, 1 = Database TX, 2 = UI Interactions

    val filteredLogs = remember(auditLogs, filterType) {
        when (filterType) {
            1 -> auditLogs.filter { it.interactionType == "DATABASE_TX" }
            2 -> auditLogs.filter { it.interactionType == "SENSITIVE_UI_INTERACTION" }
            else -> auditLogs
        }
    }

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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Institutional Audit Ledger",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = GovNavyDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        GlassBadge(text = "IMMUTABLE CHAIN", color = GovGreen)
                    }
                    Text(
                        text = "Cryptographically Chained Non-Repudiable Log & Encrypted Sync",
                        fontSize = 10.sp,
                        color = GovSlateMuted
                    )
                }
            }

            IconButton(
                onClick = {
                    viewModel.recordSensitiveUiInteraction(
                        action = "MANUAL_AUDIT_LEDGER_REVIEW",
                        screenName = "AuditLogsScreen",
                        details = "Compliance Officer reviewed immutable audit ledger"
                    )
                    Toast.makeText(context, "Sensitive UI review action recorded!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GovNavyPrimary.copy(alpha = 0.08f))
            ) {
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Record UI Action",
                    tint = GovNavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Security Protocol & Cryptographic Compliance Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            headerRibbonColor = GovNavyDark,
            headerTitle = "NON-REPUDIATION & ENCRYPTED COMPLIANCE PROTOCOL"
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = GovGreen, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cryptographically Anchored Hash Chain",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovSlateDark
                        )
                        Text(
                            text = "Every database mutation and sensitive UI gesture is chained via SHA-256 with AES-GCM (256-bit) encrypted server transmission.",
                            fontSize = 9.5.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (chainReport?.isTamperFree != false) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (chainReport?.isTamperFree != false) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("CHAIN INTEGRITY", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GovSlateMuted)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (chainReport?.isTamperFree != false) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (chainReport?.isTamperFree != false) GovGreen else GovRedAlert,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (chainReport?.isTamperFree != false) "100% UNBROKEN" else "TAMPER DETECTED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (chainReport?.isTamperFree != false) GovGreen else GovRedAlert
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("ENCRYPTED SYNC", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = GovSlateMuted)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pendingSyncCount == 0) "ALL SYNCED" else "$pendingSyncCount PENDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (pendingSyncCount == 0) GovGreen else Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }

                if (isSyncing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = GovNavyPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Verification and Sync Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassButton(
                        text = "Verify Chain",
                        onClick = {
                            viewModel.verifyAuditChainIntegrity()
                            Toast.makeText(context, "Audit chain integrity re-verified!", Toast.LENGTH_SHORT).show()
                        },
                        color = GovGreen,
                        leadingIcon = {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    GlassButton(
                        text = if (isSyncing) "Syncing..." else "Sync Encrypted Batch",
                        onClick = {
                            viewModel.triggerEncryptedAuditSync { res ->
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Encrypted sync confirmed! Token: ${res.serverReceiptToken}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Sync deferred: ${res.errorMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        color = GovNavyPrimary,
                        leadingIcon = {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips (All vs Database Transactions vs Sensitive UI Interactions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All Logs (${auditLogs.size})", "Database TX", "UI Gestures").forEachIndexed { index, label ->
                val isSelected = filterType == index
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) GovNavyPrimary else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isSelected) GovNavyPrimary else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .clickable { filterType = index }
                        .testTag("filter_audit_$index")
                ) {
                    Text(
                        text = label,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PureWhite else GovSlateDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chronological Log List
        filteredLogs.reversed().forEach { log ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                headerRibbonColor = when {
                    log.interactionType == "SENSITIVE_UI_INTERACTION" -> Color(0xFFD97706)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Actor: ${log.actorName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GovSlateDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                GlassBadge(
                                    text = if (log.interactionType == "SENSITIVE_UI_INTERACTION") "UI ACTION" else "DB TX",
                                    color = if (log.interactionType == "SENSITIVE_UI_INTERACTION") Color(0xFFD97706) else GovNavyPrimary
                                )
                            }
                            Text(
                                text = "Role: ${log.actorRole} • Entity: ${log.entityType} [${log.entityId}]",
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GovNavyPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = log.formattedTime.take(19).replace("T", " "),
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GovSlateMuted
                            )
                            GlassBadge(
                                text = if (log.syncStatus == "SYNCED_ENCRYPTED") "SYNCED (AES)" else "LOCAL",
                                color = if (log.syncStatus == "SYNCED_ENCRYPTED") GovGreen else Color(0xFF64748B)
                            )
                        }
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

                    if (log.remarks.isNotBlank()) {
                        Text(
                            text = log.remarks,
                            fontSize = 10.5.sp,
                            color = GovSlateText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Cryptographic Block Link
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Block Hash: ${if (log.currentHash.isNotBlank()) log.currentHash.take(16) + "..." else log.securityVerificationHash}",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = GovNavyPrimary
                                )
                            }
                            Text(
                                text = "Chained Prev: ${log.previousHash.take(16)}...",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GovSlateMuted
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

