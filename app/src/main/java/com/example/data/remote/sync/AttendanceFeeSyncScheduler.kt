package com.example.data.remote.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Summary telemetry data for the WorkManager job scheduling strategy.
 */
data class AttendanceFeeSyncScheduleSummary(
    val isPeriodicScheduled: Boolean,
    val requiresCharging: Boolean,
    val requiresIdle: Boolean,
    val requiresNetwork: Boolean,
    val periodicIntervalHours: Long,
    val uniqueWorkName: String,
    val tag: String,
    val strategyDescription: String
)

/**
 * Enterprise WorkManager Job Scheduling Strategy for Attendance and Offline Fee Logs.
 *
 * Offline-First Cloud Synchronization Architecture:
 * - Offline operations (marking attendance, collecting fees) ALWAYS save immediately to local Room
 *   database and the Durable Outbox without requiring network connectivity.
 * - Device-idle and charging constraints are removed from normal synchronization so updates are
 *   not delayed waiting for charging/idle states.
 * - Requires NetworkType.CONNECTED: This constraint applies solely to the cloud synchronization attempt,
 *   never gating or delaying the offline-first operations.
 * - As soon as network connectivity becomes available, WorkManager automatically triggers background
 *   synchronization of all pending mutations from the Durable Outbox to central cloud / Firestore.
 * - Exponential Backoff: Protects against transient network drops and server throttling.
 */
object AttendanceFeeSyncScheduler {

    const val UNIQUE_PERIODIC_WORK_NAME = "attendance_fee_idle_charging_sync_periodic"
    const val UNIQUE_ONETIME_WORK_NAME = "attendance_fee_idle_charging_sync_onetime"
    const val TAG_ATTENDANCE_FEE_SYNC = "ATTENDANCE_FEE_BATTERY_SAVER_SYNC"

    const val DEFAULT_REPEAT_INTERVAL_HOURS = 6L
    const val DEFAULT_FLEX_INTERVAL_MINUTES = 30L

    /**
     * Builds standard cloud synchronization constraints requiring NetworkType.CONNECTED.
     * Charging and device-idle requirements are omitted so that pending offline attendance
     * and fee logs synchronize automatically as soon as internet connectivity is available.
     */
    fun buildSyncConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Builds standard network-connected constraints for cloud synchronization.
     * Offline operations are never blocked; this constraint only gates the background cloud sync.
     */
    fun buildNetworkConnectedConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Optional strict battery-saver constraints if explicitly requested for low-power maintenance.
     */
    fun buildIdleChargingConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    /**
     * Enqueues the authoritative periodic background synchronization job for attendance and fee logs.
     * Configured with NetworkType.CONNECTED constraint only (no charging or idle restrictions),
     * ensuring pending offline operations sync promptly whenever the device has internet access.
     *
     * @param context Application context
     * @param repeatIntervalHours Interval in hours (default 6 hours)
     * @param flexIntervalMinutes Flex window in minutes (default 30 mins)
     * @param policy Work policy (default KEEP to preserve scheduled execution cycle)
     */
    fun schedulePeriodicSync(
        context: Context,
        repeatIntervalHours: Long = DEFAULT_REPEAT_INTERVAL_HOURS,
        flexIntervalMinutes: Long = DEFAULT_FLEX_INTERVAL_MINUTES,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    ) {
        val constraints = buildSyncConstraints()

        val periodicRequest = PeriodicWorkRequestBuilder<AttendanceFeeSyncWorker>(
            repeatInterval = repeatIntervalHours,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = flexIntervalMinutes,
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_ATTENDANCE_FEE_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK_NAME,
            policy,
            periodicRequest
        )
    }

    /**
     * Schedules a one-time background synchronization execution for pending attendance and fee records.
     * By default uses NetworkType.CONNECTED constraint so pending records sync immediately upon connectivity.
     *
     * @param context Application context
     * @param enforceIdleCharging When false (default), runs with NetworkType.CONNECTED only.
     */
    fun scheduleOneTimeSync(
        context: Context,
        enforceIdleCharging: Boolean = false
    ) {
        val constraints = if (enforceIdleCharging) {
            buildIdleChargingConstraints()
        } else {
            buildSyncConstraints()
        }

        val oneTimeRequest = OneTimeWorkRequestBuilder<AttendanceFeeSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(TAG_ATTENDANCE_FEE_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONETIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    /**
     * Cancels all scheduled periodic and one-time attendance & fee sync jobs.
     */
    fun cancelScheduledSync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(UNIQUE_ONETIME_WORK_NAME)
        workManager.cancelAllWorkByTag(TAG_ATTENDANCE_FEE_SYNC)
    }

    /**
     * Observes the WorkInfo stream for the periodic synchronization job.
     */
    fun observePeriodicSyncWorkInfo(context: Context): Flow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(UNIQUE_PERIODIC_WORK_NAME)
            .map { it.firstOrNull() }
    }

    /**
     * Observes the WorkInfo stream for the one-time synchronization job.
     */
    fun observeOneTimeSyncWorkInfo(context: Context): Flow<WorkInfo?> {
        return WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(UNIQUE_ONETIME_WORK_NAME)
            .map { it.firstOrNull() }
    }

    /**
     * Returns a structured summary of the scheduling configuration.
     */
    fun getSchedulingSummary(context: Context): AttendanceFeeSyncScheduleSummary {
        val normalConstraints = buildSyncConstraints()
        return AttendanceFeeSyncScheduleSummary(
            isPeriodicScheduled = true,
            requiresCharging = normalConstraints.requiresCharging(),
            requiresIdle = normalConstraints.requiresDeviceIdle(),
            requiresNetwork = normalConstraints.requiredNetworkType == NetworkType.CONNECTED,
            periodicIntervalHours = DEFAULT_REPEAT_INTERVAL_HOURS,
            uniqueWorkName = UNIQUE_PERIODIC_WORK_NAME,
            tag = TAG_ATTENDANCE_FEE_SYNC,
            strategyDescription = "Offline-First Network-Aware Sync (Runs when connected to sync Room & Outbox records; no charging/idle gate)"
        )
    }
}
