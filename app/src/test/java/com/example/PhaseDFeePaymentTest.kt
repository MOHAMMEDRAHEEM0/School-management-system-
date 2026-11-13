package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.remote.sync.CloudOutboxDispatcher
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.repository.CreateFeeAssessmentRequest
import com.example.data.repository.DuplicatePaymentException
import com.example.data.repository.DuplicateReceiptException
import com.example.data.repository.FeeRepositoryImpl
import com.example.data.repository.ManualFeePaymentRequest
import com.example.data.repository.UserSession
import com.example.security.SecureSessionManager
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhaseDFeePaymentTest {

    private lateinit var db: AppDatabase
    private lateinit var outboxManager: DurableOutboxManager
    private lateinit var feeRepository: FeeRepositoryImpl

    private val tenantA = "INST-ALPHA-2026"
    private val tenantB = "INST-BETA-2026"
    private val accountantId = "FAC-ACC-01"
    private val studentA = StudentEntity(
        id = "STU-A-101",
        institutionId = tenantA,
        name = "Aarav Sharma",
        grade = "10",
        section = "A",
        rollNumber = 1,
        admissionNumber = "ADM-A-101",
        gender = "Male",
        guardianName = "Vikram Sharma",
        guardianPhone = "+91 9876543210",
        email = "aarav@alpha.edu"
    )
    private val studentA2 = StudentEntity(
        id = "STU-A-102",
        institutionId = tenantA,
        name = "Diya Patel",
        grade = "10",
        section = "A",
        rollNumber = 2,
        admissionNumber = "ADM-A-102",
        gender = "Female",
        guardianName = "Kiran Patel",
        guardianPhone = "+91 9876543211",
        email = "diya@alpha.edu"
    )
    private val studentB = StudentEntity(
        id = "STU-B-201",
        institutionId = tenantB,
        name = "Rohan Verma",
        grade = "9",
        section = "B",
        rollNumber = 4,
        admissionNumber = "ADM-B-201",
        gender = "Male",
        guardianName = "Suresh Verma",
        guardianPhone = "+91 9876500001",
        email = "rohan@beta.edu"
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        outboxManager = DurableOutboxManager(
            dao = db.schoolDao(),
            defaultInstitutionId = tenantA
        )
        feeRepository = FeeRepositoryImpl(
            dao = db.schoolDao(),
            outboxManager = outboxManager
        )

        setSession(
            userId = accountantId,
            displayName = "Suresh Mehta",
            role = UserRole.ACCOUNTANT,
            institution = tenantA,
            permissions = setOf(
                SchoolPermissions.MANAGE_FEE_STRUCTURE,
                SchoolPermissions.RECORD_OFFLINE_FEE
            )
        )

        runBlocking {
            db.schoolDao().insertStudent(studentA)
            db.schoolDao().insertStudent(studentA2)
            db.schoolDao().insertStudent(studentB)
        }
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        db.close()
    }

    private fun setSession(
        userId: String,
        displayName: String = "User $userId",
        role: UserRole,
        institution: String,
        permissions: Set<String> = emptySet()
    ) {
        val claims = SecurityClaims(
            userId = userId,
            role = role,
            roleClaim = role.name,
            displayName = displayName,
            email = "$userId@school.edu",
            permissions = permissions,
            sessionToken = "SESSION-$userId"
        )
        val session = UserSession(
            userId = userId,
            institutionId = institution,
            email = claims.email,
            displayName = displayName,
            role = role,
            sessionToken = claims.sessionToken,
            claims = claims
        )
        SecureSessionManager.saveSession(session)
    }

    private suspend fun createTuitionFeeForStudentA(totalPaise: Long = 2000000L): FeeRecordEntity {
        val result = feeRepository.createFeeAssessment(
            CreateFeeAssessmentRequest(
                studentId = studentA.id,
                term = "Term 1: 2026-27",
                feeType = "Tuition Fee",
                totalAmountPaise = totalPaise
            )
        )
        assertTrue("Fee assessment creation must succeed", result.isSuccess)
        return result.getOrThrow()
    }

    // =========================================================================
    // 1. AUTHORIZED PAYMENT CREATION, ATOMIC ROOM PERSISTENCE & OFFLINE QUEUE
    // =========================================================================

    @Test
    fun testAuthorizedRolesCanRecordPaymentAtomicallyAndRemainPendingOffline() = runBlocking {
        val fee = createTuitionFeeForStudentA(totalPaise = 2000000L) // ₹20,000

        // Authorized as ACCOUNTANT
        val paymentResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 800000L, // ₹8,000
                paymentMode = "CASH",
                remarks = "Term 1 installment 1",
                idempotencyKey = "IDEMP-PAY-001"
            )
        )
        assertTrue("Accountant manual payment must succeed", paymentResult.isSuccess)
        val receipt = paymentResult.getOrThrow()

        // Verify transaction properties & manual source tag
        assertEquals(800000L, receipt.transaction.amountPaidPaise)
        assertEquals(FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED, receipt.transaction.verificationSource)
        assertEquals(FeePaymentTransactionEntity.STATUS_RECORDED_MANUAL, receipt.transaction.status)
        assertFalse("Manual entry must not report gateway verification", receipt.transaction.isGatewayVerified)
        assertFalse("Offline record must NEVER be marked synced before server ACK", receipt.transaction.isSyncedToCentral)
        assertEquals(FeePaymentTransactionEntity.SYNC_PENDING, receipt.transaction.syncStatus)
        assertNull(receipt.transaction.serverAckToken)
        assertEquals(accountantId, receipt.transaction.recordedByUserId)
        assertEquals(UserRole.ACCOUNTANT.name, receipt.transaction.recordedByRole)
        assertEquals(1200000L, receipt.outstandingBalancePaise)

        // Verify persisted in Room (survives re-query)
        val persistedTxn = db.schoolDao().getPaymentTransactionById(receipt.transaction.transactionId)
        assertNotNull(persistedTxn)
        assertEquals(receipt.transaction.receiptNumber, persistedTxn!!.receiptNumber)
        assertFalse(persistedTxn.isSyncedToCentral)

        val persistedFee = db.schoolDao().getFeeRecordById(fee.id)
        assertNotNull(persistedFee)
        assertEquals("PARTIAL", persistedFee!!.status)
        assertEquals(800000L, persistedFee.paidAmount)
        assertEquals(1200000L, persistedFee.dueAmount)

        // Verify DurableOutbox contains high-integrity FEE_PAYMENT item
        val outboxItems = db.schoolDao().getPendingOutbox(tenantA).first()
        val payOutbox = outboxItems.find { it.entityType == "FEE_PAYMENT" && it.entityId == receipt.transaction.transactionId }
        assertNotNull("Payment outbox entry must be persisted atomically", payOutbox)
        assertEquals(DurableOutboxEntity.STATUS_PENDING, payOutbox!!.status)
        assertEquals("IDEMP-PAY-001", payOutbox.idempotencyKey)

        // Verify immutable audit log written
        val audits = db.schoolDao().getAllAuditLogsAsc()
            .filter { it.entityId == receipt.transaction.transactionId && it.institutionId == tenantA }
        assertTrue("Financial audit log must be recorded", audits.any { it.action == "MANUAL_FEE_PAYMENT_RECORDED" })

        // Also verify PRINCIPAL and ADMIN are authorized to record subsequent installments
        setSession(userId = "FAC-PRIN-01", displayName = "Dr. Vance", role = UserRole.PRINCIPAL, institution = tenantA)
        val prinPayment = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 200000L,
                paymentMode = "UPI_MANUAL",
                transactionReference = "UPI-UTR-445511",
                idempotencyKey = "IDEMP-PAY-002"
            )
        )
        assertTrue("Principal must be authorized to record manual payment", prinPayment.isSuccess)

        setSession(userId = "ADM-01", displayName = "System Admin", role = UserRole.ADMIN, institution = tenantA)
        val adminPayment = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 1000000L,
                paymentMode = "CHEQUE",
                transactionReference = "CHQ-778899",
                idempotencyKey = "IDEMP-PAY-003"
            )
        )
        assertTrue("Admin must be authorized to record manual payment", adminPayment.isSuccess)
        assertEquals(0L, adminPayment.getOrThrow().outstandingBalancePaise)
        assertEquals("PAID", adminPayment.getOrThrow().updatedFeeRecord.status)
    }

    // =========================================================================
    // 2. AMOUNT, MODE & BALANCE CALCULATION VALIDATION
    // =========================================================================

    @Test
    fun testPaymentAmountAndModeValidation() = runBlocking {
        val fee = createTuitionFeeForStudentA(totalPaise = 500000L) // ₹5,000

        // Zero amount rejected
        val zeroResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 0L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-ZERO"
            )
        )
        assertTrue(zeroResult.isFailure)
        assertTrue(zeroResult.exceptionOrNull() is IllegalArgumentException)

        // Negative amount rejected
        val negativeResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = -5000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-NEG"
            )
        )
        assertTrue(negativeResult.isFailure)
        assertTrue(negativeResult.exceptionOrNull() is IllegalArgumentException)

        // Overpayment exceeding outstanding balance rejected
        val overpayResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 500100L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-OVERPAY"
            )
        )
        assertTrue(overpayResult.isFailure)
        assertTrue(overpayResult.exceptionOrNull()?.message?.contains("exceeds persisted outstanding balance") == true)

        // Non-cash without reference rejected
        val missingRefResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 100000L,
                paymentMode = "CHEQUE",
                transactionReference = "",
                idempotencyKey = "IDEMP-NOREF"
            )
        )
        assertTrue(missingRefResult.isFailure)
        assertTrue(missingRefResult.exceptionOrNull()?.message?.contains("Reference number") == true)

        // Manual repository rejects gateway mode (must never fake gateway verification)
        val gatewaySpoofResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 100000L,
                paymentMode = "GATEWAY_ONLINE",
                transactionReference = "FAKE_RZP_123",
                idempotencyKey = "IDEMP-GATEWAY-SPOOF"
            )
        )
        assertTrue(gatewaySpoofResult.isFailure)
        assertTrue(gatewaySpoofResult.exceptionOrNull()?.message?.contains("Gateway payments require live server verification") == true)

        // Confirm zero transactions persisted after failed validations
        val txns = db.schoolDao().getConfirmedTransactionsForFeeRecord(fee.id, tenantA)
        assertEquals(0, txns.size)
    }

    @Test
    fun testBalanceCalculatedStrictlyFromPersistedTransactionsNotMutableColumns() = runBlocking {
        // Seed a fee record whose paidAmount/dueAmount columns are deliberately stale/corrupted,
        // then prove FeeRepository calculates balance strictly from persisted FeePaymentTransactionEntity rows.
        val fee = createTuitionFeeForStudentA(totalPaise = 1000000L) // ₹10,000

        // Record ₹3,000 verified cash payment
        val pay1 = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 300000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-BAL-1"
            )
        ).getOrThrow()
        assertEquals(700000L, pay1.outstandingBalancePaise)

        // Simulate stale cached column values in fee_records
        db.schoolDao().recordFeePayment(
            feeId = fee.id,
            paid = 999900L,
            due = 100L,
            status = "PARTIAL",
            receipt = pay1.transaction.receiptNumber,
            date = "2026-10-01",
            mode = "CASH"
        )

        // Repository summary must compute strictly from persisted transactions: ₹10,000 - ₹3,000 = ₹7,000
        val summary = feeRepository.getFeeBalanceSummary(fee.id).getOrThrow()
        assertEquals(300000L, summary.confirmedPaidPaise)
        assertEquals(700000L, summary.outstandingBalancePaise)
        assertEquals(1, summary.confirmedTransactionCount)

        // Paying remaining ₹7,000 succeeds and settles to PAID (even though stale due column said 100 paise)
        val pay2 = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 700000L,
                paymentMode = "BANK_TRANSFER",
                transactionReference = "NEFT-990011",
                idempotencyKey = "IDEMP-BAL-2"
            )
        ).getOrThrow()
        assertEquals(0L, pay2.outstandingBalancePaise)
        assertEquals("PAID", pay2.updatedFeeRecord.status)
        assertEquals(1000000L, pay2.updatedFeeRecord.paidAmount)

        // All transaction rows remain preserved for financial audit history
        val allTxns = db.schoolDao().getConfirmedTransactionsForFeeRecord(fee.id, tenantA)
        assertEquals(2, allTxns.size)
        assertEquals(1000000L, allTxns.sumOf { it.amountPaidPaise })
    }

    // =========================================================================
    // 3. DUPLICATE PAYMENT & RECEIPT UNIQUENESS PREVENTION
    // =========================================================================

    @Test
    fun testDuplicatePaymentAndReceiptUniquenessEnforcement() = runBlocking {
        val fee = createTuitionFeeForStudentA(totalPaise = 1500000L)

        // 1. First payment with explicit idempotency key & custom receipt
        val first = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 500000L,
                paymentMode = "CHEQUE",
                transactionReference = "CHQ-10001",
                idempotencyKey = "IDEMP-DUP-KEY-1",
                customReceiptNumber = "RCP-ALPHA-0001"
            )
        ).getOrThrow()
        assertEquals("RCP-ALPHA-0001", first.transaction.receiptNumber)

        // 2. Duplicate idempotencyKey submission -> rejected with DuplicatePaymentException
        val dupKeyResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 500000L,
                paymentMode = "CHEQUE",
                transactionReference = "CHQ-10001",
                idempotencyKey = "IDEMP-DUP-KEY-1",
                customReceiptNumber = "RCP-ALPHA-0002"
            )
        )
        assertTrue(dupKeyResult.isFailure)
        assertTrue(dupKeyResult.exceptionOrNull() is DuplicatePaymentException)

        // 3. Duplicate cheque reference on same fee record -> rejected with DuplicatePaymentException
        val dupRefResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 200000L,
                paymentMode = "CHEQUE",
                transactionReference = "CHQ-10001",
                idempotencyKey = "IDEMP-DUP-REF-2"
            )
        )
        assertTrue(dupRefResult.isFailure)
        assertTrue(dupRefResult.exceptionOrNull() is DuplicatePaymentException)

        // 4. Duplicate receipt number -> rejected with DuplicateReceiptException
        val dupReceiptResult = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 200000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-DUP-RCP-3",
                customReceiptNumber = "RCP-ALPHA-0001"
            )
        )
        assertTrue(dupReceiptResult.isFailure)
        assertTrue(dupReceiptResult.exceptionOrNull() is DuplicateReceiptException)

        // 5. Auto-generated receipts are unique across subsequent valid payments
        val auto1 = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 300000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-AUTO-1"
            )
        ).getOrThrow()
        val auto2 = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 300000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-AUTO-2"
            )
        ).getOrThrow()
        assertNotEquals(first.transaction.receiptNumber, auto1.transaction.receiptNumber)
        assertNotEquals(auto1.transaction.receiptNumber, auto2.transaction.receiptNumber)
    }

    // =========================================================================
    // 4. RETRY, CONFLICT & GENUINE SERVER ACKNOWLEDGEMENT
    // =========================================================================

    @Test
    fun testRetryAndGenuineServerAcknowledgementUpdatesPaymentSyncState() = runBlocking {
        var dispatchAttempt = 0
        val confirmedAckToken = "ACK-FS-FEE-CONFIRMED-7741"

        val retryThenAckDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                if (item.entityType != "FEE_PAYMENT") {
                    return DurableOutboxManager.CloudDispatchResult.Success(
                        ackToken = "ACK-OTHER-${item.entityId}",
                        serverTimestamp = System.currentTimeMillis()
                    )
                }
                dispatchAttempt++
                return if (dispatchAttempt == 1) {
                    DurableOutboxManager.CloudDispatchResult.TransientError("503 Cloud Function temporarily unavailable")
                } else {
                    DurableOutboxManager.CloudDispatchResult.Success(
                        ackToken = confirmedAckToken,
                        serverTimestamp = System.currentTimeMillis()
                    )
                }
            }
        }

        val syncingOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = retryThenAckDispatcher,
            defaultInstitutionId = tenantA
        )
        val syncingRepo = FeeRepositoryImpl(db.schoolDao(), syncingOutbox)

        val fee = syncingRepo.createFeeAssessment(
            CreateFeeAssessmentRequest(
                studentId = studentA.id,
                term = "Term 2",
                feeType = "Exam Fee",
                totalAmountPaise = 250000L
            )
        ).getOrThrow()

        val payResult = syncingRepo.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 250000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-RETRY-ACK-1"
            )
        ).getOrThrow()

        // Attempt 1: transient failure -> outbox enters RETRYABLE_FAILURE, transaction MUST remain PENDING_SYNC
        val firstFlush = syncingOutbox.flushQueue(tenantA, forceAll = true).getOrThrow()
        assertTrue(firstFlush.failedCount >= 1)

        val afterRetryTxn = db.schoolDao().getPaymentTransactionById(payResult.transaction.transactionId)!!
        assertFalse("Must NOT mark synced on transient failure", afterRetryTxn.isSyncedToCentral)
        assertEquals(FeePaymentTransactionEntity.SYNC_PENDING, afterRetryTxn.syncStatus)
        assertNull(afterRetryTxn.serverAckToken)

        val retryOutboxItem = db.schoolDao().getOutboxByIdempotencyKey("IDEMP-RETRY-ACK-1")!!
        assertEquals(DurableOutboxEntity.STATUS_RETRYABLE_FAILURE, retryOutboxItem.status)
        assertEquals(1, retryOutboxItem.retryCount)

        // Attempt 2: genuine server ACK -> marks transaction SYNCED and stores ackToken
        val secondFlush = syncingOutbox.flushQueue(tenantA, forceAll = true).getOrThrow()
        assertEquals(1, secondFlush.acknowledgedCount)

        val syncedTxn = db.schoolDao().getPaymentTransactionById(payResult.transaction.transactionId)!!
        assertTrue("Must mark synced after genuine server ACK", syncedTxn.isSyncedToCentral)
        assertEquals(FeePaymentTransactionEntity.SYNC_SYNCED, syncedTxn.syncStatus)
        assertEquals(confirmedAckToken, syncedTxn.serverAckToken)
    }

    @Test
    fun testServerConflictMarksTransactionConflictWithoutClaimingSynced() = runBlocking {
        val conflictDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                return if (item.entityType == "FEE_PAYMENT") {
                    DurableOutboxManager.CloudDispatchResult.Conflict("Duplicate receipt collision on server")
                } else {
                    DurableOutboxManager.CloudDispatchResult.Success("ACK-OTHER", System.currentTimeMillis())
                }
            }
        }
        val syncingOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = conflictDispatcher,
            defaultInstitutionId = tenantA
        )
        val syncingRepo = FeeRepositoryImpl(db.schoolDao(), syncingOutbox)

        val fee = syncingRepo.createFeeAssessment(
            CreateFeeAssessmentRequest(
                studentId = studentA.id,
                term = "Term 1",
                feeType = "Lab Fee",
                totalAmountPaise = 150000L
            )
        ).getOrThrow()
        val payResult = syncingRepo.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 150000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-CONFLICT-1"
            )
        ).getOrThrow()

        val flush = syncingOutbox.flushQueue(tenantA, forceAll = true).getOrThrow()
        assertEquals(1, flush.conflictCount)

        val conflictTxn = db.schoolDao().getPaymentTransactionById(payResult.transaction.transactionId)!!
        assertFalse(conflictTxn.isSyncedToCentral)
        assertEquals(FeePaymentTransactionEntity.SYNC_CONFLICT, conflictTxn.syncStatus)
    }

    // =========================================================================
    // 5. UNAUTHORIZED ROLES & CROSS-TENANT REJECTION
    // =========================================================================

    @Test
    fun testUnauthorizedRolesRejectedFromFinancialWritesAndCrossStudentReads() = runBlocking {
        val fee = createTuitionFeeForStudentA(totalPaise = 400000L)

        // Roles forbidden from fee mutation: TEACHER, INCHARGE, STUDENT, PARENT
        val unauthorizedRoles = listOf(UserRole.TEACHER, UserRole.INCHARGE, UserRole.STUDENT, UserRole.PARENT)
        for (role in unauthorizedRoles) {
            setSession(
                userId = "USR-${role.name}",
                displayName = "Test ${role.name}",
                role = role,
                institution = tenantA,
                // Even if caller supplies broad permissions in memory, role check rejects non-financial roles
                permissions = setOf(SchoolPermissions.MANAGE_FEE_STRUCTURE)
            )
            val attempt = feeRepository.recordManualPayment(
                ManualFeePaymentRequest(
                    studentId = studentA.id,
                    feeRecordId = fee.id,
                    amountPaise = 100000L,
                    paymentMode = "CASH",
                    idempotencyKey = "IDEMP-UNAUTH-${role.name}"
                )
            )
            assertTrue("Role ${role.name} must be rejected", attempt.isFailure)
            assertTrue("Role ${role.name} must throw SecurityException", attempt.exceptionOrNull() is SecurityException)
        }

        // Student reading own fee summary succeeds, reading another student's fails
        setSession(userId = studentA.id, displayName = studentA.name, role = UserRole.STUDENT, institution = tenantA)
        assertTrue(feeRepository.getStudentFeeSummaries(studentA.id).isSuccess)
        val otherRead = feeRepository.getStudentFeeSummaries(studentA2.id)
        assertTrue(otherRead.isFailure)
        assertTrue(otherRead.exceptionOrNull() is SecurityException)

        // Unauthenticated session fails closed
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        val unauthAttempt = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentA.id,
                feeRecordId = fee.id,
                amountPaise = 100000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-NO-SESSION"
            )
        )
        assertTrue(unauthAttempt.isFailure)
        assertTrue(unauthAttempt.exceptionOrNull() is SecurityException)

        // Verify zero unauthorized payments leaked into DB
        assertEquals(0, db.schoolDao().getConfirmedTransactionsForFeeRecord(fee.id, tenantA).size)
    }

    @Test
    fun testCrossTenantAccessAndPaymentRejected() = runBlocking {
        // Seed fee record in Tenant B while logged into Tenant B
        setSession(userId = "ACC-BETA", displayName = "Beta Accountant", role = UserRole.ACCOUNTANT, institution = tenantB)
        val betaFee = feeRepository.createFeeAssessment(
            CreateFeeAssessmentRequest(
                studentId = studentB.id,
                term = "Term 1",
                feeType = "Annual Tuition",
                totalAmountPaise = 900000L
            )
        ).getOrThrow()

        // Switch session to Tenant A's Accountant and attempt cross-tenant read, assessment, and payment
        setSession(userId = accountantId, displayName = "Suresh Mehta", role = UserRole.ACCOUNTANT, institution = tenantA)

        val crossRead = feeRepository.getFeeBalanceSummary(betaFee.id)
        assertTrue(crossRead.isFailure)
        assertTrue(crossRead.exceptionOrNull() is TenantSecurityException)

        val crossStudentRead = feeRepository.getStudentFeeSummaries(studentB.id)
        assertTrue(crossStudentRead.isFailure)
        assertTrue(crossStudentRead.exceptionOrNull() is TenantSecurityException)

        val crossAssess = feeRepository.createFeeAssessment(
            CreateFeeAssessmentRequest(
                studentId = studentB.id,
                term = "Term 1",
                feeType = "Malicious Assessment",
                totalAmountPaise = 100000L
            )
        )
        assertTrue(crossAssess.isFailure)
        assertTrue(crossAssess.exceptionOrNull() is TenantSecurityException)

        val crossPayment = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                studentId = studentB.id,
                feeRecordId = betaFee.id,
                amountPaise = 100000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-CROSS-TENANT"
            )
        )
        assertTrue(crossPayment.isFailure)
        assertTrue(crossPayment.exceptionOrNull() is TenantSecurityException)

        val explicitTenantSpoof = feeRepository.recordManualPayment(
            ManualFeePaymentRequest(
                institutionId = tenantB,
                studentId = studentB.id,
                feeRecordId = betaFee.id,
                amountPaise = 100000L,
                paymentMode = "CASH",
                idempotencyKey = "IDEMP-TENANT-SPOOF"
            )
        )
        assertTrue(explicitTenantSpoof.isFailure)
        assertTrue(explicitTenantSpoof.exceptionOrNull() is TenantSecurityException)

        // Verify Tenant B fee record still has zero transactions
        assertEquals(0, db.schoolDao().getConfirmedTransactionsForFeeRecord(betaFee.id, tenantB).size)
    }

    // =========================================================================
    // 6. MANUAL VS GATEWAY DISTINCTION
    // =========================================================================

    @Test
    fun testManualVsGatewayVerificationDistinction() {
        val manualTxn = FeePaymentTransactionEntity(
            transactionId = "TXN-MAN-1",
            institutionId = tenantA,
            feeRecordId = "FEE-1",
            studentId = studentA.id,
            studentName = studentA.name,
            amountPaidPaise = 100000L,
            paymentMode = "CASH",
            transactionReference = "CASH-1",
            receiptNumber = "RCP-MAN-1",
            status = FeePaymentTransactionEntity.STATUS_RECORDED_MANUAL,
            verificationSource = FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED
        )
        val gatewayTxn = FeePaymentTransactionEntity(
            transactionId = "TXN-GW-1",
            institutionId = tenantA,
            feeRecordId = "FEE-1",
            studentId = studentA.id,
            studentName = studentA.name,
            amountPaidPaise = 100000L,
            paymentMode = "GATEWAY_ONLINE",
            transactionReference = "rzp_pay_123",
            receiptNumber = "RCP-GW-1",
            status = FeePaymentTransactionEntity.STATUS_GATEWAY_VERIFIED,
            verificationSource = FeePaymentTransactionEntity.SOURCE_GATEWAY_VERIFIED,
            syncStatus = FeePaymentTransactionEntity.SYNC_SYNCED,
            isSyncedToCentral = true,
            serverAckToken = "ACK-GW-WEBHOOK"
        )
        assertFalse(manualTxn.isGatewayVerified)
        assertEquals(FeePaymentTransactionEntity.SOURCE_MANUAL_RECORDED, manualTxn.verificationSource)
        assertTrue(gatewayTxn.isGatewayVerified)
        assertEquals(FeePaymentTransactionEntity.SOURCE_GATEWAY_VERIFIED, gatewayTxn.verificationSource)
    }
}
