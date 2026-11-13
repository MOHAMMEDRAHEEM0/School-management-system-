package com.example.data.remote.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import java.util.concurrent.TimeUnit

/**
 * Production WorkManager Worker for Resilient Background Outbox Synchronization.
 *
 * Guarantees that pending mutations survive process death, device reboots, and network switches.
 * Configured with connected network constraints.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.schoolDao()
        val connectivity = NetworkConnectivityMonitor(applicationContext)
        val outboxManager = DurableOutboxManager(dao, connectivity)
        return executeSync(dao, outboxManager, runAttemptCount)
    }

    companion object {
        private const val PERIODIC_WORK_TAG = "SCHOOL_OUTBOX_PERIODIC_SYNC"
        private const val EXPEDITED_WORK_TAG = "SCHOOL_OUTBOX_IMMEDIATE_FLUSH"

        internal suspend fun executeSync(
            dao: com.example.data.local.dao.SchoolDao,
            outboxManager: DurableOutboxManager,
            attemptCount: Int
        ): Result {
            return try {
                val activeTenant = com.example.security.TenantSecurityContext.getActiveTenantIdOrNull()
                    ?: com.example.security.SecureSessionManager.getActiveSession()?.institutionId
                val tenantIds = (dao.getPendingOutboxInstitutionIds() + listOfNotNull(activeTenant))
                    .filter { it.isNotBlank() }
                    .distinct()

                var anyFailure = false
                for (tenantId in tenantIds) {
                    val syncResult = outboxManager.flushQueue(institutionId = tenantId, forceAll = false)
                    val failedCount = syncResult.getOrNull()?.failedCount ?: 0
                    if (syncResult.isFailure || failedCount > 0) {
                        anyFailure = true
                    }
                }

                if (!anyFailure) {
                    Result.success()
                } else if (attemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            } catch (e: Exception) {
                if (attemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }

        /**
         * Schedules periodic background sync every 15 minutes whenever connected to the internet.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<CloudSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }

        /**
         * Triggers an immediate one-time outbox synchronization when network connectivity is detected
         * or when user requests manual flush.
         */
        fun triggerImmediateFlush(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                EXPEDITED_WORK_TAG,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        }
    }
}
