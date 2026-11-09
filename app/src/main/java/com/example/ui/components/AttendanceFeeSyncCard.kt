package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ModeStandby
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.work.WorkInfo
import com.example.data.remote.sync.AttendanceFeeSyncScheduler
import com.example.data.remote.sync.AttendanceFeeSyncWorker
import com.example.ui.theme.GovGreen
import com.example.ui.theme.GovNavyDark
import com.example.ui.theme.GovNavyPrimary
import com.example.ui.theme.GovRedAlert
import com.example.ui.theme.GovSaffron
import com.example.ui.theme.GovSlateDark
import com.example.ui.theme.GovSlateMuted
import com.example.ui.theme.GovSlateText
import com.example.ui.theme.PureWhite

/**
 * Material 3 / Glassmorphic UI Card for Monitoring and Managing WorkManager
 * Background Synchronization for Attendance & Offline Fee Logs.
 */
@Composable
fun AttendanceFeeSyncWorkManagerCard(
    modifier: Modifier = Modifier,
    institutionId: String = "INST-SXA-2026",
    testTag: String = "card_attendance_fee_sync_workmanager"
) {
    val context = LocalContext.current
    val periodicWorkInfo by remember {
        AttendanceFeeSyncScheduler.observePeriodicSyncWorkInfo(context)
    }.collectAsState(initial = null)

    val oneTimeWorkInfo by remember {
        AttendanceFeeSyncScheduler.observeOneTimeSyncWorkInfo(context)
    }.collectAsState(initial = null)

    var actionNotice by remember { mutableStateOf<String?>(null) }

    val activeState = oneTimeWorkInfo?.state ?: periodicWorkInfo?.state ?: WorkInfo.State.ENQUEUED
    val outputData = oneTimeWorkInfo?.outputData ?: periodicWorkInfo?.outputData

    val syncedAttendance = outputData?.getInt(AttendanceFeeSyncWorker.KEY_SYNCED_ATTENDANCE_COUNT, 0) ?: 0
    val syncedFees = outputData?.getInt(AttendanceFeeSyncWorker.KEY_SYNCED_FEE_COUNT, 0) ?: 0
    val formattedTime = outputData?.getString(AttendanceFeeSyncWorker.KEY_EXECUTION_TIME_FORMATTED) ?: "Awaiting initial execution"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Title & Strategy Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "WorkManager Sync",
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "WorkManager Offline-First Sync",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = GovNavyDark
                        )
                        Text(
                            text = "Attendance & Offline Fee Logs • $institutionId",
                            fontSize = 10.sp,
                            color = GovSlateMuted
                        )
                    }
                }

                // Status Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (activeState) {
                        WorkInfo.State.RUNNING -> Color(0xFFEFF6FF)
                        WorkInfo.State.SUCCEEDED -> Color(0xFFF0FDF4)
                        WorkInfo.State.FAILED -> Color(0xFFFEF2F2)
                        WorkInfo.State.CANCELLED -> Color(0xFFF1F5F9)
                        else -> Color(0xFFFFFBEB)
                    },
                    border = BorderStroke(
                        0.8.dp,
                        when (activeState) {
                            WorkInfo.State.RUNNING -> Color(0xFF93C5FD)
                            WorkInfo.State.SUCCEEDED -> Color(0xFF86EFAC)
                            WorkInfo.State.FAILED -> Color(0xFFFCA5A5)
                            WorkInfo.State.CANCELLED -> Color(0xFFCBD5E1)
                            else -> Color(0xFFFDE68A)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeState == WorkInfo.State.RUNNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 1.5.dp,
                                color = GovNavyPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(
                                imageVector = when (activeState) {
                                    WorkInfo.State.SUCCEEDED -> Icons.Default.CheckCircle
                                    WorkInfo.State.RUNNING -> Icons.Default.Sync
                                    else -> Icons.Default.HourglassTop
                                },
                                contentDescription = null,
                                tint = when (activeState) {
                                    WorkInfo.State.SUCCEEDED -> GovGreen
                                    WorkInfo.State.RUNNING -> GovNavyPrimary
                                    WorkInfo.State.FAILED -> GovRedAlert
                                    else -> Color(0xFFB45309)
                                },
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when (activeState) {
                                WorkInfo.State.RUNNING -> "SYNCING"
                                WorkInfo.State.SUCCEEDED -> "SYNCED"
                                WorkInfo.State.ENQUEUED -> "AWAITING NETWORK"
                                WorkInfo.State.BLOCKED -> "CONNECTIVITY PENDING"
                                WorkInfo.State.CANCELLED -> "PAUSED"
                                else -> "SCHEDULED"
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (activeState) {
                                WorkInfo.State.RUNNING -> GovNavyPrimary
                                WorkInfo.State.SUCCEEDED -> GovGreen
                                WorkInfo.State.FAILED -> GovRedAlert
                                else -> Color(0xFFB45309)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Policy Narrative Box
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Offline-First Cloud Sync Strategy:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Attendance and fee logs always save immediately to local Room database and the Durable Outbox without requiring network. Cloud sync requires NetworkType.CONNECTED only (no charging or device-idle gate). When internet reconnects, WorkManager automatically syncs pending mutations to the cloud.",
                        fontSize = 9.5.sp,
                        color = GovSlateText,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Constraint Pills Row
            Text(
                text = "WorkManager Synchronization Constraints:",
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = GovSlateMuted
            )
            Spacer(modifier = Modifier.height(5.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Offline First Room & Outbox Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(0.6.dp, Color(0xFF86EFAC)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = GovGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("OFFLINE-FIRST", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = GovGreen)
                            Text("ROOM & OUTBOX", fontSize = 7.5.sp, color = GovSlateDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // 2. Network Connected Constraint Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF5F3FF),
                    border = BorderStroke(0.6.dp, Color(0xFFDDD6FE)),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = Color(0xFF6D28D9),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("NETWORK.CONNECTED", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                            Text("CLOUD SYNC ONLY", fontSize = 7.5.sp, color = GovSlateDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // 3. Charging/Idle Not Gated Pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(0.6.dp, Color(0xFF93C5FD)),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = GovNavyPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text("AUTO-TRIGGER", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                            Text("ON RECONNECT", fontSize = 7.5.sp, color = GovSlateDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Telemetry stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Last Synced Output:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateMuted
                    )
                    Text(
                        text = "$syncedAttendance Attendance Rosters • $syncedFees Fee Logs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateDark
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Execution Timestamp:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = GovSlateMuted
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = GovSlateDark
                    )
                }
            }

            actionNotice?.let { notice ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notice,
                    fontSize = 9.5.sp,
                    color = GovNavyPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Schedule Periodic Network Sync
                Button(
                    onClick = {
                        AttendanceFeeSyncScheduler.schedulePeriodicSync(context)
                        actionNotice = "Periodic job scheduled (Runs every 6h whenever network is connected; offline data saved in Room)."
                        Toast.makeText(context, "Periodic sync enqueued with NetworkType.CONNECTED", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GovNavyPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_schedule_periodic_sync")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Schedule Periodic", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Trigger One-time Sync (Network Connected)
                OutlinedButton(
                    onClick = {
                        AttendanceFeeSyncScheduler.scheduleOneTimeSync(context, enforceIdleCharging = false)
                        actionNotice = "One-time sync enqueued: Will sync immediately once connected to internet."
                        Toast.makeText(context, "Job enqueued (Requires NetworkType.CONNECTED)", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, GovNavyPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("btn_trigger_onetime_idle_sync")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GovNavyPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Now (Net)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = GovNavyPrimary)
                }

                // 3. Cancel Button
                OutlinedButton(
                    onClick = {
                        AttendanceFeeSyncScheduler.cancelScheduledSync(context)
                        actionNotice = "Sync jobs cancelled."
                        Toast.makeText(context, "Cancelled WorkManager sync jobs", Toast.LENGTH_SHORT).show()
                    },
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_cancel_sync_jobs")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, tint = GovRedAlert, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
