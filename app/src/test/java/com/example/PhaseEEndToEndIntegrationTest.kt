package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import org.robolectric.Shadows
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.remote.sync.AttendanceFeeSyncWorker
import com.example.data.remote.sync.CloudOutboxDispatcher
import com.example.data.remote.sync.CloudSyncWorker
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.AuthState
import com.example.data.repository.CreateFeeAssessmentRequest
import com.example.data.repository.DuplicatePaymentException
import com.example.data.repository.FeeRepositoryImpl
import com.example.data.repository.ManualFeePaymentRequest
import com.example.data.repository.SchoolRepository
import com.example.data.repository.UserRepositoryImpl
import com.example.data.repository.UserSession
import com.example.security.SecureSessionManager
import com.example.security.SecurityUtils
import com.example.security.SessionExpiredException
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
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
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhaseEEndToEndIntegrationTest {

    private lateinit var context: Context
    private val tempDbFiles = mutableListOf<File>()

    private val tenantA = "INST-ALPHA-2026"
    private val tenantB = "INST-BETA-2026"

    private class ControllableDispatcher : CloudOutboxDispatcher {
        var shouldFailTransiently: Boolean = false
        val dispatchedKeys = mutableListOf<String>()

        override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
            dispatchedKeys.add(item.idempotencyKey)
            return if (shouldFailTransiently) {
                DurableOutboxManager.CloudDispatchResult.TransientError("503 Simulated Offline / Firestore Unavailable")
            } else {
                DurableOutboxManager.CloudDispatchResult.Success(
                    ackToken = "ACK-${item.entityId}-${dispatchedKeys.size}",
                    serverTimestamp = System.currentTimeMillis()
                )
            }
        }
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        tempDbFiles.forEach { file ->
            context.deleteDatabase(file.name)
            file.delete()
        }
        tempDbFiles.clear()
    }

    private fun openPersistentDb(dbName: String): AppDatabase {
        val dbFile = context.getDatabasePath(dbName)
        if (!tempDbFiles.contains(dbFile)) tempDbFiles.add(dbFile)
        return Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()
    }

    private fun setActiveSession(
        userId: String,
        institutionId: String,
        role: UserRole,
        displayName: String = "$role $userId",
        assignedClassSection: String? = "10-A",
        permissions: Set<String> = emptySet()
    ): UserSession {
        val claims = SecurityClaims(
            userId = userId,
            role = role,
            roleClaim = role.name,
            displayName = displayName,
            email = "$userId@$institutionId.edu".lowercase(),
            assignedClassSection = assignedClassSection,
            permissions = permissions,
            sessionToken = "TOKEN-$institutionId-$userId"
        )
        val session = UserSession(
            userId = userId,
            institutionId = institutionId,
            email = claims.email,
            displayName = displayName,
            role = role,
            sessionToken = claims.sessionToken,
            claims = claims
        )
        SecureSessionManager.saveSession(session)
        return session
    }

    // =========================================================================
    // 1. AUTHENTICATION, EXPIRED SESSION FAIL-CLOSED & LOGOUT ISOLATION
    // =========================================================================

    @Test
    fun testExpiredSessionFailsClosedAndLogoutClearsAllStateAcrossLogins() = runBlocking {
        val dbName = "phase_e_auth_e2e.db"
        val db = openPersistentDb(dbName)
        try {
            val dao = db.schoolDao()
            val salt = "phase_e_salt_1"
            val passHash = SecurityUtils.hashPasswordPBKDF2("Password@123", salt)

            dao.insertInstitution(InstitutionEntity(institutionId = tenantA, registrationCode = "1001", name = "Alpha Academy"))
            dao.insertInstitution(InstitutionEntity(institutionId = tenantB, registrationCode = "2002", name = "Beta Academy"))
            dao.insertUsers(
                listOf(
                    UserEntity(
                        id = "ACC-A-1",
                        institutionId = tenantA,
                        username = "acc_alpha",
                        email = "acc@alpha.edu",
                        passwordSalt = salt,
                        passwordHash = passHash,
                        role = "ACCOUNTANT",
                        displayName = "Alpha Accountant"
                    ),
                    UserEntity(
                        id = "TCH-B-1",
                        institutionId = tenantB,
                        username = "tch_beta",
                        email = "tch@beta.edu",
                        passwordSalt = salt,
                        passwordHash = passHash,
                        role = "TEACHER",
                        displayName = "Beta Teacher"
                    )
                )
            )
            dao.insertStudent(StudentEntity(id = "STU-A-1", institutionId = tenantA, name = "Aarav Alpha", grade = "10", section = "A", rollNumber = 1))
            dao.insertStudent(StudentEntity(id = "STU-B-1", institutionId = tenantB, name = "Rohan Beta", grade = "10", section = "A", rollNumber = 1))

            // 1A. Expired token must be rejected by SecureSessionManager (no silent extension)
            val expiredSession = UserSession(
                userId = "ACC-A-1",
                institutionId = tenantA,
                email = "acc@alpha.edu",
                displayName = "Alpha Accountant",
                role = UserRole.ACCOUNTANT,
                sessionToken = "EXPIRED-TOKEN",
                claims = SecurityClaims(
                    userId = "ACC-A-1",
                    role = UserRole.ACCOUNTANT,
                    roleClaim = "ACCOUNTANT",
                    displayName = "Alpha Accountant",
                    permissions = emptySet()
                ),
                issuedAt = System.currentTimeMillis() - 7_200_000L,
                expiresAt = System.currentTimeMillis() - 1_000L
            )
            try {
                SecureSessionManager.saveSession(expiredSession)
                fail("Expired session must fail closed")
            } catch (_: SessionExpiredException) {
                assertNull(SecureSessionManager.getActiveSession())
                assertNull(TenantSecurityContext.getActiveTenantIdOrNull())
            }

            // 1B. Local offline login establishes Tenant A accountant claims & denies unauthorized tenant switch
            val authRepo = AuthRepositoryImpl(dao, firebaseAuth = null)
            val loginAlpha = authRepo.signInWithEmailPassword("acc@alpha.edu", "Password@123", tenantA)
            assertTrue("Valid credentials must succeed", loginAlpha.isSuccess)
            assertEquals(tenantA, TenantSecurityContext.getActiveTenantIdOrNull())
            assertTrue(authRepo.switchTenant(tenantB).isFailure)

            // 1C. SchoolViewModel logout clears auth flags, fee selection, ward state & TenantSecurityContext
            val outbox = DurableOutboxManager(dao, defaultInstitutionId = tenantA)
            val viewModel = SchoolViewModel(
                repository = SchoolRepository(dao, outbox),
                userRepository = UserRepositoryImpl(dao),
                attendanceRepository = AttendanceRepositoryImpl(dao, outbox),
                authRepository = authRepo,
                preferencesManager = null,
                context = context
            )
            viewModel.selectFeeStudent("STU-A-1")
            viewModel.selectWard("STU-A-1")
            viewModel.logout()
            repeat(20) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (authRepo.authState.value is AuthState.Unauthenticated) return@repeat
                delay(50)
            }

            assertFalse(viewModel.isAuthenticated.value)
            assertNull(viewModel.userClaims.value)
            assertNull(viewModel.currentAuthSession.value)
            assertEquals(UserRole.STUDENT, viewModel.currentRole.value)
            assertNull(viewModel.feeWorkflowState.value.selectedStudent)
            assertNull(SecureSessionManager.getActiveSession())
            assertNull(TenantSecurityContext.getActiveTenantIdOrNull())
            assertTrue(authRepo.authState.value is AuthState.Unauthenticated)

            // 1D. Subsequent login as Tenant B Teacher isolates tenant & denies Tenant A access
            val loginBeta = authRepo.signInWithEmailPassword("tch@beta.edu", "Password@123", tenantB)
            assertTrue("Beta teacher login must succeed", loginBeta.isSuccess)
            assertEquals(tenantB, TenantSecurityContext.getActiveTenantIdOrNull())
            assertEquals(UserRole.TEACHER, loginBeta.getOrThrow().role)
        } finally {
            db.close()
        }
    }

    // =========================================================================
    // 2. DAILY ATTENDANCE: MULTI-TENANT, RESTART PERSISTENCE & WORKER RETRY->ACK
    // =========================================================================

    @Test
    fun testDailyAttendanceEndToEndOfflineRestartIdempotentRetryAndServerAck() = runBlocking {
        val dbName = "phase_e_attendance_e2e.db"
        val dispatcher = ControllableDispatcher().apply { shouldFailTransiently = true }

        val dateKey = "2026-11-12"
        val classSection = "10-A"

        // --- SESSION 1: Offline mark & restart ---
        var db = openPersistentDb(dbName)
        val studentA = StudentEntity(id = "STU-A-101", institutionId = tenantA, name = "Aarav Alpha", grade = "10", section = "A", rollNumber = 1)
        val studentB = StudentEntity(id = "STU-B-201", institutionId = tenantB, name = "Diya Beta", grade = "10", section = "A", rollNumber = 1)

        try {
            db.schoolDao().insertStudent(studentA)
            db.schoolDao().insertStudent(studentB)

            setActiveSession(
                userId = "TCH-A-1",
                institutionId = tenantA,
                role = UserRole.TEACHER,
                assignedClassSection = classSection,
                permissions = setOf(SchoolPermissions.MARK_ATTENDANCE_SECTION)
            )
            val attRepo1 = AttendanceRepositoryImpl(
                dao = db.schoolDao(),
                outboxManager = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher)
            )

            // Cross-tenant student insertion under Tenant A session must fail closed
            try {
                attRepo1.saveDailyAttendanceBatch(
                    classSection = classSection,
                    date = dateKey,
                    attendances = listOf(
                        AttendanceEntity(
                            institutionId = tenantA,
                            studentId = studentB.id,
                            studentName = studentB.name,
                            classSection = classSection,
                            date = dateKey,
                            status = "PRESENT"
                        )
                    ),
                    updateStudentPercentages = true
                )
                fail("Cross-tenant student attendance save must be rejected")
            } catch (_: TenantSecurityException) {
                // Expected
            }

            // Authorized Tenant A attendance save
            attRepo1.saveDailyAttendanceBatch(
                classSection = classSection,
                date = dateKey,
                attendances = listOf(
                    AttendanceEntity(
                        institutionId = tenantA,
                        studentId = studentA.id,
                        studentName = studentA.name,
                        classSection = classSection,
                        date = dateKey,
                        status = "PRESENT"
                    )
                ),
                updateStudentPercentages = true
            )

            // Re-save on same date & class should replace cleanly without duplicate rows
            attRepo1.saveDailyAttendanceBatch(
                classSection = classSection,
                date = dateKey,
                attendances = listOf(
                    AttendanceEntity(
                        institutionId = tenantA,
                        studentId = studentA.id,
                        studentName = studentA.name,
                        classSection = classSection,
                        date = dateKey,
                        status = "ABSENT"
                    )
                ),
                updateStudentPercentages = true
            )

            // Also save class 10-A attendance inside Tenant B (proves tenant isolation for identical classSection+date)
            setActiveSession(
                userId = "TCH-B-1",
                institutionId = tenantB,
                role = UserRole.TEACHER,
                assignedClassSection = classSection,
                permissions = setOf(SchoolPermissions.MARK_ATTENDANCE_SECTION)
            )
            attRepo1.saveDailyAttendanceBatch(
                classSection = classSection,
                date = dateKey,
                attendances = listOf(
                    AttendanceEntity(
                        institutionId = tenantB,
                        studentId = studentB.id,
                        studentName = studentB.name,
                        classSection = classSection,
                        date = dateKey,
                        status = "PRESENT"
                    )
                ),
                updateStudentPercentages = true
            )

            // Simulate transient failure while offline/backend down via Worker -> must return retry & keep unsynced
            val retryResult = AttendanceFeeSyncWorker.executeSync(
                dao = db.schoolDao(),
                outboxManager = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher),
                attemptCount = 1,
                workerEntityId = "WORK-RETRY-1"
            )
            assertEquals(ListenableWorker.Result.retry(), retryResult)
            val uiBeforeAck = db.schoolDao().getAttendanceRecordsForInstitution(tenantA).first().filter { it.date == dateKey }
            assertEquals(1, uiBeforeAck.size)
            assertEquals("ABSENT", uiBeforeAck.first().status)
            assertFalse("Unsynced before server ACK", uiBeforeAck.first().isSyncedToCentral)
        } finally {
            db.close()
        }

        // --- SESSION 2: App Restart (re-open DB, restore Tenant A session, network recovers -> Worker ACKs) ---
        db = openPersistentDb(dbName)
        try {
            setActiveSession(
                userId = "TCH-A-1",
                institutionId = tenantA,
                role = UserRole.TEACHER,
                assignedClassSection = classSection,
                permissions = setOf(SchoolPermissions.MARK_ATTENDANCE_SECTION)
            )
            val attRepo2 = AttendanceRepositoryImpl(
                dao = db.schoolDao(),
                outboxManager = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher)
            )

            // Verify tenant-scoped read shows only Tenant A's latest status ("ABSENT") across restart
            val tenantAAttendance = attRepo2.getAttendanceForClassAndDate(classSection, dateKey)
            assertEquals(1, tenantAAttendance.size)
            assertEquals(studentA.id, tenantAAttendance.first().studentId)
            assertEquals("ABSENT", tenantAAttendance.first().status)

            // Force nextAttemptAt ready after simulated restart and bring network/backend online
            db.openHelper.writableDatabase.execSQL("UPDATE durable_outbox SET nextAttemptAt = 0")
            dispatcher.shouldFailTransiently = false

            val successResult = AttendanceFeeSyncWorker.executeSync(
                dao = db.schoolDao(),
                outboxManager = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher),
                attemptCount = 1,
                workerEntityId = "WORK-ACK-2"
            )
            assertTrue("Worker must succeed once server ACKs all tenants", successResult is ListenableWorker.Result.Success)

            // Both Tenant A & Tenant B outbox items must be ACKNOWLEDGED and attendance records marked synced
            assertEquals(0, db.schoolDao().getPendingOutbox(tenantA).first().size)
            assertEquals(0, db.schoolDao().getPendingOutbox(tenantB).first().size)
            val syncedUiA = db.schoolDao().getAttendanceRecordsForInstitution(tenantA).first().filter { it.date == dateKey }
            val syncedUiB = db.schoolDao().getAttendanceRecordsForInstitution(tenantB).first().filter { it.date == dateKey }
            assertEquals(1, syncedUiA.size)
            assertEquals(1, syncedUiB.size)
            assertTrue(syncedUiA.first().isSyncedToCentral)
            assertTrue(syncedUiB.first().isSyncedToCentral)
        } finally {
            db.close()
        }
    }

    // =========================================================================
    // 3. FEE RECEIPTS & LEDGER: RESTART CONSISTENCY, IMMUTABILITY & RETRY->ACK
    // =========================================================================

    @Test
    fun testFeeReceiptsAndLedgerEndToEndConsistencySecurityAndWorkerAck() = runBlocking {
        val dbName = "phase_e_fees_e2e.db"
        val dispatcher = ControllableDispatcher().apply { shouldFailTransiently = true }

        val studentA = StudentEntity(id = "STU-FEE-A1", institutionId = tenantA, name = "Kabir Alpha", grade = "10", section = "A", rollNumber = 4)
        val studentB = StudentEntity(id = "STU-FEE-B1", institutionId = tenantB, name = "Rohan Beta", grade = "10", section = "B", rollNumber = 5)
        lateinit var feeRecordId: String
        lateinit var receipt1: String
        lateinit var receipt2: String
        val idemKey1 = "FEE-IDEM-E2E-1"

        // --- SESSION 1: Create fee assessment & partial payments offline ---
        var db = openPersistentDb(dbName)
        try {
            db.schoolDao().insertStudent(studentA)
            db.schoolDao().insertStudent(studentB)

            setActiveSession(
                userId = "ACC-A-01",
                institutionId = tenantA,
                role = UserRole.ACCOUNTANT,
                displayName = "Suresh Mehta",
                permissions = setOf(SchoolPermissions.MANAGE_FEE_STRUCTURE, SchoolPermissions.RECORD_OFFLINE_FEE)
            )
            val outbox1 = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher)
            val feeRepo1 = FeeRepositoryImpl(db.schoolDao(), outbox1)

            val assessment = feeRepo1.createFeeAssessment(
                CreateFeeAssessmentRequest(
                    studentId = studentA.id,
                    term = "Term 1 (2026-27)",
                    feeType = "Composite Tuition",
                    totalAmountPaise = 65_000_00L
                )
            ).getOrThrow()
            feeRecordId = assessment.id

            // Payment 1: 40,000 INR via CASH
            val pay1 = feeRepo1.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 40_000_00L,
                    paymentMode = "CASH",
                    transactionReference = "CASH-CTR-01",
                    remarks = "Installment 1",
                    idempotencyKey = idemKey1
                )
            ).getOrThrow()
            receipt1 = pay1.transaction.receiptNumber
            assertEquals(25_000_00L, pay1.outstandingBalancePaise)
            assertEquals(FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED, pay1.transaction.verificationSource)
            assertFalse(pay1.transaction.isSyncedToCentral)

            // Duplicate idempotency key must be blocked
            val dupIdem = feeRepo1.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 10_000_00L,
                    paymentMode = "CASH",
                    idempotencyKey = idemKey1
                )
            )
            assertTrue(dupIdem.isFailure)
            assertTrue(dupIdem.exceptionOrNull() is DuplicatePaymentException)

            // Overpayment above 25,000 INR must be rejected
            val overpay = feeRepo1.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 30_000_00L,
                    paymentMode = "CASH"
                )
            )
            assertTrue(overpay.isFailure)

            // Unauthorized TEACHER cannot record payment
            setActiveSession(
                userId = "TCH-A-01",
                institutionId = tenantA,
                role = UserRole.TEACHER
            )
            val teacherAttempt = feeRepo1.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 5_000_00L,
                    paymentMode = "CASH"
                )
            )
            assertTrue(teacherAttempt.isFailure)
            assertTrue(teacherAttempt.exceptionOrNull() is SecurityException)

            // Cross-tenant ACCOUNTANT cannot touch Tenant A record
            setActiveSession(
                userId = "ACC-B-01",
                institutionId = tenantB,
                role = UserRole.ACCOUNTANT
            )
            val crossTenantAttempt = feeRepo1.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 5_000_00L,
                    paymentMode = "CASH"
                )
            )
            assertTrue(crossTenantAttempt.isFailure)
            assertTrue(crossTenantAttempt.exceptionOrNull() is TenantSecurityException)

            // Worker retry while offline keeps outbox & transaction un-synced
            val workerRetry = CloudSyncWorker.executeSync(
                dao = db.schoolDao(),
                outboxManager = outbox1,
                attemptCount = 1
            )
            assertEquals(ListenableWorker.Result.retry(), workerRetry)
            assertFalse(db.schoolDao().getPaymentTransactionById(pay1.transaction.transactionId)!!.isSyncedToCentral)
        } finally {
            db.close()
        }

        // --- SESSION 2: Restart DB, record 2nd partial payment, then cloud ACK ---
        db = openPersistentDb(dbName)
        try {
            setActiveSession(
                userId = "PRIN-A-01",
                institutionId = tenantA,
                role = UserRole.PRINCIPAL,
                displayName = "Prof. Vance"
            )
            val outbox2 = DurableOutboxManager(db.schoolDao(), cloudDispatcher = dispatcher)
            val feeRepo2 = FeeRepositoryImpl(db.schoolDao(), outbox2)

            // Verify balance survived restart accurately from persisted transactions
            val summaryAfterRestart = feeRepo2.getStudentFeeSummaries(studentA.id).getOrThrow().first()
            assertEquals(65_000_00L, summaryAfterRestart.totalAmountPaise)
            assertEquals(40_000_00L, summaryAfterRestart.confirmedPaidPaise)
            assertEquals(25_000_00L, summaryAfterRestart.outstandingBalancePaise)
            assertEquals("PARTIAL", summaryAfterRestart.feeRecord.status)

            // Payment 2: remaining 25,000 INR settles account cleanly with distinct receipt
            val pay2 = feeRepo2.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = feeRecordId,
                    amountPaise = 25_000_00L,
                    paymentMode = "UPI_MANUAL",
                    transactionReference = "UPI-REF-991",
                    remarks = "Final installment"
                )
            ).getOrThrow()
            receipt2 = pay2.transaction.receiptNumber
            assertTrue("Receipts must be unique", receipt1 != receipt2)
            assertEquals(0L, pay2.outstandingBalancePaise)
            assertEquals("PAID", pay2.updatedFeeRecord.status)

            // Bring network back & expire backoff so worker executes live ACK path
            db.openHelper.writableDatabase.execSQL("UPDATE durable_outbox SET nextAttemptAt = 0")
            dispatcher.shouldFailTransiently = false

            val workerSuccess = AttendanceFeeSyncWorker.executeSync(
                dao = db.schoolDao(),
                outboxManager = outbox2,
                attemptCount = 1,
                workerEntityId = "WORK-FEE-ACK"
            )
            assertTrue("Worker must succeed on server ACK", workerSuccess is ListenableWorker.Result.Success)
            assertEquals(0, db.schoolDao().getPendingOutbox(tenantA).first().size)

            val txns = db.schoolDao().getPaymentTransactionsForFeeRecordList(feeRecordId)
            assertEquals(2, txns.size)
            assertTrue(txns.all { it.isSyncedToCentral && it.syncStatus == FeePaymentTransactionEntity.SYNC_SYNCED && !it.serverAckToken.isNullOrBlank() })
            val finalBalance = feeRepo2.getStudentFeeSummaries(studentA.id).getOrThrow().first()
            assertEquals(65_000_00L, finalBalance.confirmedPaidPaise)
            assertEquals(0L, finalBalance.outstandingBalancePaise)
        } finally {
            db.close()
        }
    }

    // =========================================================================
    // 4. ROOM MIGRATION 9 -> 10 -> 11 VERIFICATION
    // =========================================================================

    @Test
    fun testRoomMigration9To11PreservesFinancialAndAttendanceDataAndCreatesIndexes() = runBlocking {
        val migrationDbName = "phase_e_migration_v9_to_v11.db"
        val dbFile = context.getDatabasePath(migrationDbName)
        if (!tempDbFiles.contains(dbFile)) tempDbFiles.add(dbFile)
        context.deleteDatabase(migrationDbName)

        // Step 1: Let Room build an authentic baseline DB schema on disk, then downgrade tables/version to v9 shape
        val baselineDb = openPersistentDb(migrationDbName)
        baselineDb.openHelper.writableDatabase
        baselineDb.close()

        val rawSql = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            rawSql.beginTransaction()
            try {
                // Drop v10/v11 indexes
                rawSql.execSQL("DROP INDEX IF EXISTS `index_users_institutionId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_students_institutionId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_staff_members_institutionId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_attendance_institutionId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_durable_outbox_institutionId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_fee_records_institutionId_studentId`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_fee_payment_transactions_receiptNumber`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_fee_payment_transactions_idempotencyKey`")
                rawSql.execSQL("DROP INDEX IF EXISTS `index_fee_payment_transactions_institutionId_studentId_feeRecordId`")

                // Recreate v9/v10 fee_payment_transactions table without v11 columns
                rawSql.execSQL("DROP TABLE `fee_payment_transactions`")
                rawSql.execSQL(
                    """
                    CREATE TABLE `fee_payment_transactions` (
                        `transactionId` TEXT NOT NULL PRIMARY KEY,
                        `institutionId` TEXT NOT NULL,
                        `feeRecordId` TEXT NOT NULL,
                        `studentId` TEXT NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `amountPaidPaise` INTEGER NOT NULL,
                        `paymentMode` TEXT NOT NULL,
                        `transactionReference` TEXT NOT NULL,
                        `receiptNumber` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `cashierName` TEXT NOT NULL,
                        `status` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Insert legacy v9 records across core tables
                rawSql.execSQL(
                    """
                    INSERT INTO `students` (`id`, `institutionId`, `admissionNumber`, `name`, `grade`, `section`, `rollNumber`, `gender`, `dateOfBirth`, `guardianName`, `guardianPhone`, `studentPhone`, `email`, `bloodGroup`, `attendancePercentage`, `feeDue`, `feePaid`, `avatarColorHex`, `address`, `realIdPhotoUri`, `convertedDigitalId`, `digitalIdStatus`, `academicYear`, `promotionStatus`)
                    VALUES ('STU-MIG-1', 'INST-SXA-2026', 'STU-MIG-1', 'Migrated Scholar', '10', 'A', 1, 'Male', '2011-01-01', 'Parent', '9999999999', '9999999999', 'mig@sxa.edu', 'O+', 92.0, 15000.0, 50000.0, '#0B2545', 'Campus', NULL, 'DIGI-1', 'APPROVED', '2026-2027', 'CURRENT')
                    """.trimIndent()
                )
                rawSql.execSQL(
                    """
                    INSERT INTO `fee_records` (`id`, `institutionId`, `studentId`, `admissionNumber`, `studentName`, `gradeSection`, `term`, `feeType`, `totalAmount`, `paidAmount`, `dueAmount`, `status`, `receiptNumber`, `paidDate`, `paymentMode`, `verifiedByIncharge`)
                    VALUES ('FEE-MIG-1', 'INST-SXA-2026', 'STU-MIG-1', 'STU-MIG-1', 'Migrated Scholar', 'Grade 10-A', 'Term 1', 'Tuition', 6500000, 5000000, 1500000, 'PARTIAL', 'REC-MIG-9001', '2026-08-01', 'CASH', 1)
                    """.trimIndent()
                )
                rawSql.execSQL(
                    """
                    INSERT INTO `fee_payment_transactions` (`transactionId`, `institutionId`, `feeRecordId`, `studentId`, `studentName`, `amountPaidPaise`, `paymentMode`, `transactionReference`, `receiptNumber`, `timestamp`, `cashierName`, `status`)
                    VALUES ('TXN-MIG-001', 'INST-SXA-2026', 'FEE-MIG-1', 'STU-MIG-1', 'Migrated Scholar', 5000000, 'CASH', 'LEGACY-CASH-1', 'REC-MIG-9001', 1785542400000, 'Ramesh Kulkarni', 'RECORDED_MANUAL')
                    """.trimIndent()
                )
                rawSql.execSQL("PRAGMA user_version = 9")
                rawSql.setTransactionSuccessful()
            } finally {
                rawSql.endTransaction()
            }
        } finally {
            rawSql.close()
        }

        // Step 2: Re-open via Room so MIGRATION_9_10 + MIGRATION_10_11 execute and Room runs full schema check
        val upgradedDb = openPersistentDb(migrationDbName)
        try {
            val migratedTxn = upgradedDb.schoolDao().getPaymentTransactionById("TXN-MIG-001")
            assertNotNull("Legacy v9 fee payment transaction must survive migration", migratedTxn)
            assertEquals(5000000L, migratedTxn!!.amountPaidPaise)
            assertEquals("TXN-MIG-001", migratedTxn.idempotencyKey)
            assertEquals(FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED, migratedTxn.verificationSource)
            assertEquals(FeePaymentTransactionEntity.SYNC_PENDING, migratedTxn.syncStatus)
            assertFalse(migratedTxn.isSyncedToCentral)

            val migratedStudent = upgradedDb.schoolDao().getStudentById("STU-MIG-1")
            assertNotNull(migratedStudent)
            assertEquals("Migrated Scholar", migratedStudent!!.name)

            // Verify balance calculation post-migration
            setActiveSession("ACC-MIG-1", "INST-SXA-2026", UserRole.ACCOUNTANT)
            val feeRepo = FeeRepositoryImpl(
                dao = upgradedDb.schoolDao(),
                outboxManager = DurableOutboxManager(upgradedDb.schoolDao())
            )
            val balance = feeRepo.getStudentFeeSummaries("STU-MIG-1").getOrThrow().first()
            assertEquals(5000000L, balance.confirmedPaidPaise)
            assertEquals(1500000L, balance.outstandingBalancePaise)
        } finally {
            upgradedDb.close()
        }
    }
}
