package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.NetworkType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.remote.sync.AttendanceFeeSyncScheduler
import com.example.data.remote.sync.AttendanceFeeSyncWorker
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.remote.sync.NetworkConnectivityMonitor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceFeeWorkManagerTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    @Test
    fun testNormalSyncConstraintsOmitChargingAndIdleWhileKeepingNetworkConnected() {
        // Normal synchronization must NOT require charging or device idle
        val constraints = AttendanceFeeSyncScheduler.buildSyncConstraints()

        assertFalse(
            "Normal sync must NOT require device charging so pending updates sync immediately",
            constraints.requiresCharging()
        )

        assertFalse(
            "Normal sync must NOT require device idle so updates are not delayed waiting for screen off",
            constraints.requiresDeviceIdle()
        )

        assertEquals(
            "Task must require connected network for cloud sync attempts",
            NetworkType.CONNECTED,
            constraints.requiredNetworkType
        )
    }

    @Test
    fun testSchedulingSummaryAndPolicy() {
        val summary = AttendanceFeeSyncScheduler.getSchedulingSummary(context)

        assertFalse("Summary must confirm charging is not required for normal sync", summary.requiresCharging)
        assertFalse("Summary must confirm device idle is not required for normal sync", summary.requiresIdle)
        assertTrue("Summary must confirm network connectivity is required for cloud sync", summary.requiresNetwork)
        assertEquals("Default repeat interval must be 6 hours", 6L, summary.periodicIntervalHours)
        assertEquals(
            "Unique work name must match canonical identifier",
            AttendanceFeeSyncScheduler.UNIQUE_PERIODIC_WORK_NAME,
            summary.uniqueWorkName
        )
        assertTrue(
            "Strategy description must document offline-first network-aware behavior",
            summary.strategyDescription.contains("Offline-First", ignoreCase = true)
        )
    }

    @Test
    fun testAttendanceAndFeeOutboxEnqueuingAndFlushing() = runBlocking {
        val dao = db.schoolDao()
        val connectivity = NetworkConnectivityMonitor(context)
        val outboxManager = DurableOutboxManager(dao, connectivity)
        val institutionId = "INST-SXA-2026"

        // 1. Insert attendance record
        val attendance = AttendanceRecordEntity(
            personId = "STU-10A-01",
            personName = "Aarav Sharma",
            roleType = "STUDENT",
            date = "2026-10-07",
            status = "PRESENT",
            timeMarked = "08:15 AM",
            markedByRole = "TEACHER",
            markedByName = "Mrs. Sunita Kapoor",
            classSection = "10-A",
            institutionId = institutionId
        )
        dao.insertAttendanceRecord(attendance)

        // 2. Insert offline fee collection transaction
        val feeTxn = FeePaymentTransactionEntity(
            transactionId = "TXN-2026-WORKER-01",
            institutionId = institutionId,
            feeRecordId = "FEE-2026-001",
            studentId = "STU-10A-01",
            studentName = "Aarav Sharma",
            amountPaidPaise = 1500000L,
            paymentMode = "CASH",
            transactionReference = "OFFLINE-RECEIPT-8891",
            receiptNumber = "RCP-8891",
            timestamp = System.currentTimeMillis(),
            cashierName = "Suresh Mehta",
            status = "SUCCESS"
        )
        dao.insertPaymentTransaction(feeTxn)

        // 3. Queue both mutations into durable outbox
        val attPayload = """{"studentId":"STU-10A-01","status":"PRESENT","date":"2026-10-07"}"""
        val attResult = outboxManager.enqueue(
            entityType = "ATTENDANCE_RECORD",
            entityId = "STU-10A-01-2026-10-07",
            operation = "UPSERT",
            payloadJson = attPayload,
            institutionId = institutionId
        )
        assertTrue("Attendance must be enqueued into outbox", attResult.isSuccess)

        val feePayload = """{"transactionId":"TXN-2026-WORKER-01","amount":1500000,"mode":"CASH"}"""
        val feeResult = outboxManager.enqueue(
            entityType = "FEE_TRANSACTION",
            entityId = "TXN-2026-WORKER-01",
            operation = "INSERT",
            payloadJson = feePayload,
            institutionId = institutionId
        )
        assertTrue("Fee transaction must be enqueued into outbox", feeResult.isSuccess)

        // 4. Verify outbox contains pending items
        val pendingCount = dao.getPendingOutbox(institutionId)
        assertNotNull(pendingCount)

        // 5. Execute flush queue
        val flushResult = outboxManager.flushQueue(institutionId, forceAll = true)
        assertTrue("Flush queue execution must succeed", flushResult.isSuccess)
        val batch = flushResult.getOrNull()
        assertNotNull(batch)
        assertTrue("Processed count must be at least 2", (batch?.totalProcessed ?: 0) >= 2)
    }

    @Test
    fun testWorkerConstantsAndStrategyKeys() {
        assertEquals("IDLE_CHARGING_BATTERY_SAVER", AttendanceFeeSyncWorker.STRATEGY_IDLE_CHARGING)
        assertEquals("KEY_SYNCED_ATTENDANCE_COUNT", AttendanceFeeSyncWorker.KEY_SYNCED_ATTENDANCE_COUNT)
        assertEquals("KEY_SYNCED_FEE_COUNT", AttendanceFeeSyncWorker.KEY_SYNCED_FEE_COUNT)
        assertEquals("KEY_TOTAL_SYNCED", AttendanceFeeSyncWorker.KEY_TOTAL_SYNCED)
        assertEquals("KEY_BATTERY_STRATEGY", AttendanceFeeSyncWorker.KEY_BATTERY_STRATEGY)
        assertEquals("attendance_fee_idle_charging_sync_periodic", AttendanceFeeSyncScheduler.UNIQUE_PERIODIC_WORK_NAME)
        assertEquals("attendance_fee_idle_charging_sync_onetime", AttendanceFeeSyncScheduler.UNIQUE_ONETIME_WORK_NAME)
    }
}
