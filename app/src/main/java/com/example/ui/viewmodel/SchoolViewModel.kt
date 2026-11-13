package com.example.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SchoolPreferencesManager
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AdmissionApplicationEntity
import com.example.data.local.entities.AiAuditLogEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CertificateEntity
import com.example.data.local.entities.CircularEntity
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.FeeStructureEntity
import com.example.data.local.entities.GovernmentComplianceEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.HomeworkSubmissionEntity
import com.example.data.local.entities.IdCardRecordEntity
import com.example.data.local.entities.InchargeEditRequestEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.InstitutionalBillEntity
import com.example.data.local.entities.NoticeMeetingEntity
import com.example.data.local.entities.ParentWardLinkEntity
import com.example.data.local.entities.PushNotificationEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.local.entities.SchoolDiaryEntity
import com.example.data.local.entities.SchoolInstructionEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentLeaveRequestEntity
import com.example.data.local.entities.SubjectEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.TimetableSlotEntity
import com.example.data.local.entities.UserEntity
import com.example.security.SecurityUtils
import com.example.security.TenantSecurityContext
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.model.getDefaultPermissionsForRole
import com.example.data.remote.sync.CloudSyncManager
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.repository.AiAcademicAssistant
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.CreateFeeAssessmentRequest
import com.example.data.repository.FeeBalanceSummary
import com.example.data.repository.FeePaymentReceiptResult
import com.example.data.repository.FeePaymentValidator
import com.example.data.repository.ManualFeePaymentRequest
import com.example.data.repository.SchoolRepository
import com.example.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SchoolViewModel @Inject constructor(
    val repository: SchoolRepository,
    val userRepository: UserRepository,
    val attendanceRepository: AttendanceRepository,
    val authRepository: AuthRepository,
    val preferencesManager: SchoolPreferencesManager? = null,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    constructor(
        repository: SchoolRepository,
        userRepository: UserRepository = repository.userRepository,
        attendanceRepository: AttendanceRepository = repository.attendanceRepository,
        context: Context? = null
    ) : this(
        repository = repository,
        userRepository = userRepository,
        attendanceRepository = attendanceRepository,
        authRepository = AuthRepositoryImpl(repository.dao),
        preferencesManager = context?.let { SchoolPreferencesManager(it) },
        context = context
    )

    val prefsManager: SchoolPreferencesManager by lazy {
        preferencesManager
            ?: context?.let { SchoolPreferencesManager(it) }
            ?: SchoolPreferencesManager(context ?: throw IllegalStateException("Context required for preferences"))
    }

    private val prefs: SharedPreferences? by lazy {
        context?.getSharedPreferences("sxa_school_prefs", Context.MODE_PRIVATE)
    }

    // =========================================================================
    // MULTI-STAGE AUTHENTICATION & INSTITUTIONAL ACCESS CONTROL
    // =========================================================================
    enum class AuthStage {
        REGISTRATION_LOOKUP,
        CREDENTIALS_LOGIN,
        AUTHENTICATED
    }

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _authStage = MutableStateFlow(AuthStage.REGISTRATION_LOOKUP)
    val authStage: StateFlow<AuthStage> = _authStage.asStateFlow()

    private val _verifiedInstitution = MutableStateFlow<InstitutionEntity?>(null)
    val verifiedInstitution: StateFlow<InstitutionEntity?> = _verifiedInstitution.asStateFlow()

    private val _selectedAcademicBatch = MutableStateFlow("2026-27")
    val selectedAcademicBatch: StateFlow<String> = _selectedAcademicBatch.asStateFlow()

    private val _currentAuthSession = MutableStateFlow<com.example.data.model.AuthSession?>(null)
    val currentAuthSession: StateFlow<com.example.data.model.AuthSession?> = _currentAuthSession.asStateFlow()

    val allInstitutions: StateFlow<List<InstitutionEntity>> = repository.allInstitutions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Current Active Role & Session Claims
    private val _currentRole = MutableStateFlow(UserRole.STUDENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _userClaims = MutableStateFlow<SecurityClaims?>(null)
    val userClaims: StateFlow<SecurityClaims?> = _userClaims.asStateFlow()

    // Persistent Security Verification Flags
    private val _isPrincipalAuthenticated = MutableStateFlow(
        prefs?.getBoolean("auth_principal", false) ?: false
    )
    val isPrincipalAuthenticated: StateFlow<Boolean> = _isPrincipalAuthenticated.asStateFlow()

    private val _isTeacherAuthenticated = MutableStateFlow(
        prefs?.getBoolean("auth_teacher", false) ?: false
    )
    val isTeacherAuthenticated: StateFlow<Boolean> = _isTeacherAuthenticated.asStateFlow()

    private val _isInchargeAuthenticated = MutableStateFlow(
        prefs?.getBoolean("auth_incharge", false) ?: false
    )
    val isInchargeAuthenticated: StateFlow<Boolean> = _isInchargeAuthenticated.asStateFlow()

    private val _isStudentAuthenticated = MutableStateFlow(
        prefs?.getBoolean("auth_student", false) ?: false
    )
    val isStudentAuthenticated: StateFlow<Boolean> = _isStudentAuthenticated.asStateFlow()

    // Active User Contexts
    private val _authenticatedTeacher = MutableStateFlow<StaffEntity?>(null)
    val authenticatedTeacher: StateFlow<StaffEntity?> = _authenticatedTeacher.asStateFlow()

    private val _authenticatedIncharge = MutableStateFlow<StaffEntity?>(null)
    val authenticatedIncharge: StateFlow<StaffEntity?> = _authenticatedIncharge.asStateFlow()

    private val _authenticatedStudent = MutableStateFlow<StudentEntity?>(null)
    val authenticatedStudent: StateFlow<StudentEntity?> = _authenticatedStudent.asStateFlow()

    private val _userProfileName = MutableStateFlow("Prof. Robert Vance, Ph.D. (Principal)")
    val userProfileName: StateFlow<String> = _userProfileName.asStateFlow()

    // Offline & Synchronization State
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow("Just now")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    // Reactive StateFlows from Room Database
    val institution: StateFlow<InstitutionEntity?> = repository.institution
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allClasses: StateFlow<List<AcademicClassEntity>> = repository.allClasses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffList: StateFlow<List<StaffEntity>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val students: StateFlow<List<StudentEntity>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendanceRecords: StateFlow<List<AttendanceRecordEntity>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registerBatches: StateFlow<List<RegisterScanBatchEntity>> = repository.allRegisterBatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val examRecords: StateFlow<List<ExamRecordEntity>> = repository.allExamRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feeRecords: StateFlow<List<FeeRecordEntity>> = repository.allFeeRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val certificates: StateFlow<List<CertificateEntity>> = repository.allCertificates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val idCardRecords: StateFlow<List<IdCardRecordEntity>> = repository.allIdCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val admissionApplications: StateFlow<List<AdmissionApplicationEntity>> = repository.allAdmissionApplications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableSlots: StateFlow<List<TimetableSlotEntity>> = repository.allTimetableSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notices: StateFlow<List<NoticeMeetingEntity>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<SchoolDiaryEntity>> = repository.allDiaryEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val diaryList: StateFlow<List<SchoolDiaryEntity>> = diaryEntries

    val instructions: StateFlow<List<SchoolInstructionEntity>> = repository.allInstructions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val institutionalBills: StateFlow<List<InstitutionalBillEntity>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val complianceDocuments: StateFlow<List<GovernmentComplianceEntity>> = repository.allComplianceDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inchargeRequests: StateFlow<List<InchargeEditRequestEntity>> = repository.allInchargeRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val homeworkList: StateFlow<List<HomeworkEntity>> = repository.getHomeworkForInstitution()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val homeworkSubmissions: StateFlow<List<HomeworkSubmissionEntity>> = repository.getSubmissionsForStudent("ADM-2026-001")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val leaveRequests: StateFlow<List<StudentLeaveRequestEntity>> = repository.getLeaveRequestsForInstitution()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val feeStructures: StateFlow<List<FeeStructureEntity>> = repository.getFeeStructures()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val paymentTransactions: StateFlow<List<FeePaymentTransactionEntity>> = repository.getAllPaymentTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // =========================================================================
    // PHASE D: FEE RECEIPTS & FINANCIAL LEDGER STATE FLOW
    // =========================================================================
    private val _feeStudentQuery = MutableStateFlow("")
    private val _selectedFeeStudentId = MutableStateFlow<String?>(null)
    private val _feeOperationUiFlags = MutableStateFlow(FeeWorkflowUiState())
    private val _hasLoadedFeeStream = MutableStateFlow(false)

    val feeWorkflowState: StateFlow<FeeWorkflowUiState> = combine(
        _currentAuthSession,
        _userClaims,
        _verifiedInstitution,
        students,
        feeRecords,
        paymentTransactions,
        _feeStudentQuery,
        _selectedFeeStudentId,
        _isOnline,
        _feeOperationUiFlags,
        _hasLoadedFeeStream
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val session = values[0] as com.example.data.model.AuthSession?
        val claims = values[1] as SecurityClaims?
        val inst = values[2] as InstitutionEntity?
        @Suppress("UNCHECKED_CAST")
        val allStudentsList = values[3] as List<StudentEntity>
        @Suppress("UNCHECKED_CAST")
        val allFeesList = values[4] as List<FeeRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val allTxnsList = values[5] as List<FeePaymentTransactionEntity>
        val query = values[6] as String
        val selectedStudentId = values[7] as String?
        val online = values[8] as Boolean
        val flags = values[9] as FeeWorkflowUiState
        val hasLoaded = values[10] as Boolean

        val activeTenant = session?.institution?.institutionId
            ?: com.example.security.TenantSecurityContext.getActiveTenantIdOrNull()
            ?: inst?.institutionId
            ?: ""
        val role = claims?.role ?: session?.role
        val canManageFees = role in FeePaymentValidator.AUTHORIZED_FINANCIAL_ROLES
        val isSelfOrParent = role == UserRole.STUDENT || role == UserRole.PARENT
        val isRestricted = role == null || (!canManageFees && !isSelfOrParent)

        val tenantStudents = allStudentsList.filter { activeTenant.isBlank() || it.institutionId == activeTenant }
        val filteredStudents = if (query.isBlank()) {
            tenantStudents
        } else {
            val q = query.trim().lowercase(java.util.Locale.US)
            tenantStudents.filter {
                it.name.lowercase(java.util.Locale.US).contains(q) ||
                    it.admissionNumber.lowercase(java.util.Locale.US).contains(q) ||
                    "${it.grade}-${it.section}".lowercase(java.util.Locale.US).contains(q)
            }
        }
        val selectedStudent = selectedStudentId?.let { id ->
            tenantStudents.firstOrNull { it.id == id || it.admissionNumber == id }
        }

        val scopedStudentIds: Set<String>? = when {
            canManageFees -> selectedStudent?.let { setOf(it.id, it.admissionNumber) }
            role == UserRole.STUDENT -> {
                val stuId = claims?.userId ?: session?.user?.id ?: ""
                setOf(stuId)
            }
            role == UserRole.PARENT -> setOf(_selectedWardId.value)
            else -> emptySet()
        }

        val scopedFees = if (isRestricted) {
            emptyList()
        } else {
            allFeesList.filter { fee ->
                (activeTenant.isBlank() || fee.institutionId == activeTenant) &&
                    (scopedStudentIds == null || fee.studentId in scopedStudentIds || fee.admissionNumber in scopedStudentIds)
            }
        }

        val scopedFeeIds = scopedFees.map { it.id }.toSet()
        val scopedTxns = if (isRestricted) {
            emptyList()
        } else {
            allTxnsList.filter { txn ->
                (activeTenant.isBlank() || txn.institutionId == activeTenant) &&
                    (scopedStudentIds == null || txn.studentId in scopedStudentIds || txn.feeRecordId in scopedFeeIds)
            }.sortedByDescending { it.timestamp }
        }

        val summaries = scopedFees.map { record ->
            val recordTxns = scopedTxns.filter {
                it.feeRecordId == record.id && it.status !in setOf("REFUNDED", "VOID", "FAILED")
            }
            val confirmedPaid = recordTxns.sumOf { it.amountPaidPaise }
            val outstanding = (record.totalAmount - confirmedPaid).coerceAtLeast(0L)
            val status = when {
                outstanding <= 0L && confirmedPaid >= record.totalAmount -> "PAID"
                confirmedPaid > 0L -> "PARTIAL"
                else -> "DUE"
            }
            FeeBalanceSummary(
                feeRecord = record.copy(paidAmount = confirmedPaid, dueAmount = outstanding, status = status),
                totalAmountPaise = record.totalAmount,
                confirmedPaidPaise = confirmedPaid,
                outstandingBalancePaise = outstanding,
                confirmedTransactionCount = recordTxns.size,
                transactions = recordTxns
            )
        }

        val loadState = when {
            isRestricted -> FeeLedgerLoadState.Restricted
            !hasLoaded && summaries.isEmpty() -> FeeLedgerLoadState.Loading
            summaries.isEmpty() -> FeeLedgerLoadState.Empty
            else -> FeeLedgerLoadState.Ready(summaries)
        }

        val totalCollected = summaries.sumOf { it.confirmedPaidPaise }
        val totalDue = summaries.sumOf { it.outstandingBalancePaise }
        val pendingTxns = scopedTxns.count { !it.isSyncedToCentral }

        flags.copy(
            loadState = loadState,
            activeTenantId = activeTenant,
            actorRoleName = role?.name ?: "GUEST",
            actorDisplayName = claims?.displayName ?: session?.user?.displayName ?: "",
            isAuthorizedToManageFees = canManageFees,
            isReadOnlySelfScope = isSelfOrParent,
            studentSearchQuery = query,
            matchedStudents = if (canManageFees) filteredStudents else emptyList(),
            selectedStudent = selectedStudent,
            feeSummaries = summaries,
            transactions = scopedTxns,
            totalCollectedPaise = totalCollected,
            totalOutstandingPaise = totalDue,
            settledAccountCount = summaries.count { it.outstandingBalancePaise == 0L && it.totalAmountPaise > 0L },
            dueAccountCount = summaries.count { it.outstandingBalancePaise > 0L },
            pendingSyncTransactionCount = pendingTxns,
            isOnline = online
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FeeWorkflowUiState(loadState = FeeLedgerLoadState.Loading)
    )

    val notifications: StateFlow<List<PushNotificationEntity>> = repository.getNotificationsForUser("INST-SXA-2026", "ALL")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationCount: StateFlow<Int> = repository.getUnreadNotificationCount("INST-SXA-2026", "ALL")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val subjects: StateFlow<List<SubjectEntity>> = repository.getSubjectsForInstitution()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val parentWardLinks: StateFlow<List<ParentWardLinkEntity>> = repository.getWardsForParent("USR-PAR-01")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedWardId = MutableStateFlow("ADM-2026-001")
    val selectedWardId: StateFlow<String> = _selectedWardId.asStateFlow()

    val isTeacherOneTimeSetupDone: StateFlow<Boolean> = isTeacherAuthenticated
    val isInchargeOneTimeSetupDone: StateFlow<Boolean> = isInchargeAuthenticated
    val isStudentOneTimeSetupDone: StateFlow<Boolean> = isStudentAuthenticated

    // Enterprise Immutable Audit Logging & Encrypted Compliance Service
    val immutableAuditService = com.example.security.audit.ImmutableAuditLoggingService(repository.dao)
    val auditChainReport: StateFlow<com.example.security.audit.AuditChainVerificationReport?> = immutableAuditService.lastVerificationReport
    val isAuditSyncing: StateFlow<Boolean> = immutableAuditService.isSyncing
    val lastAuditSyncResult: StateFlow<com.example.security.audit.EncryptedAuditSyncResult?> = immutableAuditService.lastSyncResult
    val pendingAuditSyncCount: StateFlow<Int> = immutableAuditService.pendingSyncCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val cloudSyncManager: CloudSyncManager by lazy {
        CloudSyncManager(repository.dao, repository.outboxManager)
    }

    val cloudSyncMessage: StateFlow<String> by lazy { cloudSyncManager.syncMessage }

    init {
        // Observe live hardware connectivity when running with Android Context
        if (context != null) {
            val connectivityMonitor = com.example.data.remote.sync.NetworkConnectivityMonitor(context)
            viewModelScope.launch {
                connectivityMonitor.isConnected.collect { connected ->
                    _isOnline.value = connected
                    cloudSyncManager.setNetworkStatus(connected)
                }
            }
        }

        // Start background periodic encrypted synchronization to compliance cloud
        immutableAuditService.startPeriodicSync(viewModelScope, intervalMillis = 60_000L)

        viewModelScope.launch {
            // Guarantee demo users and institution data are seeded in Room
            runCatching { repository.ensureDemoUsersAndDataSeeded() }
            _hasLoadedFeeStream.value = true

            val isVerified = runCatching { prefsManager.isSchoolVerifiedOnce() }.getOrDefault(false)
            val isSetupCompleted = runCatching { prefsManager.isSetupCompletedOnce() }.getOrDefault(false)

            // Load verified default institution
            val defaultInst = repository.getInstitution()
            if (defaultInst != null) {
                _verifiedInstitution.value = defaultInst
                val batches = defaultInst.availableBatches.split(",").map { it.trim() }
                if (batches.isNotEmpty()) {
                    _selectedAcademicBatch.value = batches.first()
                }
            }

            // Authoritative session restoration via Firebase Authentication
            val restored = authRepository.restoreSessionIfValid()
            restored.onSuccess { session ->
                val authSession = com.example.data.model.AuthSession(
                    token = session.sessionToken,
                    user = UserEntity(
                        id = session.userId,
                        institutionId = session.institutionId,
                        username = session.email.substringBefore("@"),
                        email = session.email,
                        displayName = session.displayName,
                        role = session.role.name,
                        password = "",
                        passwordSalt = "",
                        passwordHash = ""
                    ),
                    role = session.role,
                    institution = _verifiedInstitution.value ?: defaultInst ?: InstitutionEntity(
                        institutionId = session.institutionId,
                        registrationCode = "3477",
                        name = "St. Xavier's International Academy"
                    ),
                    academicBatch = _selectedAcademicBatch.value
                )
                _currentAuthSession.value = authSession
                _currentRole.value = session.role
                _userProfileName.value = session.displayName
                _userClaims.value = session.claims
                _isAuthenticated.value = true
                _authStage.value = AuthStage.AUTHENTICATED
                TenantSecurityContext.setContext(session.claims, session.institutionId)
            }.onFailure {
                // Fail closed: No valid active session. Require explicit user login.
                _isAuthenticated.value = false
                _userClaims.value = null
                _currentAuthSession.value = null
                _currentRole.value = UserRole.STUDENT

                // If school was previously verified on this device, proceed directly to credentials login.
                // Otherwise prompt for school registration lookup.
                if (isSetupCompleted || isVerified || defaultInst != null) {
                    _authStage.value = AuthStage.CREDENTIALS_LOGIN
                } else {
                    _authStage.value = AuthStage.REGISTRATION_LOOKUP
                }
            }

            val savedTeacherPhone = prefs?.getString("saved_teacher_phone", null)
            if (!savedTeacherPhone.isNullOrBlank()) {
                val staff = repository.getStaffByPhone(savedTeacherPhone)
                if (staff != null) _authenticatedTeacher.value = staff
            }
            val savedInchargePhone = prefs?.getString("saved_incharge_phone", null)
            if (!savedInchargePhone.isNullOrBlank()) {
                val staff = repository.getStaffByPhone(savedInchargePhone)
                if (staff != null) _authenticatedIncharge.value = staff
            }
            val savedStudentAdmission = prefs?.getString("saved_student_admission", null)
            if (!savedStudentAdmission.isNullOrBlank()) {
                val stu = repository.getStudentByAdmissionNumber(savedStudentAdmission)
                if (stu != null) _authenticatedStudent.value = stu
            }

            if (_isAuthenticated.value) {
                updateProfileNameForRole(_currentRole.value)
            }
        }
    }

    // =========================================================================
    // MULTI-STAGE AUTHENTICATION CONTROLLERS
    // =========================================================================

    fun lookupRegistrationCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanCode = code.trim()
            if (cleanCode.isBlank()) {
                onResult(false, "Please enter a valid Registration Number or School Code.")
                return@launch
            }

            val inst = repository.getInstitutionByCode(cleanCode)
            if (inst != null) {
                _verifiedInstitution.value = inst
                val batches = inst.availableBatches.split(",").map { it.trim() }
                if (batches.isNotEmpty()) {
                    _selectedAcademicBatch.value = batches.first()
                }
                // Persist school verification strictly once in DataStore
                runCatching {
                    prefsManager.setSchoolVerified(true, cleanCode, inst.institutionId)
                    prefsManager.setSetupCompleted(true)
                }
                _authStage.value = AuthStage.CREDENTIALS_LOGIN
                onResult(true, "Verified: ${inst.name}")
            } else {
                onResult(false, "Registration code '$cleanCode' not found. Please contact school administrator.")
            }
        }
    }

    fun selectInstitution(institution: InstitutionEntity) {
        _verifiedInstitution.value = institution
        val batches = institution.availableBatches.split(",").map { it.trim() }
        if (batches.isNotEmpty()) {
            _selectedAcademicBatch.value = batches.first()
        }
        _authStage.value = AuthStage.CREDENTIALS_LOGIN
    }

    fun selectAcademicBatch(batch: String) {
        _selectedAcademicBatch.value = batch
    }

    fun returnToRegistrationLookup() {
        _authStage.value = AuthStage.REGISTRATION_LOOKUP
    }

    fun loginWithCredentials(
        username: String,
        password: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val inst = _verifiedInstitution.value ?: repository.getInstitution()
            if (inst == null) {
                onResult(false, "Please select or verify your institution first.")
                return@launch
            }

            val batch = _selectedAcademicBatch.value
            val result = authRepository.signInWithEmailPassword(username, password, inst.institutionId)
            result.onSuccess { session ->
                val authSession = com.example.data.model.AuthSession(
                    token = session.sessionToken,
                    user = UserEntity(
                        id = session.userId,
                        institutionId = session.institutionId,
                        username = session.email.substringBefore("@"),
                        email = session.email,
                        displayName = session.displayName,
                        role = session.role.name,
                        password = "",
                        passwordSalt = "",
                        passwordHash = ""
                    ),
                    role = session.role,
                    institution = inst,
                    academicBatch = batch.ifBlank { inst.currentAcademicYear }
                )

                _currentAuthSession.value = authSession
                _currentRole.value = session.role
                _userProfileName.value = session.displayName
                _userClaims.value = session.claims
                _isAuthenticated.value = true
                _authStage.value = AuthStage.AUTHENTICATED
                TenantSecurityContext.setContext(session.claims, inst.institutionId)

                when (session.role) {
                    UserRole.ADMIN, UserRole.PRINCIPAL -> _isPrincipalAuthenticated.value = true
                    UserRole.INCHARGE -> {
                        _isInchargeAuthenticated.value = true
                        val staff = repository.getStaffByPhone(session.claims.phone)
                        if (staff != null) _authenticatedIncharge.value = staff
                    }
                    UserRole.TEACHER, UserRole.TEACHING -> {
                        _isTeacherAuthenticated.value = true
                        val staff = repository.getStaffByPhone(session.claims.phone)
                        if (staff != null) _authenticatedTeacher.value = staff
                    }
                    UserRole.STUDENT -> {
                        _isStudentAuthenticated.value = true
                    }
                    else -> {}
                }

                // Persist session to DataStore
                runCatching {
                    prefsManager.saveAuthenticatedSession(
                        userId = session.userId,
                        role = session.role.name,
                        displayName = session.displayName,
                        token = session.sessionToken
                    )
                }

                onResult(true, "Authentication successful. Access granted as ${session.role.label}.")
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Firebase Authentication failed.")
            }
        }
    }

    fun registerUser(
        email: String,
        password: String,
        displayName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val inst = _verifiedInstitution.value ?: repository.getInstitution()
            val tenantId = inst?.institutionId ?: "INST-DEFAULT"

            val result = authRepository.registerUser(
                email = email.trim(),
                password = password.trim(),
                displayName = displayName.trim(),
                role = UserRole.STUDENT,
                institutionId = tenantId
            )
            result.onSuccess { session ->
                val authSession = com.example.data.model.AuthSession(
                    token = session.sessionToken,
                    user = UserEntity(
                        id = session.userId,
                        institutionId = session.institutionId,
                        username = session.email.substringBefore("@"),
                        email = session.email,
                        displayName = session.displayName,
                        role = session.role.name,
                        password = "",
                        passwordSalt = "",
                        passwordHash = ""
                    ),
                    role = session.role,
                    institution = inst ?: InstitutionEntity(
                        institutionId = session.institutionId,
                        registrationCode = "3477",
                        name = "School Academy"
                    ),
                    academicBatch = _selectedAcademicBatch.value
                )

                _currentAuthSession.value = authSession
                _currentRole.value = session.role
                _userProfileName.value = session.displayName
                _userClaims.value = session.claims
                _isAuthenticated.value = true
                _authStage.value = AuthStage.AUTHENTICATED
                _isStudentAuthenticated.value = true
                TenantSecurityContext.setContext(session.claims, session.institutionId)

                onResult(true, "Account created successfully for ${session.displayName} with safe role STUDENT.")
            }.onFailure { error ->
                onResult(false, error.localizedMessage ?: "Registration failed.")
            }
        }
    }

    fun resetUserPassword(
        usernameOrPhone: String,
        newPass: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(usernameOrPhone)
            result.onSuccess {
                onResult(true, "Password reset link sent to $usernameOrPhone")
            }.onFailure { err ->
                onResult(false, err.localizedMessage ?: "Password reset failed.")
            }
        }
    }

    fun logout() {
        val user = _currentAuthSession.value?.user
        val inst = _verifiedInstitution.value
        val tokenSnapshot = _currentAuthSession.value?.token ?: "N/A"
        val profileSnapshot = _userProfileName.value
        val actorClaimsSnapshot = _userClaims.value
        val actorRoleSnapshot = _currentRole.value

        // Fail closed immediately: clear in-memory and session security state synchronously
        com.example.security.SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        _currentAuthSession.value = null
        _userClaims.value = null
        _currentRole.value = UserRole.STUDENT
        _userProfileName.value = ""
        _isAuthenticated.value = false
        _isPrincipalAuthenticated.value = false
        _isTeacherAuthenticated.value = false
        _isInchargeAuthenticated.value = false
        _isStudentAuthenticated.value = false
        _authenticatedTeacher.value = null
        _authenticatedIncharge.value = null
        _authenticatedStudent.value = null
        _selectedFeeStudentId.value = null
        _feeStudentQuery.value = ""
        _feeOperationUiFlags.value = FeeWorkflowUiState()
        _selectedWardId.value = "ADM-2026-001"
        _authStage.value = AuthStage.CREDENTIALS_LOGIN
        prefs?.edit()?.clear()?.apply()

        viewModelScope.launch {
            runCatching {
                immutableAuditService.recordDatabaseTransaction(
                    institutionId = inst?.institutionId ?: "INST-SXA-2026",
                    action = "USER_LOGOUT_TERMINATED",
                    entityType = "SECURITY_SESSION",
                    entityId = tokenSnapshot,
                    actorUserId = actorClaimsSnapshot?.userId ?: "SYSTEM_AUTH",
                    actorName = actorClaimsSnapshot?.displayName ?: profileSnapshot,
                    actorRole = actorClaimsSnapshot?.roleClaim ?: actorRoleSnapshot.roleClaim,
                    previousState = "AUTHENTICATED",
                    newState = "TERMINATED",
                    remarks = "User '${user?.username ?: profileSnapshot}' logged out of ${inst?.name ?: "Portal"}"
                )
            }
            runCatching { authRepository.signOut() }
            runCatching { prefsManager.clearSessionOnSignOut() }
        }
    }

    fun resetAllOnboardingAndSignOut() {
        viewModelScope.launch {
            runCatching { prefsManager.resetAllOnboarding() }
            logout()
            _authStage.value = AuthStage.REGISTRATION_LOOKUP
        }
    }

    private fun saveSessionToPrefs(claims: SecurityClaims) {
        prefs?.edit()
            ?.putString("auth_session_user_id", claims.userId)
            ?.putString("auth_session_role", claims.role.name)
            ?.putString("auth_session_name", claims.displayName)
            ?.putString("auth_session_phone", claims.phone)
            ?.putString("auth_session_email", claims.email)
            ?.putString("auth_session_desig", claims.designationOrGrade)
            ?.putString("auth_session_section", claims.assignedClassSection ?: "")
            ?.putString("auth_session_token", claims.sessionToken)
            ?.apply()
    }

    suspend fun logAuditEvent(
        action: String,
        entityType: String,
        entityId: String,
        previousState: String?,
        newState: String,
        remarks: String
    ) {
        val claims = _userClaims.value
        val actorId = claims?.userId ?: "SYSTEM_AUTH"
        val actorName = claims?.displayName ?: _userProfileName.value
        val actorRole = claims?.roleClaim ?: _currentRole.value.roleClaim
        val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"

        immutableAuditService.recordDatabaseTransaction(
            institutionId = instId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            actorUserId = actorId,
            actorName = actorName,
            actorRole = actorRole,
            previousState = previousState,
            newState = newState,
            remarks = remarks
        )
    }

    fun recordSensitiveUiInteraction(
        action: String,
        screenName: String,
        details: String
    ) {
        viewModelScope.launch {
            val claims = _userClaims.value
            val actorId = claims?.userId ?: "SYSTEM_AUTH"
            val actorName = claims?.displayName ?: _userProfileName.value
            val actorRole = claims?.roleClaim ?: _currentRole.value.roleClaim
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"

            immutableAuditService.recordSensitiveUiInteraction(
                institutionId = instId,
                action = action,
                screenName = screenName,
                actorUserId = actorId,
                actorName = actorName,
                actorRole = actorRole,
                details = details
            )
        }
    }

    fun verifyAuditChainIntegrity() {
        viewModelScope.launch {
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            immutableAuditService.verifyChainIntegrity(instId)
        }
    }

    fun triggerEncryptedAuditSync(onComplete: ((com.example.security.audit.EncryptedAuditSyncResult) -> Unit)? = null) {
        viewModelScope.launch {
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val res = immutableAuditService.syncPendingAuditLogsEncrypted(instId)
            onComplete?.invoke(res)
        }
    }

    // =========================================================================
    // AUTHENTICATION & LOGIN GATEWAY
    // =========================================================================

    suspend fun loginStudentOrParent(
        admissionNumber: String,
        phone: String,
        otp: String,
        isParentLogin: Boolean
    ): Pair<Boolean, String> {
        val student = repository.getStudentByAdmissionAndPhone(admissionNumber, phone)
            ?: return Pair(false, "Admission Number ($admissionNumber) & Guardian Phone do not match enrolled records.")

        if (otp.trim() != "123456" && otp.trim().length != 6) {
            return Pair(false, "Invalid OTP code. Please enter the 6-digit verification code.")
        }

        val role = if (isParentLogin) UserRole.PARENT else UserRole.STUDENT
        val claims = SecurityClaims(
            userId = if (isParentLogin) "PAR-${student.id}" else student.id,
            role = role,
            roleClaim = role.roleClaim,
            displayName = if (isParentLogin) "${student.guardianName} (Parent of ${student.name})" else "${student.name} (Scholar)",
            phone = phone,
            email = student.email,
            designationOrGrade = "Grade ${student.grade}-${student.section}",
            assignedClassSection = "${student.grade}-${student.section}",
            permissions = getDefaultPermissionsForRole(role),
            sessionToken = "SEC-TOKEN-${System.currentTimeMillis()}-${(1000..9999).random()}"
        )

        _userClaims.value = claims
        _currentRole.value = role
        if (!isParentLogin) {
            _authenticatedStudent.value = student
            _isStudentAuthenticated.value = true
        }
        updateProfileNameForRole(role)
        saveSessionToPrefs(claims)

        logAuditEvent(
            action = "USER_LOGIN_GATEWAY",
            entityType = "SECURITY_SESSION",
            entityId = claims.sessionToken,
            previousState = "ANONYMOUS",
            newState = "AUTHENTICATED",
            remarks = "2-Step authentication successful for $role: ${claims.displayName}"
        )

        return Pair(true, "Authentication successful. Access granted to ${role.clearanceTitle}.")
    }

    suspend fun loginStaff(
        employeeId: String,
        phone: String,
        otp: String,
        principalPin: String?
    ): Pair<Boolean, String> {
        val cleanPhone = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        val staff = repository.getStaffById(employeeId.trim())
            ?: repository.getStaffByPhone(cleanPhone)
            ?: return Pair(false, "Employee ID ($employeeId) or Mobile Number is not registered in Academy Staff Registry.")

        if (otp.trim() != "123456" && otp.trim().length != 6) {
            return Pair(false, "Invalid OTP. Please enter the 6-digit staff verification code.")
        }

        val role: UserRole = when {
            staff.roleType == "PRINCIPAL" -> {
                if (principalPin == null || (principalPin.trim() != "admin123" && principalPin.trim() != "9876" && principalPin.trim() != staff.authPin)) {
                    return Pair(false, "Principal Security Verification Failed: Invalid Master PIN.")
                }
                _isPrincipalAuthenticated.value = true
                UserRole.PRINCIPAL
            }
            staff.isClassIncharge -> {
                _authenticatedIncharge.value = staff
                _isInchargeAuthenticated.value = true
                UserRole.INCHARGE
            }
            staff.roleType == "TEACHING" -> {
                _authenticatedTeacher.value = staff
                _isTeacherAuthenticated.value = true
                UserRole.TEACHING
            }
            staff.roleType == "ACCOUNTANT" -> UserRole.ACCOUNTANT
            staff.roleType == "OFFICE" -> UserRole.OFFICE
            staff.roleType == "STAFF_MANAGER" -> UserRole.STAFF_MANAGER
            else -> UserRole.NON_TEACHING
        }

        repository.updateStaffOtpVerified(staff.id, true)

        val claims = SecurityClaims(
            userId = staff.id,
            role = role,
            roleClaim = role.roleClaim,
            displayName = "${staff.name} (${staff.designation})",
            phone = staff.phone,
            email = staff.email,
            designationOrGrade = staff.designation,
            assignedClassSection = staff.inchargeClass ?: "10-A",
            permissions = getDefaultPermissionsForRole(role),
            sessionToken = "SEC-TOKEN-${System.currentTimeMillis()}-${(1000..9999).random()}"
        )

        _userClaims.value = claims
        _currentRole.value = role
        updateProfileNameForRole(role)
        saveSessionToPrefs(claims)

        logAuditEvent(
            action = "STAFF_LOGIN_GATEWAY",
            entityType = "SECURITY_SESSION",
            entityId = claims.sessionToken,
            previousState = "ANONYMOUS",
            newState = "AUTHENTICATED",
            remarks = "2-Step verification successful for ${staff.name} as ${role.roleClaim}"
        )

        return Pair(true, "Staff verified. Access granted to ${role.clearanceTitle}.")
    }

    // Role switching for Developer / Sandbox Testing Mode (Authorized for Admins only)
    fun switchRole(role: UserRole) {
        val authoritativeRole = _userClaims.value?.role ?: _currentAuthSession.value?.role ?: UserRole.STUDENT
        // Privilege escalation guard: Only verified administrators or principals may switch role views
        if (authoritativeRole != UserRole.ADMIN && authoritativeRole != UserRole.PRINCIPAL) {
            return
        }
        _currentRole.value = role
        prefs?.edit()?.putString("sxa_current_role", role.name)?.apply()
        updateProfileNameForRole(role)
        val dummyClaims = SecurityClaims(
            userId = when (role) {
                UserRole.ADMIN -> "USR-ADM-01"
                UserRole.PRINCIPAL -> "FAC-PRIN-01"
                UserRole.INCHARGE -> "FAC-TCH-02"
                UserRole.TEACHER, UserRole.TEACHING -> "FAC-TCH-01"
                UserRole.STUDENT -> "ADM-2026-001"
                UserRole.PARENT -> "PAR-ADM-2026-001"
                UserRole.ACCOUNTANT -> "FAC-ACC-01"
                else -> "USR-GEN-01"
            },
            role = role,
            roleClaim = role.roleClaim,
            displayName = when (role) {
                UserRole.ADMIN -> "Dr. Elena Vance (Administrator)"
                UserRole.PRINCIPAL -> "Prof. Robert Vance (Principal)"
                UserRole.INCHARGE -> "Mrs. Sunita Kapoor (Class Incharge 10-A)"
                UserRole.TEACHER, UserRole.TEACHING -> "Mrs. Sunita Kapoor (Senior Faculty & Teacher)"
                UserRole.STUDENT -> "Aarav Sharma (Scholar Roll #1)"
                UserRole.PARENT -> "Vikram Sharma (Guardian)"
                UserRole.ACCOUNTANT -> "Suresh Mehta (Accounts Head)"
                else -> "Operations Staff"
            },
            phone = "9811122334",
            email = "demo@stxaviers.edu",
            designationOrGrade = when (role) {
                UserRole.STUDENT, UserRole.PARENT -> "Grade 10-A"
                else -> "Staff Faculty"
            },
            assignedClassSection = "10-A",
            permissions = getDefaultPermissionsForRole(role),
            sessionToken = "SEC-DEV-SIM-${System.currentTimeMillis()}"
        )
        _userClaims.value = dummyClaims
    }

    fun selectRole(role: UserRole) = switchRole(role)

    private fun updateProfileNameForRole(role: UserRole) {
        _userProfileName.value = when (role) {
            UserRole.ADMIN -> "Dr. Elena Vance (System Administrator)"
            UserRole.PRINCIPAL -> "Prof. Robert Vance, Ph.D. (Principal)"
            UserRole.VICE_PRINCIPAL -> "Vice Principal Desk"
            UserRole.INCHARGE -> _authenticatedIncharge.value?.name?.let { "$it (Incharge 10-A)" } ?: "Mrs. Sunita Kapoor (Incharge 10-A)"
            UserRole.TEACHER, UserRole.TEACHING -> _authenticatedTeacher.value?.name?.let { "$it (Faculty & Teacher)" } ?: "Mrs. Sunita Kapoor (Faculty & Teacher)"
            UserRole.STUDENT -> _authenticatedStudent.value?.let { "${it.name} (${it.admissionNumber})" } ?: "Aarav Sharma (ADM-2026-001)"
            UserRole.PARENT -> "Vikram Sharma (Guardian of Aarav)"
            UserRole.ACCOUNTANT -> "Suresh Mehta (Accounts Manager)"
            UserRole.OFFICE -> "Registrar & Admissions Desk"
            UserRole.STAFF_MANAGER -> "Staff Directorate"
            UserRole.LIBRARIAN -> "Anita Roy (Chief Librarian)"
            UserRole.TRANSPORT_STAFF -> "Harish Chandra (Transport Head)"
            UserRole.NON_TEACHING -> "Rajesh Verma (Operations Head)"
        }
    }

    // --- PRINCIPAL SECURITY ---
    fun authenticatePrincipal(password: String): Boolean {
        if (password.trim() == "admin123" || password.trim() == "9876" || password.trim() == "principal") {
            _isPrincipalAuthenticated.value = true
            prefs?.edit()?.putBoolean("auth_principal", true)?.apply()
            _currentRole.value = UserRole.PRINCIPAL
            updateProfileNameForRole(UserRole.PRINCIPAL)
            val claims = SecurityClaims(
                userId = "FAC-PRIN-01",
                role = UserRole.PRINCIPAL,
                roleClaim = "ROLE_PRINCIPAL",
                displayName = "Prof. Robert Vance, Ph.D.",
                phone = "9811122334",
                email = "principal@stxaviers.edu",
                designationOrGrade = "Principal & Executive Director",
                permissions = getDefaultPermissionsForRole(UserRole.PRINCIPAL),
                sessionToken = "SEC-PRIN-${System.currentTimeMillis()}"
            )
            _userClaims.value = claims
            saveSessionToPrefs(claims)
            return true
        }
        return false
    }

    fun lockPrincipal() {
        _isPrincipalAuthenticated.value = false
        prefs?.edit()?.putBoolean("auth_principal", false)?.apply()
    }

    // --- TEACHER 2FA BINDING ---
    suspend fun verifyTeacherPhoneAndOtp(phone: String, otp: String): Pair<Boolean, String> {
        val cleanPhone = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        val staff = repository.getStaffByPhone(cleanPhone)
            ?: return Pair(false, "Phone number is not found in the Principal's Staff Registry.")
        if (otp.length != 6) return Pair(false, "Invalid OTP code.")

        repository.updateStaffOtpVerified(staff.id, true)
        _authenticatedTeacher.value = staff.copy(isOtpVerified = true)
        _isTeacherAuthenticated.value = true
        prefs?.edit()?.putBoolean("auth_teacher", true)?.putString("saved_teacher_phone", cleanPhone)?.apply()
        _currentRole.value = UserRole.TEACHING
        updateProfileNameForRole(UserRole.TEACHING)
        return Pair(true, "Faculty verified successfully.")
    }

    // --- INCHARGE 2FA BINDING ---
    suspend fun verifyInchargePhoneAndOtp(phone: String, otp: String): Pair<Boolean, String> {
        val cleanPhone = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        val staff = repository.getStaffByPhone(cleanPhone)
            ?: return Pair(false, "Phone number not registered in Staff Registry.")
        if (otp.length != 6) return Pair(false, "Invalid OTP code.")

        repository.updateStaffOtpVerified(staff.id, true)
        _authenticatedIncharge.value = staff.copy(isOtpVerified = true)
        _isInchargeAuthenticated.value = true
        prefs?.edit()?.putBoolean("auth_incharge", true)?.putString("saved_incharge_phone", cleanPhone)?.apply()
        _currentRole.value = UserRole.INCHARGE
        updateProfileNameForRole(UserRole.INCHARGE)
        return Pair(true, "Class Incharge verified successfully.")
    }

    // --- STUDENT 2FA BINDING ---
    suspend fun verifyStudentAdmissionAndOtp(admissionNo: String, phone: String, otp: String): Pair<Boolean, String> {
        val student = repository.getStudentByAdmissionAndPhone(admissionNo, phone)
            ?: return Pair(false, "Admission Number ($admissionNo) and Phone do not match our enrolled records.")
        if (otp.length != 6) return Pair(false, "Invalid OTP code.")

        _authenticatedStudent.value = student
        _isStudentAuthenticated.value = true
        prefs?.edit()?.putBoolean("auth_student", true)?.putString("saved_student_admission", student.admissionNumber)?.apply()
        _currentRole.value = UserRole.STUDENT
        updateProfileNameForRole(UserRole.STUDENT)
        return Pair(true, "Student Scholar profile verified successfully.")
    }

    fun resetAllOneTimeSecurityForDemo() {
        _isPrincipalAuthenticated.value = false
        _isTeacherAuthenticated.value = false
        _isInchargeAuthenticated.value = false
        _isStudentAuthenticated.value = false
        _authenticatedTeacher.value = null
        _authenticatedIncharge.value = null
        _authenticatedStudent.value = null
        prefs?.edit()?.clear()?.apply()
    }

    // =========================================================================
    // SYNCHRONIZATION ENGINE & FIRESTORE CLOUD INTEGRATION
    // =========================================================================

    fun triggerSyncNow(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val result = cloudSyncManager.syncAll(instId)
            _isSyncing.value = false
            val timeStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            _lastSyncTime.value = "Just now ($timeStr)"

            result.onSuccess { count ->
                val msg = cloudSyncManager.syncMessage.value
                logAuditEvent(
                    action = "CLOUD_SYNC_COMPLETED",
                    entityType = "SYNC_ENGINE",
                    entityId = "SYNC-${System.currentTimeMillis() % 10000}",
                    previousState = "PENDING_CHANGES",
                    newState = "SYNCHRONIZED",
                    remarks = "Sync completed for $instId: $msg ($count items processed)"
                )
                onComplete?.invoke(true, msg)
            }.onFailure { err ->
                val errMsg = err.localizedMessage ?: "Sync deferred"
                onComplete?.invoke(false, errMsg)
            }
        }
    }

    // =========================================================================
    // ATTENDANCE & REGISTERS
    // =========================================================================

    fun markStudentAttendance(
        studentId: String,
        studentName: String,
        status: String,
        role: String = "TEACHER",
        markedByName: String? = null
    ) {
        viewModelScope.launch {
            val rec = AttendanceRecordEntity(
                personId = studentId,
                personName = studentName,
                roleType = "STUDENT",
                date = "2026-09-15",
                status = status,
                timeMarked = "08:15 AM",
                markedByRole = role,
                markedByName = markedByName ?: _userProfileName.value,
                classSection = "10-A",
                sourceMethod = "CAMERA_REGISTER_OCR"
            )
            attendanceRepository.insertAttendanceRecord(rec)
            logAuditEvent(
                action = "ROLL_CALL_MARKED",
                entityType = "ATTENDANCE_RECORD",
                entityId = studentId,
                previousState = "UNRECORDED",
                newState = status,
                remarks = "Marked $status by ${markedByName ?: _userProfileName.value}"
            )
        }
    }

    fun commitRegisterScanBatch(batch: RegisterScanBatchEntity, records: List<AttendanceRecordEntity>) {
        viewModelScope.launch {
            attendanceRepository.commitRegisterScan(batch, records)
            logAuditEvent(
                action = "FACULTY_SUBMIT_ATTENDANCE_BATCH",
                entityType = "ATTENDANCE_BATCH",
                entityId = batch.batchId,
                previousState = "CAMERA_OCR_SCANNED",
                newState = batch.status,
                remarks = "Attendance batch submitted for Section ${batch.classSection}. Present: ${batch.presentCount}, Absent: ${batch.absentCount}."
            )
        }
    }
    fun commitRegisterBatch(batch: RegisterScanBatchEntity, records: List<AttendanceRecordEntity>) =
        commitRegisterScanBatch(batch, records)

    fun verifyRegisterBatchIncharge(batchId: String) {
        viewModelScope.launch {
            attendanceRepository.verifyBatchByIncharge(batchId)
            val claims = _userClaims.value
            val audit = AuditLogEntity(
                action = "INCHARGE_VERIFY_ATTENDANCE_BATCH",
                entityType = "ATTENDANCE_BATCH",
                entityId = batchId,
                actorUserId = claims?.userId ?: "FAC-TCH-02",
                actorName = claims?.displayName ?: "Mrs. Sunita Kapoor (Class Incharge)",
                actorRole = "ROLE_INCHARGE",
                previousState = "PENDING_INCHARGE_VERIFICATION",
                newState = "VERIFIED_BY_INCHARGE",
                remarks = "Section Incharge verified roll-call anomalies."
            )
            repository.insertAuditLog(audit)
        }
    }

    fun approveRegisterBatchPrincipal(batchId: String) {
        viewModelScope.launch {
            attendanceRepository.approveBatchByPrincipal(batchId)
            val claims = _userClaims.value
            val audit = AuditLogEntity(
                action = "PRINCIPAL_APPROVE_ATTENDANCE_BATCH",
                entityType = "ATTENDANCE_BATCH",
                entityId = batchId,
                actorUserId = claims?.userId ?: "FAC-PRIN-01",
                actorName = claims?.displayName ?: "Prof. Robert Vance, Ph.D. (Principal)",
                actorRole = "ROLE_PRINCIPAL",
                previousState = "VERIFIED_BY_INCHARGE",
                newState = "APPROVED_BY_PRINCIPAL",
                remarks = "Executive seal applied. Central ledger committed permanently."
            )
            repository.insertAuditLog(audit)
        }
    }

    // =========================================================================
    // EXAMINATIONS & REPORT CARDS
    // =========================================================================

    fun recordExamMarks(
        examId: String,
        examType: String,
        examName: String,
        classSection: String,
        subject: String,
        studentId: String,
        studentName: String,
        rollNo: Int,
        marksObtained: Float,
        maxMarks: Float = 100f,
        grade: String,
        remarks: String
    ) {
        viewModelScope.launch {
            val examRecord = ExamRecordEntity(
                examId = examId,
                examType = examType,
                examName = examName,
                classSection = classSection,
                subject = subject,
                studentId = studentId,
                studentName = studentName,
                rollNumber = rollNo,
                marksObtained = marksObtained,
                maxMarks = maxMarks,
                grade = grade,
                teacherRemarks = remarks,
                enteredByStaffId = _userClaims.value?.userId ?: "FAC-TCH-02",
                isPublished = true
            )
            repository.insertExamRecord(examRecord)
            logAuditEvent(
                action = "MARKS_ENTERED",
                entityType = "EXAM_RECORD",
                entityId = examId,
                previousState = "UNENTERED",
                newState = "PUBLISHED",
                remarks = "Marks for $subject ($marksObtained/$maxMarks, Grade: $grade) recorded for $studentName"
            )
        }
    }

    // =========================================================================
    // ID CARDS & CERTIFICATES
    // =========================================================================

    fun saveConvertedIdCard(record: IdCardRecordEntity) {
        viewModelScope.launch {
            repository.saveIdCard(record)
            logAuditEvent(
                action = "DIGITAL_ID_GENERATED",
                entityType = "ID_CARD",
                entityId = record.cardId,
                previousState = "RAW_CAMERA_CAPTURE",
                newState = record.verificationStatus,
                remarks = "Computerized ID generated for ${record.personName}."
            )
        }
    }

    fun inchargeVerifyIdCard(cardId: String, remarks: String) {
        viewModelScope.launch {
            repository.inchargeVerifyIdCard(cardId, "VERIFIED_BY_INCHARGE", remarks)
            logAuditEvent(
                action = "INCHARGE_VERIFY_ID_CARD",
                entityType = "ID_CARD",
                entityId = cardId,
                previousState = "PENDING_INCHARGE",
                newState = "VERIFIED_BY_INCHARGE",
                remarks = remarks
            )
        }
    }
    fun verifyIdCardIncharge(cardId: String, remarks: String) = inchargeVerifyIdCard(cardId, remarks)

    fun principalApproveIdCard(cardId: String, remarks: String) {
        viewModelScope.launch {
            repository.principalApproveIdCard(cardId, "APPROVED_BY_PRINCIPAL", remarks, _userProfileName.value)
            logAuditEvent(
                action = "PRINCIPAL_APPROVE_ID_CARD",
                entityType = "ID_CARD",
                entityId = cardId,
                previousState = "VERIFIED_BY_INCHARGE",
                newState = "APPROVED_BY_PRINCIPAL",
                remarks = remarks
            )
        }
    }
    fun approveIdCardPrincipal(cardId: String, remarks: String) = principalApproveIdCard(cardId, remarks)

    fun applyForCertificate(type: String, studentId: String, studentName: String, grade: String, purpose: String) {
        viewModelScope.launch {
            val certId = "CERT-2026-${(1000..9999).random()}"
            val cert = CertificateEntity(
                certificateId = certId,
                certificateType = type,
                studentName = studentName,
                studentId = studentId,
                grade = grade,
                issueDate = "15 Sep 2026",
                academicYear = "2026-2027",
                purposeOrAchievement = purpose,
                applicationStatus = "PENDING_INCHARGE",
                isDownloadUnlocked = false
            )
            repository.insertCertificate(cert)
            logAuditEvent(
                action = "STUDENT_APPLY_CERTIFICATE",
                entityType = "CERTIFICATE",
                entityId = certId,
                previousState = "NONE",
                newState = "PENDING_INCHARGE",
                remarks = "Certificate ($type) requested by $studentName ($studentId)."
            )
        }
    }

    fun inchargeVerifyCertificate(certId: String, inchargeName: String, remarks: String) {
        viewModelScope.launch {
            repository.inchargeVerifyCertificate(certId, inchargeName, remarks)
            logAuditEvent(
                action = "INCHARGE_VERIFY_CERTIFICATE",
                entityType = "CERTIFICATE",
                entityId = certId,
                previousState = "PENDING_INCHARGE",
                newState = "VERIFIED_BY_INCHARGE",
                remarks = remarks
            )
        }
    }
    fun verifyCertificateIncharge(
        certId: String,
        inchargeName: String = "Mrs. Sunita Kapoor (Class Incharge)",
        remarks: String = ""
    ) = inchargeVerifyCertificate(certId, inchargeName, remarks)

    fun principalApproveCertificate(certId: String, remarks: String) {
        viewModelScope.launch {
            val token = "SXA-CERT-${(1000..9999).random()}"
            repository.principalApproveCertificate(certId, _userProfileName.value, remarks, token)
            logAuditEvent(
                action = "PRINCIPAL_APPROVE_CERTIFICATE",
                entityType = "CERTIFICATE",
                entityId = certId,
                previousState = "VERIFIED_BY_INCHARGE",
                newState = "APPROVED_BY_PRINCIPAL",
                remarks = remarks
            )
        }
    }
    fun approveCertificatePrincipal(certId: String, remarks: String) = principalApproveCertificate(certId, remarks)

    // =========================================================================
    // FEE MANAGEMENT & RECEIPTS (PHASE D)
    // =========================================================================

    fun updateFeeStudentQuery(query: String) {
        _feeStudentQuery.value = query
    }

    fun selectFeeStudent(studentId: String?) {
        _selectedFeeStudentId.value = studentId
        _feeOperationUiFlags.update { it.copy(validationError = null) }
    }

    fun clearFeeFeedback() {
        _feeOperationUiFlags.update { it.copy(validationError = null, feedbackMessage = null) }
    }

    fun dismissFeeReceiptPreview() {
        _feeOperationUiFlags.update { it.copy(activeReceiptResult = null) }
    }

    fun createFeeAssessmentForStudent(
        studentId: String,
        term: String,
        feeType: String,
        totalAmountRupees: Long,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            _feeOperationUiFlags.update { it.copy(isSubmitting = true, validationError = null, feedbackMessage = null) }
            val result = repository.createFeeAssessmentSafe(
                CreateFeeAssessmentRequest(
                    institutionId = _verifiedInstitution.value?.institutionId,
                    studentId = studentId,
                    term = term,
                    feeType = feeType,
                    totalAmountPaise = totalAmountRupees * 100L
                )
            )
            result.onSuccess { fee ->
                _feeOperationUiFlags.update {
                    it.copy(
                        isSubmitting = false,
                        feedbackMessage = "Fee assessment '${fee.feeType}' (₹${fee.totalRupees}) created for ${fee.studentName}. Queued for sync."
                    )
                }
                onResult(true, "Fee assessment created for ${fee.studentName}.")
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to create fee assessment."
                _feeOperationUiFlags.update { it.copy(isSubmitting = false, validationError = msg) }
                onResult(false, msg)
            }
        }
    }

    fun recordManualFeePayment(
        studentId: String,
        feeRecordId: String,
        amountRupees: Long,
        paymentMode: String,
        transactionReference: String,
        remarks: String = "",
        idempotencyKey: String = java.util.UUID.randomUUID().toString(),
        onResult: (Boolean, String, FeePaymentReceiptResult?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch {
            _feeOperationUiFlags.update { it.copy(isSubmitting = true, validationError = null, feedbackMessage = null) }
            val result = repository.recordManualPaymentSafe(
                ManualFeePaymentRequest(
                    institutionId = _verifiedInstitution.value?.institutionId,
                    studentId = studentId,
                    feeRecordId = feeRecordId,
                    amountPaise = amountRupees * 100L,
                    paymentMode = paymentMode,
                    transactionReference = transactionReference,
                    remarks = remarks,
                    idempotencyKey = idempotencyKey
                )
            )
            result.onSuccess { receiptResult ->
                _feeOperationUiFlags.update {
                    it.copy(
                        isSubmitting = false,
                        feedbackMessage = "Manual receipt ${receiptResult.transaction.receiptNumber} recorded (PENDING_SYNC until server acknowledgement).",
                        activeReceiptResult = receiptResult
                    )
                }
                onResult(true, "Receipt ${receiptResult.transaction.receiptNumber} generated.", receiptResult)
            }.onFailure { err ->
                val msg = err.localizedMessage ?: "Failed to record fee payment."
                _feeOperationUiFlags.update { it.copy(isSubmitting = false, validationError = msg) }
                onResult(false, msg, null)
            }
        }
    }

    fun syncPendingFinancialOutbox() {
        viewModelScope.launch {
            _feeOperationUiFlags.update { it.copy(isSyncingOutbox = true, syncStatusMessage = "Syncing pending financial records...") }
            val result = repository.feeRepository.syncPendingFinancialOutbox(forceAll = true)
            result.onSuccess { batch ->
                _feeOperationUiFlags.update {
                    it.copy(
                        isSyncingOutbox = false,
                        syncStatusMessage = if (batch.acknowledgedCount > 0) {
                            "Server confirmed ${batch.acknowledgedCount} record(s)."
                        } else if (batch.failedCount > 0) {
                            "Offline/retryable: ${batch.failedCount} record(s) preserved in local queue."
                        } else if (batch.conflictCount > 0) {
                            "Security/conflict on ${batch.conflictCount} record(s)."
                        } else {
                            "No pending financial records."
                        }
                    )
                }
            }.onFailure { err ->
                _feeOperationUiFlags.update {
                    it.copy(
                        isSyncingOutbox = false,
                        syncStatusMessage = err.localizedMessage ?: "Unable to sync financial records."
                    )
                }
            }
        }
    }

    @Deprecated("Use recordManualFeePayment which enforces role, tenant, balance, and atomic outbox integrity.")
    fun inchargeRecordFeePayment(feeId: String, paidAmount: Long, dueAmount: Long, receiptNo: String) {
        viewModelScope.launch {
            val fee = repository.dao.getFeeRecordById(feeId) ?: return@launch
            repository.recordManualPaymentSafe(
                ManualFeePaymentRequest(
                    institutionId = fee.institutionId,
                    studentId = fee.studentId,
                    feeRecordId = feeId,
                    amountPaise = if (paidAmount >= 100000L) paidAmount else paidAmount * 100L,
                    paymentMode = "CASH",
                    transactionReference = receiptNo,
                    customReceiptNumber = receiptNo
                )
            )
        }
    }

    // =========================================================================
    // STAFF REGISTRY & UNIQUE PHONE ENFORCEMENT
    // =========================================================================

    suspend fun checkStaffPhoneUnique(phone: String, excludeId: String? = null): Boolean {
        return repository.isStaffPhoneUnique(phone, excludeId)
    }

    fun addStaffMember(staff: StaffEntity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val isUnique = repository.isStaffPhoneUnique(staff.phone)
            if (!isUnique) {
                onResult(false, "Error: Mobile number ${staff.phone} is already registered to another staff member! Each teacher can have only one phone number.")
                return@launch
            }
            repository.insertStaff(staff)
            onResult(true, "Staff member ${staff.name} successfully registered in School DBMS.")
            logAuditEvent(
                action = "STAFF_REGISTERED",
                entityType = "STAFF_RECORD",
                entityId = staff.id,
                previousState = "NONE",
                newState = "REGISTERED",
                remarks = "Staff ${staff.name} (${staff.designation}) registered."
            )
        }
    }

    fun updateStaffMember(staff: StaffEntity, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val isUnique = repository.isStaffPhoneUnique(staff.phone, staff.id)
            if (!isUnique) {
                onResult(false, "Error: Mobile number ${staff.phone} is already registered to another staff member.")
                return@launch
            }
            repository.updateStaff(staff)
            onResult(true, "Staff record updated.")
        }
    }

    fun deleteStaffMember(staffId: String) {
        viewModelScope.launch {
            repository.deleteStaff(staffId)
            logAuditEvent(
                action = "STAFF_DELETED",
                entityType = "STAFF_RECORD",
                entityId = staffId,
                previousState = "ACTIVE",
                newState = "DELETED",
                remarks = "Staff member $staffId removed from database."
            )
        }
    }

    // =========================================================================
    // ADMISSIONS & ENROLLMENT
    // =========================================================================

    fun submitAdmissionApplication(app: AdmissionApplicationEntity) {
        viewModelScope.launch {
            repository.insertAdmissionApplication(app)
            logAuditEvent(
                action = "ADMISSION_APPLICATION_SUBMITTED",
                entityType = "ADMISSION",
                entityId = app.applicationId,
                previousState = "NONE",
                newState = "PENDING_VERIFICATION",
                remarks = "Admission application submitted for ${app.studentName}"
            )
        }
    }

    fun verifyAdmissionApplication(appId: String) {
        viewModelScope.launch {
            val app = repository.allAdmissionApplications.stateIn(viewModelScope).value.firstOrNull { it.applicationId == appId }
            if (app != null) {
                repository.updateAdmissionApplication(app.copy(status = "VERIFIED_OFFICE"))
                logAuditEvent(
                    action = "ADMISSION_VERIFIED_OFFICE",
                    entityType = "ADMISSION",
                    entityId = appId,
                    previousState = "PENDING_VERIFICATION",
                    newState = "VERIFIED_OFFICE",
                    remarks = "Office verified documents for ${app.studentName}"
                )
            }
        }
    }

    fun approveAdmissionAndEnroll(appId: String, grade: String, section: String) {
        viewModelScope.launch {
            val app = repository.allAdmissionApplications.stateIn(viewModelScope).value.firstOrNull { it.applicationId == appId }
            if (app != null) {
                val newAdmissionNo = "ADM-2026-${(100..999).random()}"
                val student = StudentEntity(
                    id = newAdmissionNo,
                    admissionNumber = newAdmissionNo,
                    name = app.studentName,
                    grade = grade,
                    section = section,
                    rollNumber = (10..40).random(),
                    gender = app.gender,
                    dateOfBirth = app.dob,
                    guardianName = app.guardianName,
                    guardianPhone = app.guardianPhone,
                    studentPhone = app.guardianPhone,
                    email = app.guardianEmail,
                    bloodGroup = "O+",
                    attendancePercentage = 100f,
                    feeDue = 65000.0,
                    feePaid = 0.0,
                    avatarColorHex = "#0B2545",
                    address = app.address,
                    realIdPhotoUri = null,
                    convertedDigitalId = "SXA-DIGI-${newAdmissionNo}",
                    digitalIdStatus = "PENDING_TEACHER_CAPTURE"
                )
                repository.insertStudent(student)
                repository.updateAdmissionApplication(
                    app.copy(
                        status = "APPROVED_BY_PRINCIPAL",
                        assignedAdmissionNumber = newAdmissionNo,
                        assignedClassSection = "$grade-$section"
                    )
                )
                logAuditEvent(
                    action = "ADMISSION_APPROVED_ENROLLED",
                    entityType = "ADMISSION",
                    entityId = appId,
                    previousState = "VERIFIED_OFFICE",
                    newState = "ENROLLED_STUDENT",
                    remarks = "Enrolled ${app.studentName} into $grade-$section with Admission No $newAdmissionNo"
                )
            }
        }
    }

    // =========================================================================
    // INSTITUTIONAL BILLS & COMPLIANCE
    // =========================================================================

    fun recordBillPayment(billId: String, receiptRef: String) {
        viewModelScope.launch {
            val bill = repository.allBills.stateIn(viewModelScope).value.firstOrNull { it.billId == billId }
            if (bill != null) {
                repository.updateBill(
                    bill.copy(
                        paymentStatus = "PAID",
                        paidDate = "2026-09-15",
                        receiptRef = receiptRef
                    )
                )
                logAuditEvent(
                    action = "INSTITUTION_BILL_PAID",
                    entityType = "BILLING",
                    entityId = billId,
                    previousState = "PENDING",
                    newState = "PAID",
                    remarks = "Paid ${bill.billType} bill:  ${bill.amountRupees}. Ref: $receiptRef"
                )
            }
        }
    }

    // =========================================================================
    // SCHOOL DIARY & CIRCULARS
    // =========================================================================

    fun addSchoolDiaryEntry(entry: SchoolDiaryEntity) {
        viewModelScope.launch {
            repository.insertDiaryEntry(entry)
            logAuditEvent(
                action = "DIARY_ENTRY_POSTED",
                entityType = "SCHOOL_DIARY",
                entityId = entry.id,
                previousState = "NONE",
                newState = "PUBLISHED",
                remarks = "Homework posted for Class ${entry.classSection}: ${entry.subject}"
            )
        }
    }

    fun publishNotice(notice: NoticeMeetingEntity) {
        viewModelScope.launch {
            repository.insertNotice(notice)
            logAuditEvent(
                action = "CIRCULAR_PUBLISHED",
                entityType = "CIRCULAR",
                entityId = notice.id,
                previousState = "DRAFT",
                newState = "PUBLISHED",
                remarks = "Published: ${notice.title} to ${notice.venueOrAudience}"
            )
        }
    }

    // Student promotion between academic years
    fun promoteStudent(studentId: String, newGrade: String, newSection: String) {
        viewModelScope.launch {
            repository.promoteStudent(studentId, newGrade, newSection)
            logAuditEvent(
                action = "STUDENT_PROMOTED",
                entityType = "STUDENT_RECORD",
                entityId = studentId,
                previousState = "CURRENT",
                newState = "PROMOTED_TO_${newGrade}",
                remarks = "Student promoted to Grade $newGrade Section $newSection"
            )
        }
    }
    val aiAssistant: AiAcademicAssistant = AiAcademicAssistant()

    // =========================================================================
    // AI ACADEMIC TOOLS & GEMINI AUTOMATION
    // =========================================================================

    suspend fun generateStudentAiEvaluation(
        student: StudentEntity,
        exams: List<ExamRecordEntity>,
        attendancePercentage: Float
    ): String {
        return aiAssistant.generateStudentPerformanceRemarks(
            student = student,
            exams = exams,
            attendancePercentage = attendancePercentage
        )
    }

    suspend fun generateSmartNotice(
        title: String,
        circularType: String,
        targetAudience: String,
        keyPoints: String,
        effectiveDate: String
    ): String {
        return aiAssistant.draftSmartCircular(
            title = title,
            circularType = circularType,
            targetAudience = targetAudience,
            keyPoints = keyPoints,
            effectiveDate = effectiveDate
        )
    }

    suspend fun generateClassQuizAndRubric(
        grade: String,
        subject: String,
        topic: String,
        totalMarks: Int,
        difficultyLevel: String,
        questionTypes: String
    ): String {
        return aiAssistant.generateTestPaperAndRubric(
            grade = grade,
            subject = subject,
            topic = topic,
            totalMarks = totalMarks,
            difficultyLevel = difficultyLevel,
            questionTypes = questionTypes
        )
    }

    suspend fun getAttendanceAndFeeRiskAnalysis(
        students: List<StudentEntity>,
        feeRecords: List<FeeRecordEntity>
    ): String {
        return aiAssistant.analyzeAttendanceAndFeeRisks(
            students = students,
            feeRecords = feeRecords
        )
    }

    // =========================================================================
    // MULTI-TENANT INSTITUTION ONBOARDING
    // =========================================================================
    fun onboardNewInstitution(
        name: String,
        regCode: String,
        affiliation: String,
        address: String,
        phone: String,
        email: String,
        motto: String,
        principalName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isBlank() || regCode.isBlank()) {
                onResult(false, "Institution name and registration code are required.")
                return@launch
            }
            val instId = "INST-${regCode.trim().uppercase()}-${System.currentTimeMillis() % 1000}"
            val newInst = InstitutionEntity(
                institutionId = instId,
                registrationCode = regCode.trim(),
                name = name.trim(),
                motto = motto.ifBlank { "Knowledge, Truth & Excellence" },
                affiliationCode = affiliation.trim(),
                address = address.trim(),
                cityCampus = address.take(25),
                contactPhone = phone.trim(),
                email = email.trim(),
                website = "https://${regCode.lowercase()}.edu",
                currentAcademicYear = "2026-2027",
                availableBatches = "2026-27,2025-26",
                principalName = principalName.ifBlank { "Executive Principal" }
            )
            repository.insertInstitution(newInst)
            logAuditEvent(
                action = "INSTITUTION_ONBOARDED_SAAS",
                entityType = "Institution",
                entityId = instId,
                previousState = null,
                newState = "ACTIVE",
                remarks = "New school tenant '$name' successfully onboarded into SaaS platform."
            )
            onResult(true, "Institution successfully registered.")
        }
    }

    // =========================================================================
    // HOMEWORK & ASSIGNMENT MANAGEMENT
    // =========================================================================
    fun createHomework(
        title: String,
        description: String,
        classSection: String,
        subject: String,
        dueDate: String,
        maxMarks: Int,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (title.isBlank() || classSection.isBlank()) {
                onResult(false, "Title and class section are required.")
                return@launch
            }
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val hw = HomeworkEntity(
                homeworkId = "HW-${classSection.replace("-", "")}-${System.currentTimeMillis() % 10000}",
                institutionId = instId,
                classSection = classSection,
                subject = subject,
                title = title,
                description = description,
                dueDate = dueDate,
                maxMarks = maxMarks,
                teacherId = _userClaims.value?.userId ?: "FAC-TCH-02",
                teacherName = _userProfileName.value
            )
            repository.insertHomework(hw)
            logAuditEvent(
                action = "HOMEWORK_ASSIGNED",
                entityType = "Homework",
                entityId = hw.homeworkId,
                previousState = null,
                newState = "PUBLISHED",
                remarks = "Homework '$title' assigned to class $classSection by ${_userProfileName.value}"
            )
            onResult(true, "Homework published successfully.")
        }
    }

    fun submitHomework(
        homeworkId: String,
        submissionText: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (submissionText.isBlank()) {
                onResult(false, "Submission text cannot be empty.")
                return@launch
            }
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val studentId = _authenticatedStudent.value?.id ?: "ADM-2026-001"
            val studentName = _authenticatedStudent.value?.name ?: "Aarav Sharma"
            val sub = HomeworkSubmissionEntity(
                submissionId = "SUB-$homeworkId-$studentId",
                homeworkId = homeworkId,
                institutionId = instId,
                studentId = studentId,
                studentName = studentName,
                submissionText = submissionText,
                status = "SUBMITTED"
            )
            repository.insertHomeworkSubmission(sub)
            logAuditEvent(
                action = "HOMEWORK_SUBMITTED",
                entityType = "HomeworkSubmission",
                entityId = sub.submissionId,
                previousState = null,
                newState = "SUBMITTED",
                remarks = "Scholar $studentName submitted assignment $homeworkId"
            )
            onResult(true, "Assignment submitted successfully.")
        }
    }

    fun gradeHomework(
        submissionId: String,
        marks: Int,
        feedback: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val auditLog = AuditLogEntity(
                institutionId = instId,
                action = "HOMEWORK_GRADED",
                entityType = "HomeworkSubmission",
                entityId = submissionId,
                actorUserId = _userClaims.value?.userId ?: "TEACHER",
                actorName = _userProfileName.value,
                actorRole = "ROLE_TEACHER",
                previousState = "SUBMITTED",
                newState = "EVALUATED",
                remarks = "Graded with score $marks and remarks '$feedback'"
            )
            repository.gradeHomeworkSubmission(submissionId, "EVALUATED", marks, feedback, auditLog)
            onResult(true, "Submission successfully graded.")
        }
    }

    // =========================================================================
    // STUDENT LEAVE REQUESTS
    // =========================================================================
    fun applyStudentLeave(
        startDate: String,
        endDate: String,
        reason: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (reason.isBlank()) {
                onResult(false, "Please provide reason for leave.")
                return@launch
            }
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val studentId = _authenticatedStudent.value?.id ?: "ADM-2026-001"
            val studentName = _authenticatedStudent.value?.name ?: "Aarav Sharma"
            val req = StudentLeaveRequestEntity(
                leaveId = "LEV-${System.currentTimeMillis() % 100000}",
                institutionId = instId,
                studentId = studentId,
                studentName = studentName,
                classSection = _authenticatedStudent.value?.let { "${it.grade}-${it.section}" } ?: "10-A",
                startDate = startDate,
                endDate = endDate,
                reason = reason,
                status = "PENDING",
                appliedBy = if (_currentRole.value == UserRole.PARENT) "PARENT" else "STUDENT"
            )
            repository.submitLeaveRequest(req)
            logAuditEvent(
                action = "STUDENT_LEAVE_APPLIED",
                entityType = "LeaveRequest",
                entityId = req.leaveId,
                previousState = null,
                newState = "PENDING",
                remarks = "Leave request submitted for $studentName ($startDate to $endDate) - $reason"
            )
            onResult(true, "Leave request submitted to Class In-Charge.")
        }
    }

    fun reviewLeaveRequest(
        leaveId: String,
        isApproved: Boolean,
        remarks: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            val status = if (isApproved) "APPROVED" else "REJECTED"
            val auditLog = AuditLogEntity(
                institutionId = instId,
                action = "LEAVE_REQUEST_REVIEWED",
                entityType = "LeaveRequest",
                entityId = leaveId,
                actorUserId = _userClaims.value?.userId ?: "INCHARGE",
                actorName = _userProfileName.value,
                actorRole = _currentRole.value.roleClaim,
                previousState = "PENDING",
                newState = status,
                remarks = "Leave $status by ${_userProfileName.value}: $remarks"
            )
            repository.reviewLeaveRequest(leaveId, status, _userProfileName.value, remarks, auditLog)
            onResult(true, "Leave request $status.")
        }
    }

    // =========================================================================
    // FEE MANAGEMENT & PAYMENT TRANSACTIONS
    // =========================================================================
    fun processFeePayment(
        feeRecord: FeeRecordEntity,
        amountRupees: Long,
        paymentMode: String,
        transactionReference: String = "",
        remarks: String = "",
        idempotencyKey: String = java.util.UUID.randomUUID().toString(),
        onResult: (Boolean, String) -> Unit
    ) {
        recordManualFeePayment(
            studentId = feeRecord.studentId,
            feeRecordId = feeRecord.id,
            amountRupees = amountRupees,
            paymentMode = paymentMode,
            transactionReference = transactionReference,
            remarks = remarks,
            idempotencyKey = idempotencyKey
        ) { success, message, _ ->
            onResult(success, message)
        }
    }

    // =========================================================================
    // PUSH NOTIFICATIONS & ALERTS
    // =========================================================================
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            val instId = _verifiedInstitution.value?.institutionId ?: "INST-SXA-2026"
            repository.markAllNotificationsAsRead(instId)
        }
    }

    fun selectWard(studentId: String) {
        viewModelScope.launch {
            _selectedWardId.value = studentId
            val student = repository.getStudentById(studentId)
            if (student != null) {
                _authenticatedStudent.value = student
            }
        }
    }

    fun publishGeneratedCircular(title: String, content: String, targetAudience: String) {
        val notice = NoticeMeetingEntity(
            id = "CIRC-AI-${System.currentTimeMillis() % 100000}",
            title = title,
            type = "CIRCULAR",
            date = "2026-09-20",
            time = "09:00 AM",
            venueOrAudience = targetAudience,
            description = content,
            isUrgent = false,
            organizer = "Office of the Principal, St. Xavier's International Academy"
        )
        publishNotice(notice)
    }
}

class SchoolViewModelFactory(
    private val repository: SchoolRepository,
    private val context: Context? = null,
    private val userRepository: UserRepository? = null,
    private val attendanceRepository: AttendanceRepository? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchoolViewModel::class.java)) {
            val userRepo = userRepository ?: repository.userRepository
            val attRepo = attendanceRepository ?: repository.attendanceRepository
            return SchoolViewModel(repository, userRepo, attRepo, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
