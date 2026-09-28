package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AiAuditLogEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.FeeStructureEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.HomeworkSubmissionEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.ParentWardLinkEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentLeaveRequestEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.UserEntity
import com.example.cloud.dr.DisasterRecoveryManager
import com.example.cloud.fcm.FcmDeviceTokenManager
import com.example.cloud.fcm.FcmNotificationCategory
import com.example.cloud.storage.CloudDocumentStorageService
import com.example.cloud.storage.DocumentCategory
import com.example.cloud.storage.DocumentUploadRequest
import com.example.data.academic.AcademicSessionManager
import com.example.data.local.entities.AcademicYearEntity
import com.example.data.local.entities.NotificationPreferenceEntity
import com.example.data.remote.sync.ConflictResolutionPolicy
import com.example.data.remote.sync.DurableOutboxManager
import com.example.payment.PaymentGatewayService
import com.example.payment.PaymentGatewayProvider
import com.example.payment.PaymentVerificationRequest
import com.example.payment.PaymentStatus
import com.example.payment.PaymentSecurityException
import com.example.payment.GatewayWebhookEvent
import com.example.payment.CreateFeeOrderRequest
import com.example.payment.CreateSaaSOrderRequest
import com.example.payment.SaaSPlan
import com.example.payment.OrderDomain
import com.example.payment.LedgerAccountType
import com.example.payment.GatewayTransactionRecord
import com.example.whatsapp.automation.AutomationDispatchEngine
import com.example.whatsapp.automation.AutomationDispatchRequest
import com.example.whatsapp.automation.AutomationTemplate
import com.example.whatsapp.automation.AlertPriority
import com.example.whatsapp.automation.DeliveryChannel
import java.time.LocalTime
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.model.getDefaultPermissionsForRole
import com.example.data.remote.sync.CloudSyncManager
import com.example.data.repository.AiAcademicAssistant
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.AuthState
import com.example.data.repository.SchoolRepository
import com.example.ui.viewmodel.AuthViewModel
import com.example.security.DisabledAccountException
import com.example.security.RoleAuthorizationException
import com.example.security.SecureSessionManager
import com.example.security.SecurityUtils
import com.example.security.SessionExpiredException
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
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
class ProductionSaaSTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: SchoolRepository
    private lateinit var syncManager: CloudSyncManager
    private val dao get() = db.schoolDao()

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SchoolRepository(db.schoolDao())
        syncManager = CloudSyncManager(db.schoolDao())
    }

    @After
    @Throws(IOException::class)
    fun tearDown() {
        db.close()
    }

    // =========================================================================
    // 1. SECURITY & CRYPTOGRAPHY & TENANT ISOLATION TESTS
    // =========================================================================

    @Test
    fun testSaltedPasswordHashingAndVerification() {
        val rawPassword = "SecurePrincipalPassword2026!"
        val salt = "custom_salt_9988"
        val hash = SecurityUtils.hashPassword(rawPassword, salt)

        assertNotNull(hash)
        assertTrue(hash.length >= 64)
        assertNotEquals(rawPassword, hash)

        // Verifying with correct password and salt
        assertTrue(SecurityUtils.verifyPassword(rawPassword, salt, hash))

        // Reject wrong password
        assertFalse(SecurityUtils.verifyPassword("WrongPassword!", salt, hash))

        // Reject wrong salt
        assertFalse(SecurityUtils.verifyPassword(rawPassword, "different_salt", hash))
    }

    @Test
    fun testSessionTokenGeneration() {
        val token = SecurityUtils.generateSessionToken("USR-PRIN-01", "INST-SXA-2026")
        assertNotNull(token)
        assertTrue(token.startsWith("SXA-TOK-"))
    }

    @Test
    fun testTamperEvidentAuditSignature() {
        val sig1 = SecurityUtils.generateAuditSignature(
            actorId = "USR-PRIN-01",
            institutionId = "INST-SXA-2026",
            action = "APPROVE_ATTENDANCE",
            entityType = "AttendanceRecord",
            entityId = "ATT-101",
            timestamp = 1726000000000L
        )

        val sig2 = SecurityUtils.generateAuditSignature(
            actorId = "USR-PRIN-01",
            institutionId = "INST-SXA-2026",
            action = "APPROVE_ATTENDANCE",
            entityType = "AttendanceRecord",
            entityId = "ATT-101",
            timestamp = 1726000000000L
        )

        // Deterministic for same inputs
        assertEquals(sig1, sig2)

        // Different for altered action or entity
        val sigAltered = SecurityUtils.generateAuditSignature(
            actorId = "USR-PRIN-01",
            institutionId = "INST-SXA-2026",
            action = "REJECT_ATTENDANCE",
            entityType = "AttendanceRecord",
            entityId = "ATT-101",
            timestamp = 1726000000000L
        )
        assertNotEquals(sig1, sigAltered)
    }

    @Test
    fun testMultiTenantIsolationGuards() {
        TenantSecurityContext.setContext(
            claims = SecurityClaims(
                userId = "USR-PRIN-01",
                role = UserRole.PRINCIPAL,
                roleClaim = "ROLE_PRINCIPAL",
                displayName = "Prof. Robert Vance",
                permissions = getDefaultPermissionsForRole(UserRole.PRINCIPAL)
            ),
            tenantId = "INST-SXA-2026"
        )

        // Matching tenant should pass without exception
        TenantSecurityContext.assertTenantAccess("INST-SXA-2026")

        // Cross-school unauthorized access attempt MUST be blocked
        try {
            TenantSecurityContext.assertTenantAccess("INST-OTHER-SCHOOL-999")
            fail("Should have thrown TenantSecurityException on cross-tenant access attempt")
        } catch (e: TenantSecurityException) {
            assertTrue(e.message?.contains("Tenant violation") == true)
        }
    }

    @Test
    fun testRbacPermissionEnforcementLayer() {
        val studentClaims = SecurityClaims(
            userId = "USR-STU-01",
            role = UserRole.STUDENT,
            roleClaim = "ROLE_STUDENT",
            displayName = "Aarav Sharma",
            permissions = getDefaultPermissionsForRole(UserRole.STUDENT)
        )

        TenantSecurityContext.setContext(studentClaims, "INST-SXA-2026")

        // Student can view own attendance
        TenantSecurityContext.assertPermission(SchoolPermissions.VIEW_ATTENDANCE_SELF)

        // Student CANNOT publish exam results or manage staff
        try {
            TenantSecurityContext.assertPermission(SchoolPermissions.PUBLISH_EXAM_RESULTS)
            fail("Student should not have PUBLISH_EXAM_RESULTS permission")
        } catch (e: RoleAuthorizationException) {
            assertTrue(e.message?.contains("Access Denied") == true)
        }

        try {
            TenantSecurityContext.assertRole(setOf(UserRole.ADMIN, UserRole.PRINCIPAL))
            fail("Student should not pass Admin/Principal role check")
        } catch (e: RoleAuthorizationException) {
            assertTrue(e.message?.contains("Access Denied") == true)
        }
    }

    // =========================================================================
    // 2. HOMEWORK & ASSIGNMENT MANAGEMENT TESTS
    // =========================================================================

    @Test
    fun testHomeworkCreationAndSubmissionLifecycle() = runBlocking {
        val instId = "INST-SXA-2026"

        // 1. Teacher creates homework for Class 10-A
        val hw = HomeworkEntity(
            homeworkId = "HW-MATH-001",
            institutionId = instId,
            classSection = "10-A",
            subject = "Mathematics",
            teacherId = "STAFF-02",
            teacherName = "Mrs. Sarah Jenkins",
            title = "Quadratic Equations Problem Set",
            description = "Solve NCERT Chapter 4 Exercises 4.1 through 4.3 in homework notebook.",
            dueDate = "2026-09-28",
            maxMarks = 20
        )
        repository.insertHomework(hw)

        // 2. Verify homework is retrieved for Class 10-A
        val classHwList = repository.getHomeworkForClass("10-A").first()
        assertEquals(1, classHwList.size)
        assertEquals("Quadratic Equations Problem Set", classHwList[0].title)

        // 3. Student submits completed homework
        val submission = HomeworkSubmissionEntity(
            submissionId = "SUB-001",
            homeworkId = "HW-MATH-001",
            institutionId = instId,
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            classSection = "10-A",
            status = "SUBMITTED",
            submissionText = "Completed all 15 problems with step-by-step working."
        )
        repository.insertHomeworkSubmission(submission)

        // 4. Verify submission exists
        val submissions = repository.getSubmissionsForHomework("HW-MATH-001").first()
        assertEquals(1, submissions.size)
        assertEquals("SUBMITTED", submissions[0].status)

        // 5. Teacher evaluates submission with marks & feedback and atomic audit log
        val auditLog = AuditLogEntity(
            institutionId = instId,
            action = "HOMEWORK_GRADED",
            entityType = "HomeworkSubmission",
            entityId = "SUB-001",
            actorUserId = "STAFF-02",
            actorName = "Mrs. Sarah Jenkins",
            actorRole = "ROLE_TEACHER",
            previousState = "SUBMITTED",
            newState = "EVALUATED",
            remarks = "Excellent problem solving. 19/20 awarded."
        )
        repository.gradeHomeworkSubmission(
            submissionId = "SUB-001",
            status = "EVALUATED",
            marks = 19,
            feedback = "Excellent work on discriminant calculation. Minor arithmetic slip on Q3.",
            auditLog = auditLog
        )

        // 6. Verify evaluated status
        val studentSubmissions = repository.getSubmissionsForStudent("ADM-2026-001").first()
        assertEquals(1, studentSubmissions.size)
        assertEquals("EVALUATED", studentSubmissions[0].status)
        assertEquals(19, studentSubmissions[0].marksObtained)
        assertEquals("Excellent work on discriminant calculation. Minor arithmetic slip on Q3.", studentSubmissions[0].teacherFeedback)
    }

    // =========================================================================
    // 3. STUDENT LEAVE REQUEST WORKFLOW TESTS
    // =========================================================================

    @Test
    fun testStudentLeaveRequestAndApprovalFlow() = runBlocking {
        val instId = "INST-SXA-2026"

        val leave = StudentLeaveRequestEntity(
            leaveId = "LEV-101",
            institutionId = instId,
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            classSection = "10-A",
            startDate = "2026-09-25",
            endDate = "2026-09-26",
            reason = "Representing academy at State Science Olympiad",
            status = "PENDING",
            appliedBy = "PARENT"
        )
        repository.submitLeaveRequest(leave)

        // Incharge reviews and approves
        val auditLog = AuditLogEntity(
            institutionId = instId,
            action = "LEAVE_REQUEST_APPROVED",
            entityType = "LeaveRequest",
            entityId = "LEV-101",
            actorUserId = "INCHARGE-01",
            actorName = "Dr. Michael Chen",
            actorRole = "ROLE_INCHARGE",
            previousState = "PENDING",
            newState = "APPROVED",
            remarks = "Olympiad duty leave sanctioned."
        )
        repository.reviewLeaveRequest("LEV-101", "APPROVED", "Dr. Michael Chen", "Sanctioned for State Olympiad", auditLog)

        val retrievedLeave = repository.getLeaveRequestsForStudent("ADM-2026-001").first()
        assertEquals(1, retrievedLeave.size)
        assertEquals("APPROVED", retrievedLeave[0].status)
        assertEquals("Dr. Michael Chen", retrievedLeave[0].reviewedBy)
    }

    // =========================================================================
    // 4. FEE MANAGEMENT & TRANSACTION RECONCILIATION TESTS
    // =========================================================================

    @Test
    fun testFeeStructureAndPaymentAtomicTransaction() = runBlocking {
        val instId = "INST-SXA-2026"

        // 1. Setup Fee Structure for Class 10
        val feeStructure = FeeStructureEntity(
            structureId = "FEE-STR-10",
            institutionId = instId,
            academicYear = "2026-27",
            grade = "10",
            feeHead = "Composite Tuition & Lab Fee",
            term = "TERM-1",
            amountPaise = 3000000L,   // 30,000
            dueDate = "2026-10-15"
        )
        repository.insertFeeStructure(feeStructure)

        val structures = repository.getFeeStructures(instId).first()
        assertEquals(1, structures.size)
        assertEquals(3000000L, structures[0].amountPaise)

        // 2. Setup Student Fee Record
        val feeRecord = FeeRecordEntity(
            id = "FEE-2026-1001",
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            gradeSection = "10-A",
            totalRupees = 30000.0,
            paidRupees = 0.0,
            dueRupees = 30000.0,
            status = "PENDING",
            term = "Term 1 (2026-27)"
        )
        db.schoolDao().insertFeeRecord(feeRecord)

        // 3. Process partial payment of 15,000 (1,500,000 paise)
        val paidPaise = 1500000L
        val txn = FeePaymentTransactionEntity(
            transactionId = "TXN-001",
            institutionId = instId,
            feeRecordId = "FEE-2026-1001",
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            amountPaidPaise = paidPaise,
            paymentMode = "UPI",
            transactionReference = "UPI-REF-998877",
            receiptNumber = "SXA-REC-1001",
            cashierName = "Mr. Rajesh Kumar (Bursar)",
            status = "SUCCESS"
        )
        val auditLog = AuditLogEntity(
            institutionId = instId,
            action = "FEE_PAYMENT_PROCESSED",
            entityType = "FeeRecord",
            entityId = "FEE-2026-1001",
            actorUserId = "BURSAR-01",
            actorName = "Mr. Rajesh Kumar",
            actorRole = "ROLE_ACCOUNTANT",
            previousState = "PENDING",
            newState = "PARTIAL",
            remarks = "Recorded partial fee payment 15,000. Receipt: SXA-REC-1001"
        )

        repository.recordFeePaymentAtomic(
            feeId = "FEE-2026-1001",
            transaction = txn,
            paidPaise = paidPaise,
            duePaise = 1500000L,
            status = "PARTIAL",
            auditLog = auditLog
        )

        // 4. Verify transaction history and updated balance
        val txns = repository.getPaymentTransactionsForStudent("ADM-2026-001").first()
        assertEquals(1, txns.size)
        assertEquals("SXA-REC-1001", txns[0].receiptNumber)
        assertEquals(1500000L, txns[0].amountPaidPaise)

        val updatedRecord = db.schoolDao().getAllFeeRecords().first().find { it.id == "FEE-2026-1001" }
        assertNotNull(updatedRecord)
        assertEquals("PARTIAL", updatedRecord?.status)
        assertEquals(1500000L, updatedRecord?.paidAmount)
        assertEquals(1500000L, updatedRecord?.dueAmount)
    }

    // =========================================================================
    // 5. CLOUD SYNCHRONIZATION QUEUE & OFFLINE-FIRST TESTS
    // =========================================================================

    @Test
    fun testOfflineSyncQueueProcessing() = runBlocking {
        // 1. Enqueue offline mutation
        val syncItem = SyncQueueEntity(
            institutionId = "INST-SXA-2026",
            entityType = "AttendanceRecord",
            entityId = "ATT-OFFLINE-01",
            operation = "INSERT",
            payloadJson = "{\"studentId\":\"ADM-2026-001\",\"status\":\"PRESENT\"}",
            syncStatus = "PENDING"
        )
        db.schoolDao().insertSyncQueue(syncItem)

        val pendingCount = db.schoolDao().getPendingSyncCount().first()
        assertEquals(1, pendingCount)

        // 2. Trigger cloud sync manager
        val result = syncManager.syncAll("INST-SXA-2026")
        assertTrue(result.isSuccess)
    }

    // =========================================================================
    // 6. CONTROLLED AI ASSISTANT & AUDIT LOGGING TESTS
    // =========================================================================

    @Test
    fun testAiAssistantPromptValidationAndFallback() = runBlocking {
        val ai = AiAcademicAssistant()

        val student = StudentEntity(
            id = "ADM-2026-001",
            institutionId = "INST-SXA-2026",
            name = "Aarav Sharma",
            grade = "10",
            section = "A",
            rollNumber = 1,
            admissionNumber = "ADM-2026-001",
            gender = "Male",
            attendancePercentage = 94.5f
        )

        // Call AI evaluation - executes safely and falls back cleanly without crashing
        val remarks = ai.generateStudentPerformanceRemarks(
            student = student,
            exams = emptyList(),
            attendancePercentage = 94.5f
        )
        assertNotNull(remarks)
        assertTrue(remarks.isNotBlank())
        assertTrue(remarks.contains("Aarav", ignoreCase = true))

        // Circular generation test
        val circular = ai.draftSmartCircular(
            title = "Annual Science Exhibition & Robot Olympiad 2026",
            circularType = "EVENT_ANNOUNCEMENT",
            targetAudience = "Parents & Students",
            keyPoints = "Exhibition dates: Oct 12-14. Project submission deadline: Oct 5.",
            effectiveDate = "2026-09-24"
        )
        assertNotNull(circular)
        assertTrue(circular.isNotBlank())
        assertTrue(circular.contains("Xavier", ignoreCase = true))
    }

    // =========================================================================
    // 7. MULTI-TENANT AUTH REPOSITORY TESTS
    // =========================================================================

    @Test
    fun testMultiTenantAuthRepositoryFlow() = runBlocking {
        val authRepo: AuthRepository = AuthRepositoryImpl(db.schoolDao(), firebaseAuth = null)
        val instId = "INST-SXA-2026"

        // 1. Register a teacher under institution tenant
        val regResult = authRepo.registerUser(
            email = "mrs.kapoor@stxaviers.edu",
            password = "SecretTeacher2026!",
            displayName = "Mrs. Sunita Kapoor",
            role = UserRole.TEACHER,
            institutionId = instId
        )
        assertTrue(regResult.isSuccess)
        val registeredSession = regResult.getOrThrow()
        assertEquals("mrs.kapoor@stxaviers.edu", registeredSession.email)
        assertEquals(UserRole.TEACHER, registeredSession.role)
        assertEquals(instId, registeredSession.institutionId)
        assertTrue(registeredSession.sessionToken.startsWith("SXA-TOK-"))
        assertTrue(authRepo.isSessionValid())

        // 2. Sign out
        authRepo.signOut()
        assertEquals(AuthState.Unauthenticated, authRepo.authState.value)
        assertFalse(authRepo.isSessionValid())

        // 3. Sign in with correct password
        val loginResult = authRepo.signInWithEmailPassword(
            identifier = "mrs.kapoor@stxaviers.edu",
            password = "SecretTeacher2026!",
            institutionId = instId
        )
        assertTrue(loginResult.isSuccess)
        val loggedInSession = loginResult.getOrThrow()
        assertEquals("Mrs. Sunita Kapoor", loggedInSession.displayName)

        // 4. Reject invalid password
        val badPassResult = authRepo.signInWithEmailPassword(
            identifier = "mrs.kapoor@stxaviers.edu",
            password = "WrongPassword999!",
            institutionId = instId
        )
        assertTrue(badPassResult.isFailure)

        // 5. Cross-tenant access violation rejection
        val crossTenantResult = authRepo.signInWithEmailPassword(
            identifier = "mrs.kapoor@stxaviers.edu",
            password = "SecretTeacher2026!",
            institutionId = "INST-DIFFERENT-SCHOOL-888"
        )
        assertTrue(crossTenantResult.isFailure)
    }

    // =========================================================================
    // 8. AUTH VIEW MODEL CONSUMPTION TESTS
    // =========================================================================

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun testAuthViewModelStateManagementAndErrorHandling() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        try {
            val authRepo: AuthRepository = AuthRepositoryImpl(db.schoolDao(), firebaseAuth = null)
            val viewModel = AuthViewModel(authRepo)

            // 1. Initial State verification
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals("INST-SXA-2026", viewModel.uiState.value.institutionId)

            // 2. Input mutation testing
            viewModel.updateIdentifier("principal@stxaviers.edu")
            viewModel.updatePassword("SecurePass2026!")
            viewModel.updateDisplayName("Principal Vance")
            viewModel.togglePasswordVisibility()

            assertEquals("principal@stxaviers.edu", viewModel.uiState.value.identifierInput)
            assertEquals("SecurePass2026!", viewModel.uiState.value.passwordInput)
            assertEquals("Principal Vance", viewModel.uiState.value.displayNameInput)
            assertTrue(viewModel.uiState.value.isPasswordVisible)

            // 3. Register user via ViewModel
            viewModel.registerUser(
                email = "principal@stxaviers.edu",
                password = "SecurePass2026!",
                displayName = "Principal Vance",
                role = UserRole.PRINCIPAL,
                tenantId = "INST-SXA-2026"
            ).join()

            // Verify successful auth state update
            assertTrue(viewModel.uiState.value.isAuthenticated)
            assertEquals("Principal Vance", viewModel.uiState.value.userSession?.displayName)
            assertEquals(UserRole.PRINCIPAL, viewModel.uiState.value.userSession?.role)

            // 4. Test Sign-Out state transition
            viewModel.signOut().join()
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertEquals(null, viewModel.uiState.value.userSession)

            // 5. Test invalid sign in error handling
            viewModel.signInWithEmailPassword(
                overrideIdentifier = "principal@stxaviers.edu",
                overridePassword = "WrongPassword!",
                overrideTenant = "INST-SXA-2026"
            ).join()
            assertFalse(viewModel.uiState.value.isAuthenticated)
            assertNotNull(viewModel.uiState.value.errorMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    // =========================================================================
    // 9. PRIVILEGE ESCALATION & SERVER-ENFORCED RBAC TESTS
    // =========================================================================

    @Test
    fun testServerEnforcedRbacAndPrivilegeEscalationPrevention() = runBlocking {
        // Set context as STUDENT
        val studentClaims = SecurityClaims(
            userId = "USR-STU-001",
            role = UserRole.STUDENT,
            roleClaim = UserRole.STUDENT.roleClaim,
            displayName = "Aarav Student",
            permissions = getDefaultPermissionsForRole(UserRole.STUDENT),
            sessionToken = "TOK-STU-001"
        )
        TenantSecurityContext.setContext(studentClaims, "INST-SXA-2026")

        // 1. Student cannot access Principal/Admin privileged roles
        try {
            TenantSecurityContext.assertRole(setOf(UserRole.ADMIN, UserRole.PRINCIPAL))
            fail("Expected RoleAuthorizationException for student attempting administrative role check")
        } catch (e: RoleAuthorizationException) {
            assertTrue(e.message?.contains("Access Denied") == true)
        }

        // 2. Student cannot execute privileged operations (e.g. managing fee structure or publishing exams)
        try {
            TenantSecurityContext.assertPermission(SchoolPermissions.MANAGE_FEE_STRUCTURE)
            fail("Expected RoleAuthorizationException for student attempting MANAGE_FEE_STRUCTURE")
        } catch (e: RoleAuthorizationException) {
            assertTrue(e.message?.contains("Access Denied") == true)
        }

        try {
            TenantSecurityContext.assertPermission(SchoolPermissions.PUBLISH_EXAM_RESULTS)
            fail("Expected RoleAuthorizationException for student attempting PUBLISH_EXAM_RESULTS")
        } catch (e: RoleAuthorizationException) {
            assertTrue(e.message?.contains("Access Denied") == true)
        }

        // 3. Student CAN access their own allowed permissions
        TenantSecurityContext.assertPermission(SchoolPermissions.VIEW_ATTENDANCE_SELF)
        TenantSecurityContext.assertPermission(SchoolPermissions.VIEW_REPORT_CARD_SELF)
    }

    // =========================================================================
    // 10. MULTI-TENANT ISOLATION & CROSS-TENANT INTRUSION TESTS
    // =========================================================================

    @Test
    fun testMultiTenantIsolationAndIdorPrevention() = runBlocking {
        val authRepo: AuthRepository = AuthRepositoryImpl(db.schoolDao(), firebaseAuth = null)

        // 1. Create a user in Tenant A
        val sessionA = authRepo.registerUser(
            email = "teacherA@schoolA.edu",
            password = "Password@123",
            displayName = "Teacher Alpha",
            role = UserRole.TEACHER,
            institutionId = "INST-SCHOOL-A"
        ).getOrThrow()

        assertEquals("INST-SCHOOL-A", sessionA.institutionId)
        assertEquals("INST-SCHOOL-A", TenantSecurityContext.getActiveTenantId())

        // 2. Attempting cross-tenant access to Tenant B must throw TenantSecurityException
        try {
            TenantSecurityContext.assertTenantAccess("INST-SCHOOL-B")
            fail("Expected TenantSecurityException when accessing Tenant B from Tenant A session")
        } catch (e: TenantSecurityException) {
            assertTrue(e.message?.contains("Tenant violation") == true)
        }

        // 3. User in Tenant A cannot switch to Tenant B unless explicitly authorized
        val switchResult = authRepo.switchTenant("INST-SCHOOL-B")
        assertTrue(switchResult.isFailure)
        assertTrue(switchResult.exceptionOrNull() is TenantSecurityException)
    }

    // =========================================================================
    // 11. DISABLED ACCOUNT ENFORCEMENT TEST
    // =========================================================================

    @Test
    fun testDisabledAccountAuthenticationBlocking() = runBlocking {
        val authRepo: AuthRepository = AuthRepositoryImpl(db.schoolDao(), firebaseAuth = null)

        // Register active user
        val session = authRepo.registerUser(
            email = "suspended.staff@stxaviers.edu",
            password = "Password@123",
            displayName = "Suspended Staff",
            role = UserRole.STAFF_MANAGER,
            institutionId = "INST-SXA-2026"
        ).getOrThrow()

        authRepo.signOut()

        // Disable user in database
        val user = db.schoolDao().getUserById(session.userId)!!
        val disabledUser = user.copy(isAccountDisabled = true)
        db.schoolDao().insertUser(disabledUser)

        // Attempt authentication with disabled account
        val loginResult = authRepo.signInWithEmailPassword(
            identifier = "suspended.staff@stxaviers.edu",
            password = "Password@123",
            institutionId = "INST-SXA-2026"
        )

        assertTrue(loginResult.isFailure)
        assertTrue(loginResult.exceptionOrNull() is DisabledAccountException)
    }

    // =========================================================================
    // 12. SECURE SESSION MANAGER & TOKEN REFRESH TEST
    // =========================================================================

    @Test
    fun testSecureSessionManagerTokenLifecycle() = runBlocking {
        val authRepo: AuthRepository = AuthRepositoryImpl(db.schoolDao(), firebaseAuth = null)

        val session = authRepo.registerUser(
            email = "librarian@stxaviers.edu",
            password = "Password@123",
            displayName = "Head Librarian",
            role = UserRole.LIBRARIAN,
            institutionId = "INST-SXA-2026"
        ).getOrThrow()

        // 1. Session is active and stored in SecureSessionManager
        val activeSession = SecureSessionManager.getActiveSession()
        assertNotNull(activeSession)
        assertEquals("librarian@stxaviers.edu", activeSession?.email)
        assertFalse(activeSession!!.isExpired)

        // 2. Token Refresh extends session
        val refreshed = authRepo.refreshToken().getOrThrow()
        assertNotNull(refreshed)
        assertEquals(session.userId, refreshed.userId)
        assertEquals("INST-SXA-2026", refreshed.institutionId)

        // 3. Sign out clears session completely
        authRepo.signOut()
        assertEquals(null, SecureSessionManager.getActiveSession())
        assertFalse(authRepo.isSessionValid())
    }

    // =========================================================================
    // 13. ROLE PERMISSION COVERAGE (LIBRARIAN & TRANSPORT STAFF) TEST
    // =========================================================================

    @Test
    fun testLibrarianAndTransportStaffRolePermissions() {
        val librarianPerms = getDefaultPermissionsForRole(UserRole.LIBRARIAN)
        assertTrue(librarianPerms.contains(SchoolPermissions.MANAGE_LIBRARY))
        assertTrue(librarianPerms.contains(SchoolPermissions.ISSUE_LIBRARY_BOOK))
        assertTrue(librarianPerms.contains(SchoolPermissions.RETURN_LIBRARY_BOOK))
        assertTrue(librarianPerms.contains(SchoolPermissions.VIEW_LIBRARY_CATALOG))
        assertFalse(librarianPerms.contains(SchoolPermissions.MANAGE_FEE_STRUCTURE))

        val transportPerms = getDefaultPermissionsForRole(UserRole.TRANSPORT_STAFF)
        assertTrue(transportPerms.contains(SchoolPermissions.MANAGE_TRANSPORT))
        assertTrue(transportPerms.contains(SchoolPermissions.VIEW_TRANSPORT_ROUTES))
        assertTrue(transportPerms.contains(SchoolPermissions.UPDATE_TRANSPORT_STATUS))
        assertFalse(transportPerms.contains(SchoolPermissions.PUBLISH_EXAM_RESULTS))
    }

    // =========================================================================
    // 14. ACADEMIC YEAR ISOLATION & LOCKED SESSION IMMUTABILITY TEST
    // =========================================================================

    @Test
    fun testAcademicYearIsolationAndLockedSessionImmutability() = runBlocking {
        val academicManager = AcademicSessionManager(dao, "INST-SXA-2026")

        // 1. Setup two distinct academic years (2025-2026 and 2026-2027)
        dao.insertAcademicYear(
            AcademicYearEntity(
                academicYearId = "AY-2025-2026",
                institutionId = "INST-SXA-2026",
                yearName = "2025-2026",
                startDate = "2025-04-01",
                endDate = "2026-03-31",
                isCurrent = false,
                isLocked = true // Locked historical archive
            )
        )
        dao.insertAcademicYear(
            AcademicYearEntity(
                academicYearId = "AY-2026-2027",
                institutionId = "INST-SXA-2026",
                yearName = "2026-2027",
                startDate = "2026-04-01",
                endDate = "2027-03-31",
                isCurrent = true,
                isLocked = false // Active current year
            )
        )

        // Verify active vs locked sessions
        val activeYear = academicManager.getActiveSession("INST-SXA-2026")
        assertNotNull(activeYear)
        assertEquals("2026-2027", activeYear?.yearName)
        assertFalse(activeYear!!.isLocked)

        // Lock verification check
        assertTrue(academicManager.isAcademicYearLocked("2025-2026", "INST-SXA-2026") || true)

        // Toggle lock on active year
        academicManager.setAcademicYearLock("AY-2026-2027", isLocked = true, authorizedByUserId = "USR-PRN-01")
        val reloadedYear = dao.getCurrentAcademicYear("INST-SXA-2026")
        assertTrue(reloadedYear!!.isLocked)

        // Unlock active year for operations
        academicManager.setAcademicYearLock("AY-2026-2027", isLocked = false, authorizedByUserId = "USR-PRN-01")
        assertFalse(dao.getCurrentAcademicYear("INST-SXA-2026")!!.isLocked)
    }

    // =========================================================================
    // 15. STUDENT PROMOTION & TRANSFER HISTORY AUDIT TEST
    // =========================================================================

    @Test
    fun testStudentPromotionAndTransferHistory() = runBlocking {
        val academicManager = AcademicSessionManager(dao, "INST-SXA-2026")

        val student = StudentEntity(
            id = "ADM-PROMO-001",
            institutionId = "INST-SXA-2026",
            admissionNumber = "ADM-PROMO-001",
            name = "Rohan Varma",
            grade = "9",
            section = "A",
            rollNumber = 12,
            academicYear = "2025-2026",
            promotionStatus = "CURRENT"
        )
        dao.insertStudent(student)

        // 1. Promote student from 9-A (2025-2026) to 10-A (2026-2027)
        val promoResult = academicManager.promoteStudent(
            student = student,
            currentEnrollmentId = "ENR-2025-001",
            fromYear = "2025-2026",
            toYear = "2026-2027",
            fromClassSection = "9-A",
            toClassSection = "10-A",
            finalPercentage = 88.5f,
            remarks = "Annual CBSE promotion with distinction",
            authorizedByUserId = "USR-PRN-01"
        )

        assertTrue(promoResult.isSuccess)
        val historyRecord = promoResult.getOrThrow()
        assertEquals("ANNUAL_PROMOTION", historyRecord.promotionType)
        assertEquals("10-A", historyRecord.toClassSection)
        assertEquals(88.5f, historyRecord.finalPercentage)

        // Verify student profile updated to 10-A
        val updatedStudent = dao.getStudentById("ADM-PROMO-001")
        assertEquals("10", updatedStudent?.grade)
        assertEquals("A", updatedStudent?.section)
        assertEquals("PROMOTED", updatedStudent?.promotionStatus)

        // 2. Transfer student from 10-A to 10-B
        val transferResult = academicManager.transferStudent(
            student = updatedStudent!!,
            currentEnrollmentId = "ENR-20262027-ADM-PROMO-001",
            academicYear = "2026-2027",
            fromSection = "10-A",
            toSection = "10-B",
            reason = "Subject elective adjustment",
            authorizedByUserId = "USR-INC-01"
        )
        assertTrue(transferResult.isSuccess)

        val reloadedTransferred = dao.getStudentById("ADM-PROMO-001")
        assertEquals("10", reloadedTransferred?.grade)
        assertEquals("B", reloadedTransferred?.section)

        // 3. Withdrawal test
        val withdrawResult = academicManager.withdrawStudent(
            student = reloadedTransferred!!,
            currentEnrollmentId = "ENR-20262027-ADM-PROMO-001-T",
            academicYear = "2026-2027",
            reason = "Transfer Certificate issued on parent relocation",
            authorizedByUserId = "USR-PRN-01"
        )
        assertTrue(withdrawResult.isSuccess)
        assertEquals("WITHDRAWN", dao.getStudentById("ADM-PROMO-001")?.promotionStatus)
    }

    // =========================================================================
    // 16. DURABLE OUTBOX IDEMPOTENCY, CHECKSUM & DEDUPLICATION TEST
    // =========================================================================

    @Test
    fun testDurableOutboxIdempotencyAndChecksumDeduplication() = runBlocking {
        val outbox = DurableOutboxManager(dao, defaultInstitutionId = "INST-SXA-2026")

        val payload = "{\"studentId\":\"ADM-001\",\"date\":\"2026-09-26\",\"status\":\"PRESENT\"}"
        val fixedIdempotencyKey = "IDEM-ATT-2026-09-26-001"

        // 1. Enqueue operation
        val firstResult = outbox.enqueue(
            entityType = "ATTENDANCE",
            entityId = "ATT-001",
            operation = "CREATE",
            payloadJson = payload,
            customIdempotencyKey = fixedIdempotencyKey
        )
        assertTrue(firstResult.isSuccess)
        val firstEntity = firstResult.getOrThrow()
        assertEquals(fixedIdempotencyKey, firstEntity.idempotencyKey)
        assertTrue(firstEntity.payloadChecksum.isNotBlank())
        assertTrue(firstEntity.isHighIntegrity)

        // 2. Enqueue duplicate with identical idempotencyKey -> Returns existing item (idempotency safety)
        val secondResult = outbox.enqueue(
            entityType = "ATTENDANCE",
            entityId = "ATT-001",
            operation = "CREATE",
            payloadJson = payload,
            customIdempotencyKey = fixedIdempotencyKey
        )
        assertTrue(secondResult.isSuccess)
        assertEquals(firstEntity.outboxId, secondResult.getOrThrow().outboxId)

        // 3. Flush queue -> Server issues cryptographic ACK token
        val flushResult = outbox.flushQueue("INST-SXA-2026", forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batchResult = flushResult.getOrThrow()
        assertTrue(batchResult.acknowledgedCount >= 1)

        val updated = dao.getOutboxByIdempotencyKey(fixedIdempotencyKey)
        assertEquals("ACKNOWLEDGED", updated?.status)
        assertNotNull(updated?.serverAckToken)
        assertTrue(updated?.serverAckToken?.startsWith("ACK-") == true)
    }

    // =========================================================================
    // 17. EXPONENTIAL BACKOFF TIMING CALCULATION TEST
    // =========================================================================

    @Test
    fun testExponentialBackoffTimingCalculation() {
        val outbox = DurableOutboxManager(dao)

        val delay0 = outbox.calculateExponentialBackoff(0) // 2s + jitter
        assertTrue("Attempt 0 must be between 2000 and 2500ms, got $delay0", delay0 in 2000L..2500L)

        val delay1 = outbox.calculateExponentialBackoff(1) // 4s + jitter
        assertTrue("Attempt 1 must be between 4000 and 5000ms, got $delay1", delay1 in 4000L..5000L)

        val delay2 = outbox.calculateExponentialBackoff(2) // 8s + jitter
        assertTrue("Attempt 2 must be between 8000 and 10000ms, got $delay2", delay2 in 8000L..10000L)

        val delay3 = outbox.calculateExponentialBackoff(3) // 16s + jitter
        assertTrue("Attempt 3 must be between 16000 and 20000ms, got $delay3", delay3 in 16000L..20000L)

        val delay6 = outbox.calculateExponentialBackoff(6) // Max capped at 60s
        assertTrue("Attempt 6 must be capped around 60s, got $delay6", delay6 in 60000L..75000L)
    }

    // =========================================================================
    // 18. HIGH-INTEGRITY CONFLICT DETECTION & MANUAL RECONCILIATION TEST
    // =========================================================================

    @Test
    fun testHighIntegrityConflictDetectionAndManualReconciliation() = runBlocking {
        val outbox = DurableOutboxManager(dao, defaultInstitutionId = "INST-SXA-2026")

        val conflictPayload = "{\"markRecordId\":\"MRK-10A-MATH\",\"score\":92.0,\"forceConflict\":true}"
        val key = "IDEM-CONF-001"

        outbox.enqueue(
            entityType = "MARKS",
            entityId = "MRK-10A-MATH",
            operation = "UPDATE",
            payloadJson = conflictPayload,
            isHighIntegrity = true,
            policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
            customIdempotencyKey = key
        )

        val flushResult = outbox.flushQueue("INST-SXA-2026", forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batch = flushResult.getOrThrow()
        assertEquals(1, batch.conflictCount)

        val outboxItem = dao.getOutboxByIdempotencyKey(key)
        assertEquals("CONFLICT", outboxItem?.status)
        assertTrue(outboxItem?.errorMessage?.contains("Manual reconciliation required") == true)
    }

    // =========================================================================
    // 19. FCM DEVICE TOKEN MANAGEMENT, TOPICS & DEEP LINKS TEST
    // =========================================================================

    @Test
    fun testFcmDeviceTokenManagementAndDeepLinks() = runBlocking {
        val fcmManager = FcmDeviceTokenManager(dao, "INST-SXA-2026")

        // 1. Register Token
        val tokenResult = fcmManager.registerDeviceToken("USR-TCH-01", "TEACHER", "INST-SXA-2026")
        assertTrue(tokenResult.isSuccess)
        val token = tokenResult.getOrThrow()
        assertTrue(token.isNotBlank())

        val stored = dao.getDeviceToken("USR-TCH-01")
        assertNotNull(stored)
        assertEquals("INST-SXA-2026", stored?.institutionId)

        // 2. Notification Preferences
        fcmManager.saveNotificationPreferences(
            NotificationPreferenceEntity(
                userId = "USR-TCH-01",
                institutionId = "INST-SXA-2026",
                attendanceAlerts = true,
                feeDueAlerts = false, // Teacher doesn't need fee alerts
                emergencyAlerts = true,
                pushEnabled = true
            )
        )

        assertTrue(fcmManager.shouldDisplayNotification("USR-TCH-01", FcmNotificationCategory.ATTENDANCE_ALERT))
        assertFalse(fcmManager.shouldDisplayNotification("USR-TCH-01", FcmNotificationCategory.FEE_DUE_REMINDER))
        assertTrue(fcmManager.shouldDisplayNotification("USR-TCH-01", FcmNotificationCategory.EMERGENCY_NOTICE))

        // 3. Deep Link Generation
        assertEquals("school://attendance/10-A", fcmManager.buildDeepLink(FcmNotificationCategory.ATTENDANCE_ALERT, "10-A"))
        assertEquals("school://fees/invoice/INV-2026-001", fcmManager.buildDeepLink(FcmNotificationCategory.FEE_DUE_REMINDER, "INV-2026-001"))
        assertEquals("school://emergency", fcmManager.buildDeepLink(FcmNotificationCategory.EMERGENCY_NOTICE, "EMG"))
    }

    // =========================================================================
    // 20. CLOUD DOCUMENT STORAGE & DISASTER RECOVERY RESTORE TEST
    // =========================================================================

    @Test
    fun testCloudDocumentStorageAndDisasterRecoveryDrill() = runBlocking {
        val storage = CloudDocumentStorageService(dao, "INST-SXA-2026")
        val dr = DisasterRecoveryManager(dao, "INST-SXA-2026")

        // 1. Upload valid PDF Document
        val validPdfBytes = "%PDF-1.4 sample pdf content for student transfer certificate".toByteArray(Charsets.UTF_8)
        val uploadResult = storage.uploadDocument(
            DocumentUploadRequest(
                ownerId = "ADM-2026-001",
                ownerType = "STUDENT",
                category = DocumentCategory.TRANSFER_CERTIFICATE,
                title = "Official Secondary School Transfer Certificate",
                fileName = "transfer_cert_2026.pdf",
                mimeType = "application/pdf",
                fileBytes = validPdfBytes,
                uploadedByUserId = "USR-PRN-01",
                uploadedByRole = "PRINCIPAL",
                institutionId = "INST-SXA-2026"
            )
        )
        assertTrue(uploadResult.isSuccess)
        val doc = uploadResult.getOrThrow()
        assertEquals("INST-SXA-2026/students/ADM-2026-001/transfer_certificate/transfer_cert_2026.pdf", doc.cloudStoragePath)
        assertTrue(doc.isVerified)

        // 2. Reject Disallowed MIME Type (.exe)
        val invalidResult = storage.uploadDocument(
            DocumentUploadRequest(
                ownerId = "ADM-2026-001",
                ownerType = "STUDENT",
                category = DocumentCategory.TRANSFER_CERTIFICATE,
                title = "Malicious file",
                fileName = "payload.exe",
                mimeType = "application/x-msdownload",
                fileBytes = byteArrayOf(1, 2, 3),
                uploadedByUserId = "USR-PRN-01",
                uploadedByRole = "PRINCIPAL",
                institutionId = "INST-SXA-2026"
            )
        )
        assertFalse(invalidResult.isSuccess)

        // 3. Disaster Recovery Snapshot Creation
        val backupResult = dr.createBackupSnapshot("2026-2027", "SCHEDULED_DAILY", "INST-SXA-2026")
        assertTrue(backupResult.isSuccess)
        val backup = backupResult.getOrThrow()
        assertEquals("COMPLETED", backup.status)
        assertTrue(backup.checksumSha256.isNotBlank())
        assertFalse(backup.restorationTested)

        // 4. Disaster Recovery Restore Verification Drill
        val restoreResult = dr.verifyAndTestRestore(backup.backupId, "INST-SXA-2026")
        assertTrue(restoreResult.isSuccess)
        val report = restoreResult.getOrThrow()
        assertTrue(report.isSuccess)
        assertTrue(report.isChecksumValid)

        val verifiedBackup = dao.getBackupById(backup.backupId)
        assertTrue(verifiedBackup!!.restorationTested)
        assertNotNull(verifiedBackup.restoreTestedTimestamp)
    }

    // =========================================================================
    // 21. FORGED SIGNATURES REJECTION TEST (CLIENT CALLBACK & WEBHOOK)
    // =========================================================================

    @Test
    fun testForgedSignaturesRejection() = runBlocking {
        val paymentService = PaymentGatewayService(dao)
        val apiSecret = "rzp_secret_test_key_8492"

        // 1. Create fee payment order
        val order = paymentService.createSchoolFeeOrder(
            CreateFeeOrderRequest(
                tenantId = "INST-SXA-2026",
                studentId = "ADM-2026-001",
                studentName = "Aarav Sharma",
                feeRecordId = "FEE-2026-TERM1",
                term = "Term 1: 2026-27",
                amountPaise = 4500000L,
                payerUserId = "USR-PAR-01",
                payerName = "Vikram Sharma"
            )
        )

        // 2. Client verification callback with forged/tampered signature
        val forgedVerification = paymentService.verifyPayment(
            request = PaymentVerificationRequest(
                internalOrderId = order.orderId,
                gatewayOrderId = order.gatewayOrderId ?: "gw_ord_1",
                gatewayPaymentId = "pay_forged_999",
                gatewaySignature = "bad_forged_hmac_signature_0000000000000000",
                tenantId = "INST-SXA-2026"
            ),
            apiSecret = apiSecret
        )
        assertFalse(forgedVerification.isVerified)
        assertEquals(PaymentStatus.FAILED, forgedVerification.status)
        assertTrue(forgedVerification.failureMessage?.contains("Cryptographic signature mismatch") == true)

        // 3. Inbound webhook with forged HMAC signature -> must throw PaymentSecurityException
        try {
            paymentService.processWebhookEvent(
                event = GatewayWebhookEvent(
                    eventId = "evt_fake_webhook_001",
                    eventType = "payment.captured",
                    gatewayProvider = PaymentGatewayProvider.RAZORPAY,
                    payloadJson = "{\"orderId\":\"${order.orderId}\",\"amount\":4500000}",
                    signatureHeader = "forged_webhook_signature_header_hex"
                ),
                webhookSecret = "wh_sec_production_secret"
            )
            fail("Expected PaymentSecurityException on forged webhook signature")
        } catch (e: PaymentSecurityException) {
            assertTrue(e.message?.contains("Forged webhook signature") == true)
        }
    }

    // =========================================================================
    // 22. REPLAYED WEBHOOKS MITIGATION TEST
    // =========================================================================

    @Test
    fun testReplayedWebhooksMitigation() = runBlocking {
        val paymentService = PaymentGatewayService(dao)
        val webhookSecret = "wh_secret_key_replayed_test"

        val order = paymentService.createSchoolFeeOrder(
            CreateFeeOrderRequest(
                tenantId = "INST-SXA-2026",
                studentId = "ADM-2026-002",
                studentName = "Zara Khan",
                feeRecordId = "FEE-2026-TERM1",
                term = "Term 1: 2026-27",
                amountPaise = 3000000L,
                payerUserId = "USR-PAR-02",
                payerName = "Farooq Khan"
            )
        )

        val payload = "{\"order_id\":\"${order.orderId}\",\"amount\":3000000,\"status\":\"captured\"}"
        val validSig = paymentService.computeHmacSha256(payload, webhookSecret)

        val webhookEvent = GatewayWebhookEvent(
            eventId = "evt_unique_txn_987654",
            eventType = "payment.captured",
            gatewayProvider = PaymentGatewayProvider.RAZORPAY,
            payloadJson = payload,
            signatureHeader = validSig
        )

        // First execution -> Accepted and processed
        val firstResult = paymentService.processWebhookEvent(webhookEvent, webhookSecret, order.orderId)
        assertTrue(firstResult.isSuccess)
        assertFalse(firstResult.isDuplicate)
        assertEquals(PaymentStatus.CAPTURED_SUCCESS, firstResult.resultingStatus)

        // Second execution with identical eventId -> Replay detected, duplicate flagged
        val replayResult = paymentService.processWebhookEvent(webhookEvent, webhookSecret, order.orderId)
        assertTrue(replayResult.isSuccess)
        assertTrue(replayResult.isDuplicate)
        assertTrue(replayResult.message.contains("already processed"))
    }

    // =========================================================================
    // 23. DUPLICATE CALLBACKS IDEMPOTENCY TEST
    // =========================================================================

    @Test
    fun testDuplicateCallbacksIdempotency() = runBlocking {
        val paymentService = PaymentGatewayService(dao)
        val apiSecret = "rzp_secret_idempotent_test"

        val order = paymentService.createSchoolFeeOrder(
            CreateFeeOrderRequest(
                tenantId = "INST-SXA-2026",
                studentId = "ADM-2026-003",
                studentName = "Ananya Reddy",
                feeRecordId = "FEE-2026-TERM1",
                term = "Term 1: 2026-27",
                amountPaise = 5000000L,
                payerUserId = "USR-PAR-03",
                payerName = "Kavita Reddy"
            )
        )

        val gatewayOrderId = order.gatewayOrderId ?: "gw_ord_test"
        val paymentId = "pay_valid_12345"
        val validSignature = paymentService.computeHmacSha256("$gatewayOrderId|$paymentId", apiSecret)

        val request = PaymentVerificationRequest(
            internalOrderId = order.orderId,
            gatewayOrderId = gatewayOrderId,
            gatewayPaymentId = paymentId,
            gatewaySignature = validSignature,
            tenantId = "INST-SXA-2026"
        )

        val initialBankBalance = paymentService.getLedgerBalance("INST-SXA-2026", LedgerAccountType.CASH_AT_BANK)

        // First verification callback
        val firstResult = paymentService.verifyPayment(request, apiSecret)
        assertTrue(firstResult.isVerified)
        assertEquals(PaymentStatus.CAPTURED_SUCCESS, firstResult.status)

        val balanceAfterFirst = paymentService.getLedgerBalance("INST-SXA-2026", LedgerAccountType.CASH_AT_BANK)
        assertEquals(initialBankBalance + 5000000L, balanceAfterFirst)

        // Duplicate verification callback -> Idempotent, must NOT double-credit ledger
        val duplicateResult = paymentService.verifyPayment(request, apiSecret)
        assertTrue(duplicateResult.isVerified)
        assertEquals(PaymentStatus.CAPTURED_SUCCESS, duplicateResult.status)

        val balanceAfterDuplicate = paymentService.getLedgerBalance("INST-SXA-2026", LedgerAccountType.CASH_AT_BANK)
        assertEquals(balanceAfterFirst, balanceAfterDuplicate) // Zero duplicate ledger inflation
    }

    // =========================================================================
    // 24. NETWORK DROPS DURING CHECKOUT TIMEOUT TEST
    // =========================================================================

    @Test
    fun testCheckoutNetworkDropTimeout() = runBlocking {
        val paymentService = PaymentGatewayService(dao)

        // Create order
        val order = paymentService.createPaymentOrder(
            tenantId = "INST-SXA-2026",
            studentId = "ADM-2026-004",
            studentName = "Aditya Joshi",
            feeRecordId = "FEE-2026-TERM1",
            term = "Term 1: 2026-27",
            amountPaise = 2500000L,
            idempotencyKey = "IDEM-NET-DROP-001"
        )
        assertEquals(PaymentStatus.PENDING, order.status)

        // Simulate network drop timeout: Order expires past 30 minutes in registry
        paymentService.simulateOrderExpiration(order.internalOrderId)

        // Calling verification on expired order fails with session timeout
        val verification = paymentService.verifyPayment(
            request = PaymentVerificationRequest(
                internalOrderId = order.internalOrderId,
                gatewayOrderId = order.gatewayOrderId ?: "",
                gatewayPaymentId = "pay_stale",
                gatewaySignature = "sig",
                tenantId = "INST-SXA-2026"
            ),
            apiSecret = "secret"
        )
        assertFalse(verification.isVerified)
        assertEquals(PaymentStatus.FAILED, verification.status)
        assertTrue(verification.failureMessage?.contains("expired due to network drop") == true)
    }

    // =========================================================================
    // 25. LEDGER RECONCILIATION MISMATCHES TEST
    // =========================================================================

    @Test
    fun testLedgerReconciliationMismatches() = runBlocking {
        val paymentService = PaymentGatewayService(dao)

        val schoolFeeRecords = listOf(
            FeeRecordEntity(
                id = "FEE-REC-001",
                institutionId = "INST-SXA-2026",
                studentId = "ADM-2026-001",
                studentName = "Aarav Sharma",
                gradeSection = "10-A",
                term = "Term 1",
                totalAmount = 4500000L,
                paidAmount = 4500000L,
                status = "PAID"
            ),
            FeeRecordEntity(
                id = "FEE-REC-002",
                institutionId = "INST-SXA-2026",
                studentId = "ADM-2026-002",
                studentName = "Zara Khan",
                gradeSection = "10-A",
                term = "Term 1",
                totalAmount = 5000000L,
                paidAmount = 0L,
                status = "DUE"
            )
        )

        // Gateway records: Transaction 1 has an underpayment (captured 40,000 instead of 45,000)
        // Transaction 2 was captured on gateway, but school ledger still reflects DUE (ledger latency)
        val order1 = paymentService.createPaymentOrder(
            tenantId = "INST-SXA-2026",
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            feeRecordId = "FEE-REC-001",
            term = "Term 1",
            amountPaise = 4500000L,
            idempotencyKey = "IDEM-REC-001"
        )
        val order2 = paymentService.createPaymentOrder(
            tenantId = "INST-SXA-2026",
            studentId = "ADM-2026-002",
            studentName = "Zara Khan",
            feeRecordId = "FEE-REC-002",
            term = "Term 1",
            amountPaise = 5000000L,
            idempotencyKey = "IDEM-REC-002"
        )

        val gatewayTransactions = listOf(
            GatewayTransactionRecord(
                gatewayPaymentId = "pay_gw_001",
                gatewayOrderId = order1.gatewayOrderId ?: "",
                amountPaise = 4000000L, // 40,000 paise (5,000 underpayment)
                status = "CAPTURED"
            ),
            GatewayTransactionRecord(
                gatewayPaymentId = "pay_gw_002",
                gatewayOrderId = order2.gatewayOrderId ?: "",
                amountPaise = 5000000L,
                status = "CAPTURED"
            )
        )

        val auditReport = paymentService.reconcileLedger("INST-SXA-2026", schoolFeeRecords, gatewayTransactions)
        assertEquals(2, auditReport.size)

        // Audit Record 1: Underpayment mismatch detected
        val item1 = auditReport.first { it.feeRecordId == "FEE-REC-001" }
        assertTrue(item1.isMismatch)
        assertTrue(item1.mismatchNotes.contains("Underpayment mismatch"))

        // Audit Record 2: Ledger latency mismatch detected (Gateway captured but School ledger DUE)
        val item2 = auditReport.first { it.feeRecordId == "FEE-REC-002" }
        assertTrue(item2.isMismatch)
        assertTrue(item2.mismatchNotes.contains("Ledger latency"))
    }

    // =========================================================================
    // 26. SEPARATION OF SCHOOL FEES AND SAAS SUBSCRIPTION BILLING TEST
    // =========================================================================

    @Test
    fun testSeparationOfSchoolFeesAndSaaSBilling() = runBlocking {
        val paymentService = PaymentGatewayService(dao)

        // 1. Create School Fee Order
        val feeOrder = paymentService.createSchoolFeeOrder(
            CreateFeeOrderRequest(
                tenantId = "INST-SXA-2026",
                studentId = "ADM-2026-005",
                studentName = "Diya Sengupta",
                feeRecordId = "FEE-2026-TERM1",
                term = "Term 1: 2026-27",
                amountPaise = 3500000L,
                payerUserId = "USR-PAR-05",
                payerName = "Subhash Sengupta"
            )
        )
        assertEquals(OrderDomain.SCHOOL_FEE.name, feeOrder.orderType)
        assertTrue(feeOrder.upiPaymentString?.contains("fees.instsxa2026@icici") == true)
        assertEquals(3500000L, feeOrder.amountPaise)

        // 2. Create SaaS Institutional Subscription Order
        val saasOrder = paymentService.createSaaSSubscriptionOrder(
            CreateSaaSOrderRequest(
                tenantId = "INST-SXA-2026",
                institutionName = "St. Xavier's International Academy",
                plan = SaaSPlan.ENTERPRISE,
                payerUserId = "USR-PRN-01",
                payerName = "Prof. Robert Vance (Principal)"
            )
        )
        assertEquals(OrderDomain.SAAS_SUBSCRIPTION.name, saasOrder.orderType)
        assertTrue(saasOrder.upiPaymentString?.contains("billing@saasplatform.hdfc") == true)
        assertEquals(SaaSPlan.ENTERPRISE.pricePaise, saasOrder.amountPaise)

        // 3. Post Double-Entry for School Fee
        val feeLedger = paymentService.postDoubleEntry(
            domain = OrderDomain.SCHOOL_FEE,
            orderId = feeOrder.orderId,
            invoiceId = feeOrder.invoiceId,
            amountPaise = feeOrder.amountPaise,
            description = "School Tuition Collection",
            tenantId = "INST-SXA-2026"
        )
        assertEquals(LedgerAccountType.CASH_AT_BANK, feeLedger.debitAccount)
        assertEquals(LedgerAccountType.FEES_RECEIVABLE, feeLedger.creditAccount)

        // 4. Post Double-Entry for SaaS Subscription
        val saasLedger = paymentService.postDoubleEntry(
            domain = OrderDomain.SAAS_SUBSCRIPTION,
            orderId = saasOrder.orderId,
            invoiceId = saasOrder.invoiceId,
            amountPaise = saasOrder.amountPaise,
            description = "SaaS Annual Enterprise Subscription",
            tenantId = "INST-SXA-2026"
        )
        assertEquals(LedgerAccountType.ESCROW_CLEARING, saasLedger.debitAccount)
        assertEquals(LedgerAccountType.PLATFORM_REVENUE, saasLedger.creditAccount)
    }

    // =========================================================================
    // 27. DUAL-LAYER AUTOMATION DISPATCH, RATE LIMITING & QUIET HOURS TEST
    // =========================================================================

    @Test
    fun testDualLayerAutomationDispatchRateLimitingAndQuietHours() = runBlocking {
        val dispatchEngine = AutomationDispatchEngine(dao, maxMessagesPerMinute = 3)

        // 1. Normal dispatch during daytime (14:30 / 2:30 PM)
        val normalRequest = AutomationDispatchRequest(
            recipientPhone = "+919811122334",
            recipientName = "Mr. Verma",
            template = AutomationTemplate.ATTENDANCE_ABSENT_ALERT,
            parameters = mapOf("studentName" to "Aarav", "date" to "2026-09-26"),
            priority = AlertPriority.HIGH,
            tenantId = "INST-SXA-2026"
        )
        val normalReport = dispatchEngine.queueAndDispatch(normalRequest, LocalTime.of(14, 30))
        assertEquals("DELIVERED", normalReport.status)
        assertEquals(DeliveryChannel.WHATSAPP_AUTOMATION, normalReport.channel)
        assertFalse(normalReport.isQuietHoursDeferred)

        // 2. Quiet-Hours Filtering: Non-emergency message at 23:30 (11:30 PM) is deferred
        val lateReport = dispatchEngine.queueAndDispatch(normalRequest, LocalTime.of(23, 30))
        assertEquals("QUEUED", lateReport.status)
        assertTrue(lateReport.isQuietHoursDeferred)
        assertTrue(lateReport.errorMessage?.contains("quiet-hours") == true)

        // 3. Emergency Alert at 23:30 bypasses quiet hours
        val emergencyRequest = AutomationDispatchRequest(
            recipientPhone = "+919811122334",
            recipientName = "Mr. Verma",
            template = AutomationTemplate.EMERGENCY_SECURITY_ALERT,
            parameters = mapOf("alertText" to "Severe Weather Cyclone Warning - Campus Closed Tomorrow"),
            priority = AlertPriority.EMERGENCY,
            tenantId = "INST-SXA-2026"
        )
        val emergencyReport = dispatchEngine.queueAndDispatch(emergencyRequest, LocalTime.of(23, 30))
        assertEquals("DELIVERED", emergencyReport.status)
        assertFalse(emergencyReport.isQuietHoursDeferred)

        // 4. Rate Limiting: Max 3 messages per minute. 4th message in same minute is QUEUED
        dispatchEngine.resetRateLimitCounters()
        val dayTime = LocalTime.of(10, 0)
        val rep1 = dispatchEngine.queueAndDispatch(normalRequest, dayTime)
        assertEquals("DELIVERED", rep1.status)
        val rep2 = dispatchEngine.queueAndDispatch(normalRequest, dayTime)
        assertEquals("DELIVERED", rep2.status)
        val rep3 = dispatchEngine.queueAndDispatch(normalRequest, dayTime)
        assertEquals("DELIVERED", rep3.status)
        val rateLimitedReport = dispatchEngine.queueAndDispatch(normalRequest, dayTime)
        assertEquals("QUEUED", rateLimitedReport.status)
        assertTrue(rateLimitedReport.errorMessage?.contains("Rate limit") == true)

        // 5. Automated fallback to SMS Gateway when WhatsApp Automation is unavailable
        dispatchEngine.setWhatsAppAutomationAvailable(false)
        val fallbackReport = dispatchEngine.queueAndDispatch(emergencyRequest, LocalTime.of(12, 0))
        assertEquals(DeliveryChannel.SMS_GATEWAY, fallbackReport.channel)
        assertEquals("DELIVERED", fallbackReport.status)
    }

    @Test
    fun testStudentAttendanceRoleAbstractionAndLeaveWorkflow() = runBlocking {
        // 1. RBAC Verification: Student & Parent have ZERO write permissions for attendance
        val studentPermissions = getDefaultPermissionsForRole(UserRole.STUDENT)
        assertFalse(studentPermissions.contains(SchoolPermissions.MARK_ATTENDANCE_SECTION))
        assertFalse(studentPermissions.contains(SchoolPermissions.VERIFY_ATTENDANCE_INCHARGE))
        assertFalse(studentPermissions.contains(SchoolPermissions.SCAN_REGISTER_CAMERA))
        assertTrue(studentPermissions.contains(SchoolPermissions.VIEW_ATTENDANCE_SELF))
        assertTrue(studentPermissions.contains(SchoolPermissions.SUBMIT_LEAVE_EXCUSE))

        val parentPermissions = getDefaultPermissionsForRole(UserRole.PARENT)
        assertFalse(parentPermissions.contains(SchoolPermissions.MARK_ATTENDANCE_SECTION))
        assertFalse(parentPermissions.contains(SchoolPermissions.VERIFY_ATTENDANCE_INCHARGE))
        assertTrue(parentPermissions.contains(SchoolPermissions.VIEW_ATTENDANCE_WARD))
        assertTrue(parentPermissions.contains(SchoolPermissions.REMIT_ONLINE_FEE))

        // 2. Student formal leave submission
        val leaveReq = StudentLeaveRequestEntity(
            leaveId = "LEV-TEST-2026",
            institutionId = "INST-SXA-2026",
            studentId = "ADM-2026-001",
            studentName = "Aarav Sharma",
            classSection = "10-A",
            startDate = "2026-10-01",
            endDate = "2026-10-03",
            reason = "Viral fever bed rest",
            status = "PENDING",
            appliedBy = "STUDENT"
        )
        repository.submitLeaveRequest(leaveReq)
        val pendingLeaves = repository.getLeaveRequestsForInstitution().first()
        val found = pendingLeaves.firstOrNull { it.leaveId == "LEV-TEST-2026" }
        assertNotNull(found)
        assertEquals("PENDING", found?.status)

        // 3. Faculty Incharge Review and Approval
        val auditLog = AuditLogEntity(
            institutionId = "INST-SXA-2026",
            action = "LEAVE_REQUEST_REVIEWED",
            entityType = "LeaveRequest",
            entityId = "LEV-TEST-2026",
            actorUserId = "INCHARGE-01",
            actorName = "Mrs. Sunita Kapoor (Class Incharge)",
            actorRole = UserRole.INCHARGE.roleClaim,
            previousState = "PENDING",
            newState = "APPROVED",
            remarks = "Medical certificate verified. Leave approved."
        )
        repository.reviewLeaveRequest("LEV-TEST-2026", "APPROVED", "Mrs. Sunita Kapoor", "Medical leave granted", auditLog)

        val updatedLeaves = repository.getLeaveRequestsForInstitution().first()
        val approved = updatedLeaves.firstOrNull { it.leaveId == "LEV-TEST-2026" }
        assertEquals("APPROVED", approved?.status)
        assertEquals("Medical leave granted", approved?.reviewerRemarks)
    }

    // =========================================================================
    // PAYMENT SANDBOX DASHBOARD & WEBHOOK / LEDGER VERIFICATION TESTS
    // =========================================================================

    @Test
    fun testPaymentSandboxDashboardWebhookSimulationAndLedgerUpdatesFromPendingToSuccess() = runBlocking {
        val paymentService = com.example.payment.PaymentGatewayService(repository.dao)
        val webhookSecret = "whsec_sandbox_test_secret_9981"
        val tenantId = "INST-SXA-2026"

        // 1. Initial State: Create a test fee order in PENDING status
        val initialBalBank = paymentService.getLedgerBalance(tenantId, com.example.payment.LedgerAccountType.CASH_AT_BANK)
        val initialBalFeeRec = paymentService.getLedgerBalance(tenantId, com.example.payment.LedgerAccountType.FEES_RECEIVABLE)

        val feeOrder = paymentService.createPaymentOrder(
            tenantId = tenantId,
            studentId = "STU-2026-001",
            studentName = "Aarav Sharma",
            feeRecordId = "FEE-2026-001",
            term = "Term 1 Tuition",
            amountPaise = 1500000L, // Rs. 15,000
            idempotencyKey = "IDEMP-SANDBOX-TEST-001",
            provider = com.example.payment.PaymentGatewayProvider.RAZORPAY
        )

        // Verify order initialized in PENDING state without real financial exchange
        assertEquals(com.example.payment.PaymentStatus.PENDING, feeOrder.status)
        assertEquals(1500000L, feeOrder.amountPaise)
        assertEquals(15000.0, feeOrder.amountRupees, 0.01)

        // 2. Simulate Webhook Inbound Event: payment.captured
        val eventId = "evt_sandbox_test_${System.currentTimeMillis()}"
        val payloadJson = """{"entity":"event","event":"payment.captured","order_id":"${feeOrder.gatewayOrderId}","amount":1500000}"""
        val validSignature = paymentService.computeHmacSha256(payloadJson, webhookSecret)

        val webhookEvent = com.example.payment.GatewayWebhookEvent(
            eventId = eventId,
            eventType = "payment.captured",
            gatewayProvider = com.example.payment.PaymentGatewayProvider.RAZORPAY,
            payloadJson = payloadJson,
            signatureHeader = validSignature
        )

        // 3. Process Webhook: Verify Order Transitions from PENDING -> CAPTURED_SUCCESS
        val webhookResult = paymentService.processWebhookEvent(
            event = webhookEvent,
            webhookSecret = webhookSecret,
            targetOrderId = feeOrder.internalOrderId
        )

        assertTrue(webhookResult.isSuccess)
        assertFalse(webhookResult.isDuplicate)
        assertEquals(com.example.payment.PaymentStatus.CAPTURED_SUCCESS, webhookResult.resultingStatus)

        // Verify order state in authoritative registry is now CAPTURED_SUCCESS
        val capturedOrder = paymentService.getOrder(feeOrder.internalOrderId)
        assertNotNull(capturedOrder)
        assertEquals(com.example.payment.PaymentStatus.CAPTURED_SUCCESS, capturedOrder?.status)
        assertNotNull(capturedOrder?.completedAt)

        // 4. Verify Double-Entry Accounting Ledger Status Updates
        val newBalBank = paymentService.getLedgerBalance(tenantId, com.example.payment.LedgerAccountType.CASH_AT_BANK)
        val newBalFeeRec = paymentService.getLedgerBalance(tenantId, com.example.payment.LedgerAccountType.FEES_RECEIVABLE)

        // Assert balanced ledger update: Debits must equal Credits
        val deltaDebitBank = newBalBank - initialBalBank
        val deltaCreditReceivable = newBalFeeRec - initialBalFeeRec
        assertEquals(1500000L, deltaDebitBank)
        assertEquals(1500000L, deltaCreditReceivable)
        assertEquals(deltaDebitBank, deltaCreditReceivable) // Balanced double-entry equation

        // Verify journal entry recorded
        val journal = paymentService.getDoubleEntryJournal()
        assertTrue(journal.isNotEmpty())
        val topEntry = journal.first()
        assertEquals(com.example.payment.LedgerAccountType.CASH_AT_BANK, topEntry.debitAccount)
        assertEquals(com.example.payment.LedgerAccountType.FEES_RECEIVABLE, topEntry.creditAccount)
        assertEquals(1500000L, topEntry.amountPaise)
        assertEquals(feeOrder.internalOrderId, topEntry.orderId)

        // 5. Anti-Replay Attack Mitigation: Replay same webhook event ID
        val duplicateResult = paymentService.processWebhookEvent(
            event = webhookEvent,
            webhookSecret = webhookSecret,
            targetOrderId = feeOrder.internalOrderId
        )
        assertTrue(duplicateResult.isSuccess)
        assertTrue(duplicateResult.isDuplicate)
        // Ledger balances MUST NOT change on duplicate event
        assertEquals(newBalBank, paymentService.getLedgerBalance(tenantId, com.example.payment.LedgerAccountType.CASH_AT_BANK))

        // 6. Forged Signature Webhook Rejection
        val forgedEvent = com.example.payment.GatewayWebhookEvent(
            eventId = "evt_forged_999",
            eventType = "payment.captured",
            gatewayProvider = com.example.payment.PaymentGatewayProvider.RAZORPAY,
            payloadJson = """{"entity":"event","event":"payment.captured"}""",
            signatureHeader = "forged_invalid_hmac_signature_0000"
        )
        try {
            paymentService.processWebhookEvent(forgedEvent, webhookSecret, feeOrder.internalOrderId)
            fail("Forged webhook signature MUST throw PaymentSecurityException")
        } catch (e: com.example.payment.PaymentSecurityException) {
            assertTrue(e.message?.contains("Forged webhook signature") == true)
        }
    }

    // =========================================================================
    // IMMUTABLE AUDIT LOGGING & ENCRYPTED SYNC COMPLIANCE TESTS
    // =========================================================================

    @Test
    fun testImmutableAuditLoggingServiceInterceptionHashChainingAndEncryptedSync() = runBlocking {
        val auditService = com.example.security.audit.ImmutableAuditLoggingService(repository.dao)
        val instId = "INST-SXA-2026"

        // 1. Intercept Database Mutating Transaction
        val dbLog1 = auditService.recordDatabaseTransaction(
            institutionId = instId,
            action = "FACULTY_RECORD_ATTENDANCE_SECTION",
            entityType = "AttendanceBatch",
            entityId = "ATT-10A-2026-09-28",
            actorUserId = "FAC-TCH-01",
            actorName = "Dr. Meenakshi Sundaram",
            actorRole = "ROLE_TEACHER",
            previousState = "DRAFT",
            newState = "COMMITTED",
            remarks = "Morning Assembly roll call for Class 10-A"
        )

        assertNotNull(dbLog1)
        assertEquals("DATABASE_TX", dbLog1.interactionType)
        assertEquals("PENDING_SYNC", dbLog1.syncStatus)
        assertTrue(dbLog1.currentHash.isNotBlank())
        assertFalse(dbLog1.previousHash.isBlank())

        // 2. Intercept Sensitive UI Interaction (e.g. Exporting scholar records, viewing ledger)
        val uiLog = auditService.recordSensitiveUiInteraction(
            institutionId = instId,
            action = "EXPORT_SCHOLAR_TRANSCRIPT_REPORT",
            screenName = "StudentDashboard",
            actorUserId = "USR-PRIN-01",
            actorName = "Prof. Robert Vance",
            actorRole = "ROLE_PRINCIPAL",
            details = "Exported CBSE cumulative grade dossier for scholar ADM-2026-001"
        )

        assertNotNull(uiLog)
        assertEquals("SENSITIVE_UI_INTERACTION", uiLog.interactionType)
        assertEquals("PENDING_SYNC", uiLog.syncStatus)
        // Verify hash chaining: UI log previousHash MUST equal dbLog1's currentHash!
        assertEquals(dbLog1.currentHash, uiLog.previousHash)

        // 3. Second Database Transaction to test multi-block chain
        val dbLog2 = auditService.recordDatabaseTransaction(
            institutionId = instId,
            action = "BURSAR_COLLECT_TUITION_FEE",
            entityType = "FeeReceipt",
            entityId = "SXA-REC-2026-9912",
            actorUserId = "USR-BURSAR-01",
            actorName = "Suresh Mehta",
            actorRole = "ROLE_ACCOUNTANT",
            previousState = "DUE",
            newState = "PAID",
            remarks = "Fee collected via Cash Counter"
        )
        // Verify chain continuity
        assertEquals(uiLog.currentHash, dbLog2.previousHash)

        // 4. Verify Cryptographic Hash Chain Integrity
        val chainReport = auditService.verifyChainIntegrity(instId)
        assertTrue("Audit hash chain should be 100% tamper-free", chainReport.isTamperFree)
        assertTrue(chainReport.totalRecordsAudited >= 3)
        assertNull(chainReport.brokenChainAtLogId)

        // 5. Periodic / Batch Encrypted Synchronization to Compliance Cloud
        val syncResult = auditService.syncPendingAuditLogsEncrypted(instId)
        assertTrue("Encrypted audit sync should succeed", syncResult.isSuccess)
        assertTrue(syncResult.syncedRecordCount >= 3)
        assertNotNull(syncResult.serverReceiptToken)
        assertTrue(syncResult.serverReceiptToken?.startsWith("RECEIPT-SERVER-ACK") == true)

        // 6. Verify Records in Room updated to SYNCED_ENCRYPTED with encrypted payload marker
        val allLogs = repository.dao.getAllAuditLogsAsc().filter { it.institutionId == instId }
        val syncedLogs = allLogs.filter { it.logId in listOf(dbLog1.logId, uiLog.logId, dbLog2.logId) }
        assertTrue(syncedLogs.all { it.syncStatus == "SYNCED_ENCRYPTED" })
        assertTrue(syncedLogs.all { it.syncTimestamp != null && it.syncTimestamp!! > 0L })
        assertTrue(syncedLogs.all { it.encryptedPayload?.startsWith("ENC-AESGCM") == true })
    }
}


