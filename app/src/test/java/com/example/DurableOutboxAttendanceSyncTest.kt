package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.remote.sync.CloudOutboxDispatcher
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.SchoolRepository
import com.example.data.repository.UserSession
import com.example.security.SecureSessionManager
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DurableOutboxAttendanceSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var outboxManager: DurableOutboxManager
    private lateinit var attendanceRepository: AttendanceRepositoryImpl
    private lateinit var schoolRepository: SchoolRepository

    private val tenantAlpha = "INST-ALPHA-2026"
    private val tenantBeta = "INST-BETA-2026"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        outboxManager = DurableOutboxManager(
            dao = db.schoolDao(),
            defaultInstitutionId = null
        )

        attendanceRepository = AttendanceRepositoryImpl(
            dao = db.schoolDao(),
            outboxManager = outboxManager
        )

        schoolRepository = SchoolRepository(
            dao = db.schoolDao(),
            outboxManager = outboxManager,
            attendanceRepository = attendanceRepository
        )

        // Set default active tenant session for tests
        setSessionForTenant(tenantAlpha)
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        db.close()
    }

    private fun setSessionForTenant(tenantId: String) {
        val user = UserEntity(
            id = "USR-$tenantId-01",
            institutionId = tenantId,
            username = "teacher_$tenantId",
            email = "teacher@$tenantId.edu",
            displayName = "Teacher $tenantId",
            role = UserRole.TEACHER.name,
            password = "",
            passwordSalt = "",
            passwordHash = ""
        )
        val claims = SecurityClaims(
            userId = user.id,
            role = UserRole.TEACHER,
            roleClaim = UserRole.TEACHER.name,
            displayName = user.displayName,
            email = user.email,
            permissions = setOf(com.example.data.model.SchoolPermissions.MARK_ATTENDANCE_SECTION),
            sessionToken = "TEST-TOKEN-$tenantId"
        )
        val session = UserSession(
            userId = user.id,
            institutionId = tenantId,
            email = user.email,
            displayName = user.displayName,
            role = UserRole.TEACHER,
            sessionToken = "TEST-TOKEN-$tenantId",
            claims = claims
        )
        SecureSessionManager.saveSession(session)
    }

    // =========================================================================
    // 1. ATOMIC WRITES (Room mutation + Outbox in the same transaction)
    // =========================================================================

    @Test
    fun testDailyAttendanceSavedAtomicallyWithOutbox() = runBlocking {
        val dateKey = "2026-10-10"
        val classSection = "10-A"
        val studentId = "STU-001"

        val attendance = AttendanceEntity(
            institutionId = tenantAlpha,
            studentId = studentId,
            studentName = "Aarav Sharma",
            classSection = classSection,
            date = dateKey,
            dateTimestamp = System.currentTimeMillis(),
            status = "PRESENT",
            session = "Morning Assembly",
            markedByTeacherId = "FAC-01",
            remarks = "Present on time"
        )

        // Save batch attendance
        attendanceRepository.saveDailyAttendanceBatch(
            classSection = classSection,
            date = dateKey,
            attendances = listOf(attendance),
            updateStudentPercentages = false
        )

        // 1. Verify Room attendance entity was persisted
        val savedAttendance = db.schoolDao().getAttendanceForClassAndDate(classSection, dateKey)
        assertEquals(1, savedAttendance.size)
        assertEquals("PRESENT", savedAttendance[0].status)
        assertEquals(tenantAlpha, savedAttendance[0].institutionId)

        // 2. Verify mirrored attendance_record entity was persisted with isSyncedToCentral = false
        val savedRecords = db.schoolDao().getAllAttendanceRecords().first()
        assertEquals(1, savedRecords.size)
        assertEquals(studentId, savedRecords[0].personId)
        assertFalse("Attendance record must not claim central sync yet", savedRecords[0].isSyncedToCentral)
        assertEquals(tenantAlpha, savedRecords[0].institutionId)

        // 3. Verify outbox entry was persisted atomically
        val outboxItems = db.schoolDao().getPendingOutbox(tenantAlpha).first()
        assertEquals(1, outboxItems.size)
        val outbox = outboxItems[0]

        // Check required fields per architecture spec
        val expectedEntityId = "${classSection}_${dateKey}_$studentId"
        val expectedIdempotencyKey = "ATT-$tenantAlpha-$expectedEntityId"
        assertEquals(expectedIdempotencyKey, outbox.idempotencyKey)
        assertEquals("ATTENDANCE_RECORD", outbox.entityType)
        assertEquals(expectedEntityId, outbox.entityId)
        assertEquals(tenantAlpha, outbox.institutionId)
        assertEquals(1, outbox.payloadVersion)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, outbox.status)
        assertEquals(0, outbox.retryCount)
        assertTrue("Checksum must be 64-char SHA-256", outbox.payloadChecksum.length == 64)
        assertEquals(outboxManager.computeSha256(outbox.payloadJson), outbox.payloadChecksum)
        assertTrue(outbox.createdAt > 0)
    }

    @Test
    fun testRegisterScanCommittedAtomicallyWithOutbox() = runBlocking {
        val batch = RegisterScanBatchEntity(
            batchId = "BATCH-SCAN-01",
            institutionId = tenantAlpha,
            classSection = "10-B",
            session = "Morning",
            date = "2026-10-10",
            teacherName = "Mrs. Sunita Kapoor",
            totalStudents = 1,
            presentCount = 1,
            absentCount = 0,
            absentRolls = "",
            inchargeVerified = false,
            principalApproved = false,
            timestamp = System.currentTimeMillis(),
            status = "PENDING_VERIFICATION"
        )

        val record = AttendanceRecordEntity(
            institutionId = tenantAlpha,
            personId = "STU-002",
            personName = "Diya Sengupta",
            roleType = "STUDENT",
            date = "2026-10-10",
            status = "PRESENT",
            timeMarked = "08:30 AM",
            classSection = "10-B",
            isSyncedToCentral = false
        )

        attendanceRepository.commitRegisterScan(batch, listOf(record))

        // 1. Verify batch in Room
        val savedBatches = db.schoolDao().getAllRegisterBatches().first()
        assertEquals(1, savedBatches.size)
        assertEquals("BATCH-SCAN-01", savedBatches[0].batchId)
        assertFalse(savedBatches[0].inchargeVerified)
        assertFalse(savedBatches[0].principalApproved)

        // 2. Verify attendance record in Room
        val savedRecords = db.schoolDao().getAllAttendanceRecords().first()
        assertEquals(1, savedRecords.size)
        assertFalse(savedRecords[0].isSyncedToCentral)

        // 3. Verify outbox contains both batch and record entries
        val outboxItems = db.schoolDao().getPendingOutbox(tenantAlpha).first()
        assertEquals(2, outboxItems.size)

        val batchOutbox = outboxItems.find { it.entityType == "REGISTER_SCAN_BATCH" }
        assertNotNull(batchOutbox)
        assertEquals("BATCH-$tenantAlpha-BATCH-SCAN-01", batchOutbox?.idempotencyKey)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, batchOutbox?.status)

        val recordOutbox = outboxItems.find { it.entityType == "ATTENDANCE_RECORD" }
        assertNotNull(recordOutbox)
        assertEquals("SCAN-$tenantAlpha-BATCH-SCAN-01_STU-002", recordOutbox?.idempotencyKey)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, recordOutbox?.status)
    }

    @Test
    fun testAttendanceVerificationAndApprovalAtomicallyQueuedInOutbox() = runBlocking {
        val batch = RegisterScanBatchEntity(
            batchId = "BATCH-VERIFY-01",
            institutionId = tenantAlpha,
            classSection = "10-C",
            session = "Morning",
            date = "2026-10-10",
            teacherName = "Mr. Vance",
            totalStudents = 1,
            presentCount = 1,
            absentCount = 0,
            absentRolls = "",
            inchargeVerified = false,
            principalApproved = false
        )
        db.schoolDao().insertRegisterBatch(batch)

        // 1. In-charge verification
        attendanceRepository.verifyBatchByIncharge("BATCH-VERIFY-01")

        val batchAfterVerify = db.schoolDao().getAllRegisterBatches().first().find { it.batchId == "BATCH-VERIFY-01" }
        assertTrue(batchAfterVerify?.inchargeVerified == true)

        val verifyOutbox = db.schoolDao().getOutboxByIdempotencyKey("VERIFY-BATCH-$tenantAlpha-BATCH-VERIFY-01")
        assertNotNull(verifyOutbox)
        assertEquals("ATTENDANCE_BATCH_VERIFICATION", verifyOutbox?.entityType)
        assertEquals("UPDATE", verifyOutbox?.operation)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, verifyOutbox?.status)

        // 2. Principal approval
        attendanceRepository.approveBatchByPrincipal("BATCH-VERIFY-01")

        val batchAfterApprove = db.schoolDao().getAllRegisterBatches().first().find { it.batchId == "BATCH-VERIFY-01" }
        assertTrue(batchAfterApprove?.principalApproved == true)

        val approveOutbox = db.schoolDao().getOutboxByIdempotencyKey("APPROVE-BATCH-$tenantAlpha-BATCH-VERIFY-01")
        assertNotNull(approveOutbox)
        assertEquals("ATTENDANCE_BATCH_APPROVAL", approveOutbox?.entityType)
        assertEquals("UPDATE", approveOutbox?.operation)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, approveOutbox?.status)
    }

    @Test
    fun testSchoolRepositoryDelegatesToAttendanceRepositoryWithOutbox() = runBlocking {
        val batch = RegisterScanBatchEntity(
            batchId = "BATCH-SR-01",
            institutionId = tenantAlpha,
            classSection = "10-D",
            session = "Morning",
            date = "2026-10-10",
            teacherName = "Mrs. Sunita Kapoor",
            totalStudents = 1,
            presentCount = 1,
            absentCount = 0,
            absentRolls = ""
        )
        val record = AttendanceRecordEntity(
            institutionId = tenantAlpha,
            personId = "STU-003",
            personName = "Rohan Malhotra",
            roleType = "STUDENT",
            date = "2026-10-10",
            status = "PRESENT",
            timeMarked = "08:15 AM",
            classSection = "10-D"
        )

        // Execute via SchoolRepository
        schoolRepository.commitRegisterScan(batch, listOf(record))
        schoolRepository.inchargeVerifyBatch("BATCH-SR-01")
        schoolRepository.principalApproveBatch("BATCH-SR-01")

        // Verify outbox contains all mutations
        val outboxItems = db.schoolDao().getPendingOutbox(tenantAlpha).first()
        assertTrue(outboxItems.any { it.idempotencyKey == "BATCH-$tenantAlpha-BATCH-SR-01" })
        assertTrue(outboxItems.any { it.idempotencyKey == "SCAN-$tenantAlpha-BATCH-SR-01_STU-003" })
        assertTrue(outboxItems.any { it.idempotencyKey == "VERIFY-BATCH-$tenantAlpha-BATCH-SR-01" })
        assertTrue(outboxItems.any { it.idempotencyKey == "APPROVE-BATCH-$tenantAlpha-BATCH-SR-01" })
    }

    // =========================================================================
    // 2. OFFLINE PERSISTENCE & NO FAKE ACKS
    // =========================================================================

    @Test
    fun testOfflineMutationsRemainPendingWithoutFakeAcks() = runBlocking {
        // Dispatcher simulating offline or failed cloud backend
        val offlineDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                return DurableOutboxManager.CloudDispatchResult.TransientError(
                    "Cloud network offline. Mutation retained in Room."
                )
            }
        }

        val testOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = offlineDispatcher
        )

        val attendance = AttendanceEntity(
            institutionId = tenantAlpha,
            studentId = "STU-OFFLINE",
            studentName = "Offline Scholar",
            classSection = "10-A",
            date = "2026-10-10",
            status = "PRESENT"
        )
        val repo = AttendanceRepositoryImpl(db.schoolDao(), testOutbox)
        repo.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(attendance))

        // Attempt queue flush
        val flushResult = testOutbox.flushQueue(tenantAlpha, forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batchResult = flushResult.getOrThrow()

        // NO FAKE ACK: Acknowledged count must be 0!
        assertEquals(0, batchResult.acknowledgedCount)
        assertEquals(1, batchResult.failedCount)

        // Mutation remains in recoverable RETRYABLE_FAILURE / PENDING state
        val outboxItem = db.schoolDao().getOutboxByIdempotencyKey("ATT-$tenantAlpha-10-A_2026-10-10_STU-OFFLINE")
        assertNotNull(outboxItem)
        assertEquals(DurableOutboxEntity.STATUS_RETRYABLE_FAILURE, outboxItem?.status)
        assertNull("Server ack token must never be issued on failed or offline write", outboxItem?.serverAckToken)
        assertEquals(1, outboxItem?.retryCount)
    }

    @Test
    fun testOnlyConfirmedFirestoreWriteIsMarkedAcknowledged() = runBlocking {
        val confirmedToken = "ACK-FS-SXA-CONFIRMED-99"
        val serverTime = System.currentTimeMillis()

        val confirmedDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                return DurableOutboxManager.CloudDispatchResult.Success(
                    ackToken = confirmedToken,
                    serverTimestamp = serverTime
                )
            }
        }

        val testOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = confirmedDispatcher
        )

        val attendance = AttendanceEntity(
            institutionId = tenantAlpha,
            studentId = "STU-ONLINE",
            studentName = "Online Scholar",
            classSection = "10-A",
            date = "2026-10-10",
            status = "PRESENT"
        )
        val repo = AttendanceRepositoryImpl(db.schoolDao(), testOutbox)
        repo.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(attendance))

        // Flush queue
        val flushResult = testOutbox.flushQueue(tenantAlpha, forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batchResult = flushResult.getOrThrow()

        assertEquals(1, batchResult.acknowledgedCount)
        assertEquals(0, batchResult.failedCount)

        val outboxItem = db.schoolDao().getOutboxByIdempotencyKey("ATT-$tenantAlpha-10-A_2026-10-10_STU-ONLINE")
        assertNotNull(outboxItem)
        assertEquals(DurableOutboxEntity.STATUS_ACKNOWLEDGED, outboxItem?.status)
        assertEquals(confirmedToken, outboxItem?.serverAckToken)
        assertEquals(serverTime, outboxItem?.serverAckTimestamp)
    }

    // =========================================================================
    // 3. DUPLICATE PREVENTION & STABLE IDEMPOTENCY
    // =========================================================================

    @Test
    fun testStableIdempotencyKeyPreventsDuplicateOutboxItems() = runBlocking {
        val attendance = AttendanceEntity(
            institutionId = tenantAlpha,
            studentId = "STU-DUP-01",
            studentName = "Duplicate Test Scholar",
            classSection = "10-A",
            date = "2026-10-10",
            status = "PRESENT"
        )

        // Save twice
        attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(attendance))
        attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(attendance))

        // Should not have duplicated records
        val outboxItems = db.schoolDao().getPendingOutbox(tenantAlpha).first()
        val studentOutbox = outboxItems.filter { it.entityId == "10-A_2026-10-10_STU-DUP-01" }
        assertEquals(1, studentOutbox.size)
    }

    @Test
    fun testChecksumTamperingDetectedAndRejected() = runBlocking {
        val attendance = AttendanceEntity(
            institutionId = tenantAlpha,
            studentId = "STU-INTEGRITY",
            studentName = "Integrity Scholar",
            classSection = "10-A",
            date = "2026-10-10",
            status = "PRESENT"
        )
        attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(attendance))

        val key = "ATT-$tenantAlpha-10-A_2026-10-10_STU-INTEGRITY"
        val original = db.schoolDao().getOutboxByIdempotencyKey(key)
        assertNotNull(original)

        // Tamper payload locally without updating checksum
        val tampered = original!!.copy(payloadJson = original.payloadJson.replace("PRESENT", "ABSENT"))
        db.schoolDao().updateOutbox(tampered)

        // Flush queue
        val flushResult = outboxManager.flushQueue(tenantAlpha, forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batch = flushResult.getOrThrow()

        // Must be flagged as Conflict due to checksum mismatch
        assertEquals(1, batch.conflictCount)
        val updated = db.schoolDao().getOutboxByIdempotencyKey(key)
        assertEquals(DurableOutboxEntity.STATUS_CONFLICT, updated?.status)
        assertTrue(updated?.errorMessage?.contains("Checksum mismatch") == true)
    }

    // =========================================================================
    // 4. RETRIES & EXPONENTIAL BACKOFF
    // =========================================================================

    @Test
    fun testRetriesIncrementAndTriggerPermanentFailureOnMaxRetries() = runBlocking {
        val failingDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                return DurableOutboxManager.CloudDispatchResult.TransientError("Transient 503 Service Unavailable")
            }
        }
        val testOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = failingDispatcher
        )

        val itemResult = testOutbox.enqueue(
            entityType = "ATTENDANCE_RECORD",
            entityId = "REC-RETRY-01",
            operation = "CREATE",
            payloadJson = """{"studentId":"STU-01","status":"PRESENT"}""",
            institutionId = tenantAlpha,
            customIdempotencyKey = "IDEM-RETRY-TEST-01"
        )
        assertTrue(itemResult.isSuccess)

        // Attempt flush 1
        testOutbox.flushQueue(tenantAlpha, forceAll = true)
        var item = db.schoolDao().getOutboxByIdempotencyKey("IDEM-RETRY-TEST-01")
        assertEquals(1, item?.retryCount)
        assertEquals(DurableOutboxEntity.STATUS_RETRYABLE_FAILURE, item?.status)

        // Simulate 4 more retries to reach maxRetries (5)
        for (i in 2..5) {
            testOutbox.flushQueue(tenantAlpha, forceAll = true)
        }

        item = db.schoolDao().getOutboxByIdempotencyKey("IDEM-RETRY-TEST-01")
        assertEquals(5, item?.retryCount)
        assertEquals(DurableOutboxEntity.STATUS_PERMANENT_FAILURE, item?.status)
        assertTrue(item?.errorMessage?.contains("Exceeded max retries") == true)
    }

    // =========================================================================
    // 5. TENANT ISOLATION
    // =========================================================================

    @Test
    fun testTenantResolvedFromAuthenticatedSessionNotHardcoded() = runBlocking {
        setSessionForTenant("INST-XYZ-CUSTOM")

        val attendance = AttendanceEntity(
            studentId = "STU-XYZ-01",
            studentName = "Custom Scholar",
            classSection = "5-A",
            date = "2026-10-10",
            status = "PRESENT"
        )

        attendanceRepository.saveDailyAttendanceBatch("5-A", "2026-10-10", listOf(attendance))

        // Verify records were tagged with authenticated tenant, NOT hardcoded "INST-SXA-2026"
        val savedAttendance = db.schoolDao().getAttendanceForClassAndDate("5-A", "2026-10-10")
        assertEquals(1, savedAttendance.size)
        assertEquals("INST-XYZ-CUSTOM", savedAttendance[0].institutionId)

        val outbox = db.schoolDao().getPendingOutbox("INST-XYZ-CUSTOM").first()
        assertEquals(1, outbox.size)
        assertEquals("INST-XYZ-CUSTOM", outbox[0].institutionId)
        assertTrue(outbox[0].idempotencyKey.startsWith("ATT-INST-XYZ-CUSTOM-"))
    }

    @Test
    fun testCrossTenantAttemptThrowsSecurityException() = runBlocking {
        // Active session is tenantAlpha (INST-ALPHA-2026)
        val foreignAttendance = AttendanceEntity(
            institutionId = tenantBeta, // INST-BETA-2026
            studentId = "STU-BETA-01",
            studentName = "Beta Scholar",
            classSection = "10-A",
            date = "2026-10-10",
            status = "PRESENT"
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-10-10", listOf(foreignAttendance))
            fail("Expected TenantSecurityException on cross-tenant mutation")
        } catch (e: TenantSecurityException) {
            assertTrue(e.message?.contains("Tenant isolation violation") == true)
        }
    }

    @Test
    fun testUnauthenticatedOperationWithoutSessionThrowsSecurityException() = runBlocking {
        // Clear active session
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()

        try {
            attendanceRepository.verifyBatchByIncharge("BATCH-UNAUTH-01")
            fail("Expected TenantSecurityException on unauthenticated operation")
        } catch (e: TenantSecurityException) {
            assertTrue(e.message?.contains("Verified authenticated session required") == true)
        }
    }
}
