package com.example.data.remote.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AuditLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Enterprise WorkManager Worker specifically designed for battery-optimized background
 * synchronization of attendance rosters and offline fee collections.
 *
 * This task is strictly scheduled to execute only when:
 * 1. Device is IDLE (no user interaction, screen off or ambient low-power state)
 * 2. Device is CHARGING (preserves battery cycle and prevents battery drain)
 * 3. Network is CONNECTED (enables secure synchronization to central cloud)
 */
class AttendanceFeeSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.schoolDao()
        val connectivity = NetworkConnectivityMonitor(applicationContext)
        val outboxManager = DurableOutboxManager(dao, connectivity)

        setProgress(
            workDataOf(
                PROGRESS_STATE to "SCANNING_OFFLINE_LOGS",
                KEY_BATTERY_STRATEGY to STRATEGY_IDLE_CHARGING
            )
        )

        executeSync(dao, outboxManager, runAttemptCount, "WORK-${id.toString().take(8)}")
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 3

        const val STRATEGY_IDLE_CHARGING = "IDLE_CHARGING_BATTERY_SAVER"
        const val PROGRESS_STATE = "PROGRESS_STATE"

        const val KEY_SYNCED_ATTENDANCE_COUNT = "KEY_SYNCED_ATTENDANCE_COUNT"
        const val KEY_SYNCED_FEE_COUNT = "KEY_SYNCED_FEE_COUNT"
        const val KEY_TOTAL_SYNCED = "KEY_TOTAL_SYNCED"
        const val KEY_FAIL_COUNT = "KEY_FAIL_COUNT"
        const val KEY_CONFLICT_COUNT = "KEY_CONFLICT_COUNT"
        const val KEY_BATTERY_STRATEGY = "KEY_BATTERY_STRATEGY"
        const val KEY_TIMESTAMP = "KEY_TIMESTAMP"
        const val KEY_EXECUTION_TIME_FORMATTED = "KEY_EXECUTION_TIME_FORMATTED"
        const val KEY_ERROR_MESSAGE = "KEY_ERROR_MESSAGE"

        internal suspend fun executeSync(
            dao: com.example.data.local.dao.SchoolDao,
            outboxManager: DurableOutboxManager,
            attemptCount: Int,
            workerEntityId: String = "WORK-MANUAL"
        ): Result = withContext(Dispatchers.IO) {
            try {
                val activeTenant = com.example.security.TenantSecurityContext.getActiveTenantIdOrNull()
                    ?: com.example.security.SecureSessionManager.getActiveSession()?.institutionId
                val tenantIds = (dao.getPendingOutboxInstitutionIds() + listOfNotNull(activeTenant))
                    .filter { it.isNotBlank() }
                    .distinct()

                val pendingAttendanceEntities = listOf("ATTENDANCE", "ATTENDANCE_RECORD", "REGISTER_SCAN")
                val pendingFeeEntities = listOf("FEE_RECORD", "FEE_PAYMENT", "FEE_TRANSACTION", "STUDENT_FEES")

                var totalAckCount = 0
                var totalFailCount = 0
                var totalConflictCount = 0
                var anyBatchFailure = false
                val allDetails = mutableListOf<String>()

                for (tenantId in tenantIds) {
                    val syncResult = outboxManager.flushQueue(institutionId = tenantId, forceAll = true)
                    if (syncResult.isFailure) {
                        anyBatchFailure = true
                    }
                    val batch = syncResult.getOrNull()
                    if (batch != null) {
                        totalAckCount += batch.acknowledgedCount
                        totalFailCount += batch.failedCount
                        totalConflictCount += batch.conflictCount
                        allDetails.addAll(batch.details)
                    }
                }

                val attendanceSynced = allDetails.count { detail ->
                    detail.startsWith("[ACK]") &&
                        pendingAttendanceEntities.any { detail.contains(it, ignoreCase = true) }
                }
                val feesSynced = allDetails.count { detail ->
                    detail.startsWith("[ACK]") &&
                        pendingFeeEntities.any { detail.contains(it, ignoreCase = true) }
                }

                val timestamp = System.currentTimeMillis()
                val timeString = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))

                if (tenantIds.isNotEmpty() && (totalAckCount > 0 || totalFailCount > 0 || totalConflictCount > 0)) {
                    dao.insertAuditLog(
                        AuditLogEntity(
                            institutionId = activeTenant ?: tenantIds.first(),
                            timestamp = timestamp,
                            formattedTime = timeString,
                            action = "OFFLINE_FIRST_BACKGROUND_SYNC",
                            entityType = "ATTENDANCE_AND_OFFLINE_FEE_LOGS",
                            entityId = workerEntityId,
                            actorUserId = "SYSTEM_WORKMANAGER",
                            actorName = "WorkManager Daemon",
                            actorRole = "SYSTEM_WORKER",
                            previousState = "OFFLINE_QUEUED",
                            newState = if (!anyBatchFailure && totalFailCount == 0) "SYNCHRONIZED_TO_CLOUD" else "PARTIALLY_SYNCED",
                            remarks = "WorkManager executed on Network Connected state. Synced: $attendanceSynced attendance logs, $feesSynced fee transactions ($totalAckCount total ack, $totalFailCount failed, $totalConflictCount conflicts) at $timeString",
                            syncStatus = if (!anyBatchFailure && totalFailCount == 0) "SYNCHRONIZED" else "PENDING_RETRY"
                        )
                    )
                }

                val outputData = workDataOf(
                    KEY_SYNCED_ATTENDANCE_COUNT to attendanceSynced,
                    KEY_SYNCED_FEE_COUNT to feesSynced,
                    KEY_TOTAL_SYNCED to totalAckCount,
                    KEY_FAIL_COUNT to totalFailCount,
                    KEY_CONFLICT_COUNT to totalConflictCount,
                    KEY_BATTERY_STRATEGY to STRATEGY_IDLE_CHARGING,
                    KEY_TIMESTAMP to timestamp,
                    KEY_EXECUTION_TIME_FORMATTED to timeString
                )

                if (!anyBatchFailure && totalFailCount == 0) {
                    Result.success(outputData)
                } else if (attemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure(outputData)
                }
            } catch (e: Exception) {
                if (attemptCount < MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    val errorData = workDataOf(
                        KEY_ERROR_MESSAGE to (e.localizedMessage ?: "Unknown sync error"),
                        KEY_BATTERY_STRATEGY to STRATEGY_IDLE_CHARGING
                    )
                    Result.failure(errorData)
                }
            }
        }
    }
}
