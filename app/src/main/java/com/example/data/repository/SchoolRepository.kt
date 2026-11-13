package com.example.data.repository

import com.example.data.local.dao.SchoolDao
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
import com.example.data.local.entities.TimetableSlotEntity
import com.example.data.local.entities.UserEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.example.data.remote.sync.DurableOutboxManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(
    val dao: SchoolDao,
    val outboxManager: DurableOutboxManager,
    val attendanceRepository: AttendanceRepository,
    val feeRepository: FeeRepository
) {

    constructor(dao: SchoolDao) : this(
        dao = dao,
        outboxManager = DurableOutboxManager(dao),
        attendanceRepository = AttendanceRepositoryImpl(dao, DurableOutboxManager(dao)),
        feeRepository = FeeRepositoryImpl(dao, DurableOutboxManager(dao))
    )

    constructor(dao: SchoolDao, outboxManager: DurableOutboxManager) : this(
        dao = dao,
        outboxManager = outboxManager,
        attendanceRepository = AttendanceRepositoryImpl(dao, outboxManager),
        feeRepository = FeeRepositoryImpl(dao, outboxManager)
    )

    constructor(
        dao: SchoolDao,
        outboxManager: DurableOutboxManager,
        attendanceRepository: AttendanceRepository
    ) : this(
        dao = dao,
        outboxManager = outboxManager,
        attendanceRepository = attendanceRepository,
        feeRepository = FeeRepositoryImpl(dao, outboxManager)
    )

    val userRepository: UserRepository = UserRepositoryImpl(dao)

    // =========================================================================
    // 1. INSTITUTION MASTER PROFILE
    // =========================================================================
    val institution: Flow<InstitutionEntity?> = dao.getInstitutionFlow()
    suspend fun getInstitution(): InstitutionEntity? = dao.getInstitution()
    suspend fun updateInstitution(institution: InstitutionEntity) = dao.updateInstitution(institution)
    suspend fun insertInstitution(institution: InstitutionEntity) = dao.insertInstitution(institution)

    // =========================================================================
    // 2. ACADEMIC CLASSES (Tenant-Aware)
    // =========================================================================
    val allClasses: Flow<List<AcademicClassEntity>> = dao.getAllClasses()
    suspend fun getAllAcademicClassesList(): List<AcademicClassEntity> = dao.getAllClassesList()
    fun getClassesForInstitution(institutionId: String): Flow<List<AcademicClassEntity>> =
        dao.getClassesForInstitution(institutionId)
    suspend fun insertClass(academicClass: AcademicClassEntity) = dao.insertAcademicClass(academicClass)
    suspend fun updateClass(academicClass: AcademicClassEntity) = dao.updateAcademicClass(academicClass)

    // =========================================================================
    // 3. STAFF MANAGEMENT (Unique Mobile Number Rule Enforced & Tenant-Aware)
    // =========================================================================
    val allStaff: Flow<List<StaffEntity>> = dao.getAllStaff()
    fun getStaffForInstitution(institutionId: String): Flow<List<StaffEntity>> =
        dao.getStaffForInstitution(institutionId)
    fun getStaffByRole(roleType: String): Flow<List<StaffEntity>> = dao.getStaffByRole(roleType)
    suspend fun getStaffById(id: String): StaffEntity? = dao.getStaffById(id)
    suspend fun getStaffByPhone(phone: String): StaffEntity? {
        val clean = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        return dao.getStaffByCleanPhone(clean)
    }
    suspend fun isStaffPhoneUnique(phone: String, excludeId: String? = null): Boolean {
        val clean = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        return dao.countStaffWithPhone(clean, excludeId) == 0
    }
    suspend fun insertStaff(staff: StaffEntity) = dao.insertStaff(staff)
    suspend fun updateStaff(staff: StaffEntity) = dao.updateStaff(staff)
    suspend fun deleteStaff(id: String) = dao.deleteStaff(id)
    suspend fun updateStaffOtpVerified(staffId: String, verified: Boolean) = dao.updateStaffOtpVerified(staffId, verified)
    suspend fun updateStaffAttendance(staffId: String, isPresent: Boolean, time: String) =
        dao.updateStaffAttendance(staffId, isPresent, time)
    suspend fun updateStaffIdCardStatus(
        staffId: String,
        status: String,
        photoUri: String?,
        digitalId: String?,
        approvedBy: String?,
        approvedDate: String?
    ) = dao.updateStaffIdCardStatus(staffId, status, photoUri, digitalId, approvedBy, approvedDate)

    // =========================================================================
    // 4. STUDENTS MANAGEMENT (Privacy & Isolation - Tenant-Aware)
    // =========================================================================
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudents()
    fun getStudentsForInstitution(institutionId: String): Flow<List<StudentEntity>> =
        dao.getStudentsForInstitution(institutionId)
    suspend fun getAllStudentsList(): List<StudentEntity> = dao.getAllStudentsList()
    suspend fun getStudentsListForInstitution(institutionId: String): List<StudentEntity> =
        dao.getStudentsListForInstitution(institutionId)
    fun getStudentsByGrade(grade: String): Flow<List<StudentEntity>> = dao.getStudentsByGrade(grade)
    fun getStudentsByClassSection(grade: String, section: String): Flow<List<StudentEntity>> =
        dao.getStudentsByClassSection(grade, section)
    suspend fun getStudentById(id: String): StudentEntity? = dao.getStudentById(id)
    suspend fun getStudentByAdmissionAndPhone(admissionNo: String, phone: String): StudentEntity? {
        val clean = phone.replace(" ", "").replace("-", "").replace("+91", "").trim()
        return dao.getStudentByAdmissionAndCleanPhone(admissionNo.trim(), clean)
    }
    suspend fun getStudentByAdmissionNumber(admissionNo: String): StudentEntity? {
        return dao.getStudentByAdmissionNumber(admissionNo.trim())
    }
    suspend fun insertStudent(student: StudentEntity) = dao.insertStudent(student)
    suspend fun updateStudent(student: StudentEntity) = dao.updateStudent(student)
    suspend fun deleteStudent(id: String) = dao.deleteStudent(id)
    suspend fun updateStudentAttendancePercentage(studentId: String, newPercentage: Float) =
        dao.updateStudentAttendancePercentage(studentId, newPercentage)
    suspend fun updateStudentIdCard(studentId: String, photoUri: String?, digitalId: String, status: String) =
        dao.updateStudentIdCard(studentId, photoUri, digitalId, status)
    suspend fun promoteStudent(studentId: String, newGrade: String, newSection: String) =
        dao.updateStudentPromotion(studentId, newGrade, newSection, "PROMOTED")

    // =========================================================================
    // 5. ATTENDANCE & REGISTER SCAN BATCHES (Tenant-Aware & Atomic Outbox Connected)
    // =========================================================================
    val allAttendanceRecords: Flow<List<AttendanceRecordEntity>> = attendanceRepository.allAttendanceRecords
    fun getAttendanceRecordsForInstitution(institutionId: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceRecordsForInstitution(institutionId)
    val allRegisterBatches: Flow<List<RegisterScanBatchEntity>> = attendanceRepository.allRegisterBatches
    fun getAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>> =
        attendanceRepository.observeAttendanceForPerson(personId)
    fun getAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>> =
        attendanceRepository.observeAttendanceForClass(classSection)

    suspend fun commitRegisterScan(batch: RegisterScanBatchEntity, records: List<AttendanceRecordEntity>) {
        attendanceRepository.commitRegisterScan(batch, records)
    }

    suspend fun inchargeVerifyBatch(batchId: String) = attendanceRepository.verifyBatchByIncharge(batchId)
    suspend fun principalApproveBatch(batchId: String) = attendanceRepository.approveBatchByPrincipal(batchId)

    suspend fun saveDailyAttendanceBatch(
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        updateStudentPercentages: Boolean = true
    ) = attendanceRepository.saveDailyAttendanceBatch(classSection, date, attendances, updateStudentPercentages)

    suspend fun recordAttendance(attendance: AttendanceEntity) =
        attendanceRepository.recordAttendance(attendance)

    // =========================================================================
    // 6. EXAMINATIONS & REPORT CARDS (Tenant-Aware)
    // =========================================================================
    val allExamRecords: Flow<List<ExamRecordEntity>> = dao.getAllExamRecords()
    fun getExamRecordsForInstitution(institutionId: String): Flow<List<ExamRecordEntity>> =
        dao.getExamRecordsForInstitution(institutionId)
    fun getExamRecordsForStudent(studentId: String): Flow<List<ExamRecordEntity>> =
        dao.getExamRecordsForStudent(studentId)
    fun getExamRecordsForClass(classSection: String): Flow<List<ExamRecordEntity>> =
        dao.getExamRecordsForClass(classSection)
    fun getExamRecordsForClassAndSubject(classSection: String, subject: String): Flow<List<ExamRecordEntity>> =
        dao.getExamRecordsForClassAndSubject(classSection, subject)
    suspend fun insertExamRecord(examRecord: ExamRecordEntity) = dao.insertExamRecord(examRecord)
    suspend fun insertExamRecords(records: List<ExamRecordEntity>) = dao.insertExamRecords(records)
    suspend fun updateExamRecord(examRecord: ExamRecordEntity) = dao.updateExamRecord(examRecord)

    // =========================================================================
    // 7. FEE MANAGEMENT & RECEIPTS (Tenant-Aware)
    // =========================================================================
    val allFeeRecords: Flow<List<FeeRecordEntity>> = dao.getAllFeeRecords()
    fun getFeeRecordsForInstitution(institutionId: String): Flow<List<FeeRecordEntity>> =
        dao.getFeeRecordsForInstitution(institutionId)
    fun getFeeRecordsForStudent(studentId: String): Flow<List<FeeRecordEntity>> =
        dao.getFeeRecordsForStudent(studentId)
    suspend fun insertFeeRecord(fee: FeeRecordEntity) = dao.insertFeeRecord(fee)
    suspend fun recordFeePayment(
        feeId: String,
        paid: Long,
        due: Long,
        status: String,
        receipt: String,
        date: String,
        mode: String = "OFFLINE_ENTRY"
    ) = dao.recordFeePayment(feeId, paid, due, status, receipt, date, mode)

    // =========================================================================
    // 8. CENTRALIZED DOCUMENTS & CERTIFICATES (Tenant-Aware)
    // =========================================================================
    val allCertificates: Flow<List<CertificateEntity>> = dao.getAllCertificates()
    fun getCertificatesForInstitution(institutionId: String): Flow<List<CertificateEntity>> =
        dao.getCertificatesForInstitution(institutionId)
    fun getCertificatesForStudent(studentId: String): Flow<List<CertificateEntity>> =
        dao.getCertificatesForStudent(studentId)
    suspend fun getCertificateById(certId: String): CertificateEntity? = dao.getCertificateById(certId)
    suspend fun insertCertificate(certificate: CertificateEntity) = dao.insertCertificate(certificate)
    suspend fun inchargeVerifyCertificate(id: String, incharge: String, remarks: String) =
        dao.inchargeVerifyCertificate(id, "VERIFIED_BY_INCHARGE", incharge, remarks)
    suspend fun principalApproveCertificate(id: String, principal: String, remarks: String, token: String) =
        dao.principalApproveCertificate(id, "APPROVED_BY_PRINCIPAL", principal, remarks, true, token)

    // =========================================================================
    // 9. COMPUTERIZED DIGITAL ID CARDS (Tenant-Aware)
    // =========================================================================
    val allIdCards: Flow<List<IdCardRecordEntity>> = dao.getAllIdCards()
    fun getIdCardsForInstitution(institutionId: String): Flow<List<IdCardRecordEntity>> =
        dao.getIdCardsForInstitution(institutionId)
    suspend fun getIdCardForPerson(personId: String): IdCardRecordEntity? = dao.getIdCardForPerson(personId)
    fun observeIdCardForPerson(personId: String): Flow<List<IdCardRecordEntity>> = dao.observeIdCardForPerson(personId)
    suspend fun saveIdCard(idCard: IdCardRecordEntity) = dao.insertIdCard(idCard)
    suspend fun inchargeVerifyIdCard(cardId: String, status: String, remarks: String) =
        dao.inchargeVerifyIdCard(cardId, status, remarks)
    suspend fun principalApproveIdCard(cardId: String, status: String, remarks: String, approvedBy: String) =
        dao.principalApproveIdCard(cardId, status, remarks, approvedBy)

    // =========================================================================
    // 10. ADMISSIONS (Tenant-Aware)
    // =========================================================================
    val allAdmissionApplications: Flow<List<AdmissionApplicationEntity>> = dao.getAllAdmissionApplications()
    fun getAdmissionApplicationsForInstitution(institutionId: String): Flow<List<AdmissionApplicationEntity>> =
        dao.getAdmissionApplicationsForInstitution(institutionId)
    suspend fun insertAdmissionApplication(app: AdmissionApplicationEntity) = dao.insertAdmissionApplication(app)
    suspend fun updateAdmissionApplication(app: AdmissionApplicationEntity) = dao.updateAdmissionApplication(app)

    // =========================================================================
    // 11. TIMETABLE
    // =========================================================================
    val allTimetableSlots: Flow<List<TimetableSlotEntity>> = dao.getAllTimetableSlots()
    fun getTimetableForClass(classSection: String): Flow<List<TimetableSlotEntity>> =
        dao.getTimetableForClass(classSection)
    fun getTimetableForTeacher(teacherId: String): Flow<List<TimetableSlotEntity>> =
        dao.getTimetableForTeacher(teacherId)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity) = dao.insertTimetableSlot(slot)

    // =========================================================================
    // 12. CIRCULARS & NOTICES
    // =========================================================================
    val allNotices: Flow<List<NoticeMeetingEntity>> = dao.getAllNotices()
    val allCirculars: Flow<List<CircularEntity>> = dao.getAllCirculars()
    fun getCircularsForRole(role: String): Flow<List<CircularEntity>> = dao.getCircularsForRole(role)
    suspend fun insertNotice(notice: NoticeMeetingEntity) = dao.insertNotice(notice)
    suspend fun insertCircular(circular: CircularEntity) = dao.insertCircular(circular)

    // =========================================================================
    // 13. SCHOOL DIARY & INSTRUCTIONS
    // =========================================================================
    val allDiaryEntries: Flow<List<SchoolDiaryEntity>> = dao.getAllDiaryEntries()
    fun getDiaryForClass(classSection: String): Flow<List<SchoolDiaryEntity>> = dao.getDiaryEntriesForClass(classSection)
    suspend fun insertDiaryEntry(entry: SchoolDiaryEntity) = dao.insertDiaryEntry(entry)
    val allInstructions: Flow<List<SchoolInstructionEntity>> = dao.getAllInstructions()

    // =========================================================================
    // 14. INSTITUTIONAL BILLS & GOVERNMENT COMPLIANCE
    // =========================================================================
    val allBills: Flow<List<InstitutionalBillEntity>> = dao.getAllBills()
    suspend fun insertBill(bill: InstitutionalBillEntity) = dao.insertBill(bill)
    suspend fun updateBill(bill: InstitutionalBillEntity) = dao.updateBill(bill)

    val allComplianceDocuments: Flow<List<GovernmentComplianceEntity>> = dao.getAllComplianceDocuments()
    suspend fun insertComplianceDocument(doc: GovernmentComplianceEntity) = dao.insertComplianceDocument(doc)

    // =========================================================================
    // 15. OFFLINE SYNCHRONIZATION QUEUE
    // =========================================================================
    val pendingSyncQueue: Flow<List<SyncQueueEntity>> = dao.getPendingSyncQueue()
    val pendingSyncCount: Flow<Int> = dao.getPendingSyncCount()
    suspend fun enqueueSyncItem(item: SyncQueueEntity) = dao.insertSyncQueue(item)
    suspend fun updateSyncItem(item: SyncQueueEntity) = dao.updateSyncQueue(item)
    suspend fun removeSyncItem(syncId: Long) = dao.deleteSyncQueueItem(syncId)

    // =========================================================================
    // 16. INCHARGE EDIT REQUESTS & AUDIT LOGS
    // =========================================================================
    val allInchargeRequests: Flow<List<InchargeEditRequestEntity>> = dao.getAllInchargeRequests()
    suspend fun insertInchargeRequest(req: InchargeEditRequestEntity) = dao.insertInchargeRequest(req)
    suspend fun authenticateInchargeRequest(requestId: String, status: String, remarks: String) =
        dao.authenticateInchargeRequest(requestId, status, remarks, System.currentTimeMillis())

    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    suspend fun insertAuditLog(log: AuditLogEntity) = dao.insertAuditLog(log)

    suspend fun approveAndApplyStudentInchargeEdit(
        requestId: String,
        remarks: String,
        timestamp: Long,
        updatedStudent: StudentEntity
    ) = dao.approveAndApplyStudentInchargeEdit(requestId, remarks, timestamp, updatedStudent)

    // =========================================================================
    // 17. INSTITUTION REGISTRATION LOOKUP & SECURE AUTHENTICATION
    // =========================================================================
    val allInstitutions: Flow<List<InstitutionEntity>> = dao.getAllInstitutionsFlow()
    suspend fun getAllInstitutionsList(): List<InstitutionEntity> = dao.getAllInstitutions()
    suspend fun getInstitutionByCode(code: String): InstitutionEntity? {
        val clean = code.trim()
        val found = dao.getInstitutionByCode(clean)
        if (found != null) return found
        // If searching for default test school and not found, auto-seed default institution
        if (clean == "3477" || clean.equals("STXAVIER", ignoreCase = true) || clean.equals("SXA", ignoreCase = true)) {
            val defaultInst = InstitutionEntity(
                institutionId = "INST-SXA-2026",
                registrationCode = "3477",
                name = "St. Xavier's International Academy",
                motto = "Excellence in Truth & Service",
                affiliationCode = "CBSE Affil: 1130492 | ISO 9001:2026",
                address = "Campus Road, Civil Lines, Sector 5",
                cityCampus = "Trimurtinagar, Ahmednagar & Sector 5",
                contactPhone = "+91 98111 22334",
                email = "office@stxaviers.edu",
                website = "https://stxaviers.edu",
                currentAcademicYear = "2026-2027",
                availableBatches = "2026-27,2025-26,2024-25",
                principalName = "Prof. Robert Vance, Ph.D.",
                themeColorHex = "#0B2545",
                logoAssetPath = "ic_school_crest",
                establishedYear = 1984,
                isSetupCompleted = true
            )
            dao.insertInstitution(defaultInst)
            return defaultInst
        }
        return null
    }

    suspend fun authenticate(
        institutionId: String,
        batch: String,
        username: String,
        password: String
    ): com.example.data.model.AuthResult = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim()
        val cleanPassword = password.trim()

        if (cleanUsername.isBlank()) {
            return@withContext com.example.data.model.AuthResult.Error("Please enter your registered Username or Email.")
        }
        if (cleanPassword.isBlank()) {
            return@withContext com.example.data.model.AuthResult.Error("Please enter your password.")
        }

        var institution = dao.getInstitutionByCode(institutionId) ?: dao.getInstitution()
        if (institution == null) {
            val defaultInst = InstitutionEntity(
                institutionId = "INST-SXA-2026",
                registrationCode = "3477",
                name = "St. Xavier's International Academy",
                motto = "Excellence in Truth & Service",
                affiliationCode = "CBSE Affil: 1130492 | ISO 9001:2026",
                address = "Campus Road, Civil Lines, Sector 5",
                cityCampus = "Trimurtinagar, Ahmednagar & Sector 5",
                contactPhone = "+91 98111 22334",
                email = "office@stxaviers.edu",
                website = "https://stxaviers.edu",
                currentAcademicYear = "2026-2027",
                availableBatches = "2026-27,2025-26,2024-25",
                principalName = "Prof. Robert Vance, Ph.D.",
                themeColorHex = "#0B2545",
                logoAssetPath = "ic_school_crest",
                establishedYear = 1984,
                isSetupCompleted = true
            )
            dao.insertInstitution(defaultInst)
            institution = defaultInst
        }

        val targetEmail = if (cleanUsername.contains("@")) cleanUsername else {
            val localUser = dao.getUserByUsernameOrPhone(cleanUsername)
            localUser?.email ?: "$cleanUsername@stxaviers.edu"
        }

        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull()
        val isLiveFirebase = runCatching {
            val app = com.google.firebase.FirebaseApp.getInstance()
            val key = app.options.apiKey
            key.isNotBlank() && !key.startsWith("AIzaSyMock")
        }.getOrDefault(false)

        val fbUser = if (isLiveFirebase && auth != null) {
            try {
                val authResult = auth.signInWithEmailAndPassword(targetEmail, cleanPassword).await()
                authResult.user ?: throw IllegalStateException("Firebase user was null after authentication.")
            } catch (e: Exception) {
                val isFirebaseEnvIssue = e.message?.contains("API key not valid", ignoreCase = true) == true ||
                    e.message?.contains("API_KEY_INVALID", ignoreCase = true) == true ||
                    e.message?.contains("Firebase App is not initialized", ignoreCase = true) == true ||
                    e.message?.contains("no api", ignoreCase = true) == true

                if (!isFirebaseEnvIssue) {
                    dao.insertAuditLog(
                        AuditLogEntity(
                            institutionId = institution.institutionId,
                            action = "AUTH_FAILED_FIREBASE",
                            entityType = "UserAuth",
                            entityId = targetEmail,
                            actorUserId = targetEmail,
                            actorName = cleanUsername,
                            actorRole = "ANONYMOUS",
                            previousState = null,
                            newState = "FAILURE",
                            remarks = "Firebase authentication failed: ${e.message}"
                        )
                    )
                    return@withContext com.example.data.model.AuthResult.Error(e.localizedMessage ?: "Invalid Firebase credentials.")
                }
                null
            }
        } else null

        if (fbUser == null) {
            // Offline institutional authentication fallback
            val localUser = dao.getUserByCredentials(targetEmail, institution.institutionId)
                ?: dao.getUserByUsernameOrPhone(cleanUsername)

            if (localUser != null) {
                if (localUser.isAccountDisabled) {
                    return@withContext com.example.data.model.AuthResult.Error("Access Denied: Account is disabled.")
                }
                val isPasswordValid = if (localUser.passwordHash.isNotBlank()) {
                    com.example.security.SecurityUtils.verifyPassword(cleanPassword, localUser.passwordSalt, localUser.passwordHash)
                } else {
                    cleanPassword == "Password@123"
                }
                if (!isPasswordValid) {
                    return@withContext com.example.data.model.AuthResult.Error("Invalid credentials. Please verify your password.")
                }
                val parsedRole = com.example.data.model.parseUserRole(localUser.role)
                val token = "INST-OFFLINE-SEC-${java.util.UUID.randomUUID()}"
                val session = com.example.data.model.AuthSession(
                    token = token,
                    user = localUser,
                    role = parsedRole,
                    institution = institution,
                    academicBatch = batch.ifBlank { institution.currentAcademicYear }
                )
                return@withContext com.example.data.model.AuthResult.Success(session)
            }
            return@withContext com.example.data.model.AuthResult.Error("Account not found for '$targetEmail'.")
        }

        val tokenResult = try {
            fbUser.getIdToken(false).await()
        } catch (e: Exception) {
            return@withContext com.example.data.model.AuthResult.Error("Failed to obtain authoritative Firebase ID token.")
        }

        val idToken = tokenResult.token ?: return@withContext com.example.data.model.AuthResult.Error("Empty Firebase ID token.")
        val roleClaim = tokenResult.claims["role"] as? String
        val parsedRole = if (!roleClaim.isNullOrBlank()) {
            com.example.data.model.parseUserRole(roleClaim)
        } else {
            com.example.data.model.UserRole.STUDENT
        }

        var user = dao.getUserByCredentials(targetEmail, institution.institutionId)
            ?: dao.getUserByUsernameOrPhone(cleanUsername)

        if (user == null) {
            user = UserEntity(
                id = fbUser.uid,
                institutionId = institution.institutionId,
                username = cleanUsername.substringBefore("@"),
                email = targetEmail,
                displayName = fbUser.displayName ?: cleanUsername,
                role = parsedRole.name,
                password = "",
                passwordSalt = "",
                passwordHash = ""
            )
            dao.insertUser(user)
        }

        val session = com.example.data.model.AuthSession(
            token = idToken,
            user = user,
            role = parsedRole,
            institution = institution,
            academicBatch = batch.ifBlank { institution.currentAcademicYear }
        )

        dao.insertAuditLog(
            AuditLogEntity(
                institutionId = institution.institutionId,
                action = "USER_LOGIN_SUCCESS",
                entityType = "SessionRegistry",
                entityId = user.id,
                actorUserId = user.id,
                actorName = user.displayName,
                actorRole = user.role,
                previousState = "ANONYMOUS",
                newState = "AUTHENTICATED",
                remarks = "User '${user.username}' authenticated with authoritative Firebase ID token as ${parsedRole.label}",
                securityVerificationHash = com.example.security.SecurityUtils.generateAuditSignature(
                    user.id, institution.institutionId, "LOGIN_SUCCESS", "SessionRegistry", idToken, System.currentTimeMillis()
                )
            )
        )

        return@withContext com.example.data.model.AuthResult.Success(session)
    }

    suspend fun resetUserPassword(
        usernameOrPhone: String,
        newPassword: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val clean = usernameOrPhone.trim()
        val user = dao.getUserByUsernameOrPhone(clean)
            ?: return@withContext Result.failure(Exception("No registered account found matching '$clean'."))

        val newHash = com.example.security.SecurityUtils.hashPassword(newPassword, user.passwordSalt)
        dao.updateUserPassword(user.id, newPassword)
        dao.insertAuditLog(
            AuditLogEntity(
                institutionId = user.institutionId,
                action = "PASSWORD_RESET_EXECUTED",
                entityType = "UserCredentials",
                entityId = user.id,
                actorUserId = user.id,
                actorName = user.displayName,
                actorRole = user.role,
                previousState = "MASKED",
                newState = "UPDATED",
                remarks = "Password reset successfully completed for user '${user.username}'",
                securityVerificationHash = com.example.security.SecurityUtils.generateAuditSignature(
                    user.id, user.institutionId, "PWD_RESET", "UserCredentials", user.id, System.currentTimeMillis()
                )
            )
        )
        return@withContext Result.success("Password successfully updated. You may now log in.")
    }

    // =========================================================================
    // 17. HOMEWORK & ASSIGNMENT MANAGEMENT
    // =========================================================================
    fun getHomeworkForClass(classSection: String, institutionId: String = "INST-SXA-2026"): Flow<List<HomeworkEntity>> =
        dao.getHomeworkForClass(classSection, institutionId)

    fun getHomeworkForInstitution(institutionId: String = "INST-SXA-2026"): Flow<List<HomeworkEntity>> =
        dao.getHomeworkForInstitution(institutionId)

    suspend fun insertHomework(homework: HomeworkEntity) = dao.insertHomework(homework)

    suspend fun deleteHomework(homeworkId: String) = dao.deleteHomework(homeworkId)

    fun getSubmissionsForHomework(homeworkId: String): Flow<List<HomeworkSubmissionEntity>> =
        dao.getSubmissionsForHomework(homeworkId)

    fun getSubmissionsForStudent(studentId: String): Flow<List<HomeworkSubmissionEntity>> =
        dao.getSubmissionsForStudent(studentId)

    suspend fun insertHomeworkSubmission(submission: HomeworkSubmissionEntity) =
        dao.insertHomeworkSubmission(submission)

    suspend fun gradeHomeworkSubmission(
        submissionId: String,
        status: String,
        marks: Int,
        feedback: String,
        auditLog: AuditLogEntity
    ) = dao.gradeHomeworkWithAudit(submissionId, status, marks, feedback, auditLog)

    // =========================================================================
    // 18. STUDENT & STAFF LEAVE REQUESTS
    // =========================================================================
    fun getLeaveRequestsForInstitution(institutionId: String = "INST-SXA-2026"): Flow<List<StudentLeaveRequestEntity>> =
        dao.getLeaveRequestsForInstitution(institutionId)

    fun getLeaveRequestsForStudent(studentId: String): Flow<List<StudentLeaveRequestEntity>> =
        dao.getLeaveRequestsForStudent(studentId)

    fun getLeaveRequestsForClass(classSection: String): Flow<List<StudentLeaveRequestEntity>> =
        dao.getLeaveRequestsForClass(classSection)

    suspend fun submitLeaveRequest(request: StudentLeaveRequestEntity) =
        dao.insertLeaveRequest(request)

    suspend fun reviewLeaveRequest(
        leaveId: String,
        status: String,
        reviewer: String,
        remarks: String,
        auditLog: AuditLogEntity
    ) = dao.reviewLeaveRequestWithAudit(leaveId, status, reviewer, remarks, auditLog)

    // =========================================================================
    // 19. CONFIGURABLE FEE STRUCTURES & TRANSACTIONS
    // =========================================================================
    fun getFeeStructures(institutionId: String = "INST-SXA-2026"): Flow<List<FeeStructureEntity>> =
        dao.getFeeStructures(institutionId)

    suspend fun insertFeeStructure(structure: FeeStructureEntity) =
        dao.insertFeeStructure(structure)

    fun getAllPaymentTransactions(): Flow<List<FeePaymentTransactionEntity>> =
        dao.getAllPaymentTransactions()

    fun getPaymentTransactions(institutionId: String = "INST-SXA-2026"): Flow<List<FeePaymentTransactionEntity>> =
        dao.getPaymentTransactions(institutionId)

    fun getPaymentTransactionsForStudent(studentId: String): Flow<List<FeePaymentTransactionEntity>> =
        dao.getPaymentTransactionsForStudent(studentId)

    suspend fun recordFeePaymentAtomic(
        feeId: String,
        transaction: FeePaymentTransactionEntity,
        paidPaise: Long,
        duePaise: Long,
        status: String,
        auditLog: AuditLogEntity
    ) = dao.processFeePaymentAtomic(feeId, transaction, paidPaise, duePaise, status, auditLog)

    suspend fun recordManualPaymentSafe(request: ManualFeePaymentRequest): Result<FeePaymentReceiptResult> =
        feeRepository.recordManualPayment(request)

    suspend fun createFeeAssessmentSafe(request: CreateFeeAssessmentRequest): Result<FeeRecordEntity> =
        feeRepository.createFeeAssessment(request)

    // =========================================================================
    // 20. PUSH NOTIFICATIONS
    // =========================================================================
    fun getNotificationsForUser(institutionId: String = "INST-SXA-2026", userId: String): Flow<List<PushNotificationEntity>> =
        dao.getNotificationsForUser(institutionId, userId)

    fun getUnreadNotificationCount(institutionId: String = "INST-SXA-2026", userId: String): Flow<Int> =
        dao.getUnreadNotificationCount(institutionId, userId)

    suspend fun insertNotification(notification: PushNotificationEntity) =
        dao.insertNotification(notification)

    suspend fun markNotificationAsRead(notificationId: String) =
        dao.markNotificationAsRead(notificationId)

    suspend fun markAllNotificationsAsRead(institutionId: String = "INST-SXA-2026") =
        dao.markAllNotificationsAsRead(institutionId)

    // =========================================================================
    // 21. AI AUDIT LOGS
    // =========================================================================
    fun getAiAuditLogs(institutionId: String = "INST-SXA-2026"): Flow<List<AiAuditLogEntity>> =
        dao.getAiAuditLogs(institutionId)

    suspend fun insertAiAuditLog(log: AiAuditLogEntity) = dao.insertAiAuditLog(log)

    // =========================================================================
    // 22. SUBJECTS MASTER
    // =========================================================================
    fun getSubjectsForInstitution(institutionId: String = "INST-SXA-2026"): Flow<List<SubjectEntity>> =
        dao.getSubjectsForInstitution(institutionId)

    fun getSubjectsForClass(classSection: String, institutionId: String = "INST-SXA-2026"): Flow<List<SubjectEntity>> =
        dao.getSubjectsForClass(classSection, institutionId)

    suspend fun insertSubject(subject: SubjectEntity) = dao.insertSubject(subject)

    // =========================================================================
    // 23. PARENT WARD LINKS
    // =========================================================================
    fun getWardsForParent(parentUserId: String, institutionId: String = "INST-SXA-2026"): Flow<List<ParentWardLinkEntity>> =
        dao.getWardsForParent(parentUserId, institutionId)

    suspend fun getWardsForParentList(parentUserId: String, institutionId: String = "INST-SXA-2026"): List<ParentWardLinkEntity> =
        dao.getWardsForParentList(parentUserId, institutionId)

    suspend fun insertParentWardLink(link: ParentWardLinkEntity) =
        dao.insertParentWardLink(link)

    // =========================================================================
    // 24. DEMO USER & DATA GUARANTEE SEEDER
    // =========================================================================
    suspend fun ensureDemoUsersAndDataSeeded() = withContext(Dispatchers.IO) {
        val defaultTenant = "INST-SXA-2026"

        // 1. Ensure St. Xavier's institution is in Room
        if (dao.getInstitution() == null) {
            dao.insertInstitution(
                InstitutionEntity(
                    institutionId = defaultTenant,
                    registrationCode = "3477",
                    name = "St. Xavier's International Academy",
                    motto = "Excellence in Truth & Service",
                    affiliationCode = "CBSE Affil: 1130492 | ISO 9001:2026",
                    address = "Campus Road, Civil Lines, Sector 5",
                    cityCampus = "Trimurtinagar, Ahmednagar & Sector 5",
                    contactPhone = "+91 98111 22334",
                    email = "office@stxaviers.edu",
                    website = "https://stxaviers.edu",
                    currentAcademicYear = "2026-2027",
                    availableBatches = "2026-27,2025-26,2024-25",
                    principalName = "Prof. Robert Vance, Ph.D.",
                    themeColorHex = "#0B2545",
                    logoAssetPath = "ic_school_crest",
                    establishedYear = 1984,
                    isSetupCompleted = true
                )
            )
        }

        // 2. Ensure Academic Class 10-A exists
        val classes = dao.getAllClassesList()
        if (classes.none { it.className.contains("10") && it.section == "A" }) {
            dao.insertAcademicClass(
                AcademicClassEntity(
                    classId = "CLS-GRADE10-A",
                    institutionId = defaultTenant,
                    className = "Grade 10",
                    section = "A",
                    classTeacherId = "FAC-TCH-02",
                    classTeacherName = "Mrs. Sunita Kapoor",
                    roomNumber = "Room 304 - Secondary Wing",
                    studentCount = 30,
                    academicYear = "2026-2027"
                )
            )
        }

        // 3. Ensure Staff members exist
        val staff = dao.getAllStaffList()
        if (staff.none { it.id == "FAC-PRIN-01" }) {
            dao.insertStaff(
                StaffEntity(
                    id = "FAC-PRIN-01",
                    institutionId = defaultTenant,
                    name = "Prof. Robert Vance, Ph.D.",
                    roleType = "PRINCIPAL",
                    designation = "Principal & Executive Director",
                    department = "Executive Directorate",
                    phone = "9811122334",
                    email = "principal@stxaviers.edu",
                    qualification = "Ph.D. Educational Leadership, M.Sc.",
                    experienceYears = 24,
                    govtRegNumber = "SXA-EXEC-001",
                    isPreAuthorizedByPrincipal = true,
                    isOtpVerified = true,
                    authPin = "9876",
                    isPresentToday = true,
                    checkInTime = "07:45 AM",
                    avatarColorHex = "#0B2545",
                    isClassIncharge = false
                )
            )
        }
        if (staff.none { it.id == "FAC-TCH-02" }) {
            dao.insertStaff(
                StaffEntity(
                    id = "FAC-TCH-02",
                    institutionId = defaultTenant,
                    name = "Mrs. Sunita Kapoor",
                    roleType = "INCHARGE",
                    designation = "Class In-Charge 10-A & Mathematics Faculty",
                    department = "Mathematical Sciences",
                    phone = "9876543211",
                    email = "sunita.kapoor@stxaviers.edu",
                    qualification = "M.Sc. Mathematics, B.Ed.",
                    experienceYears = 12,
                    govtRegNumber = "SXA-FAC-102",
                    isPreAuthorizedByPrincipal = true,
                    isOtpVerified = true,
                    authPin = "1234",
                    isPresentToday = true,
                    checkInTime = "08:00 AM",
                    avatarColorHex = "#E65100",
                    isClassIncharge = true,
                    inchargeClass = "10-A",
                    assignedSubjects = "Mathematics, Advanced Calculus"
                )
            )
        }
        if (staff.none { it.id == "FAC-TCH-01" }) {
            dao.insertStaff(
                StaffEntity(
                    id = "FAC-TCH-01",
                    institutionId = defaultTenant,
                    name = "Dr. Meenakshi Sundaram",
                    roleType = "TEACHING",
                    designation = "Senior Faculty in Physics & Science",
                    department = "Science Division",
                    phone = "9876543210",
                    email = "m.sundaram@stxaviers.edu",
                    qualification = "M.Sc., B.Ed., Ph.D. Physics",
                    experienceYears = 14,
                    govtRegNumber = "SXA-FAC-101",
                    isPreAuthorizedByPrincipal = true,
                    isOtpVerified = true,
                    authPin = "1234",
                    isPresentToday = true,
                    checkInTime = "07:55 AM",
                    avatarColorHex = "#15803D",
                    isClassIncharge = false,
                    assignedSubjects = "Physics, General Science"
                )
            )
        }

        // 4. Ensure Students exist
        val students = dao.getAllStudentsList()
        if (students.none { it.id == "ADM-2026-001" }) {
            dao.insertStudent(
                StudentEntity(
                    id = "ADM-2026-001",
                    institutionId = defaultTenant,
                    admissionNumber = "ADM-2026-001",
                    name = "Aarav Sharma",
                    grade = "10",
                    section = "A",
                    rollNumber = 1,
                    gender = "Male",
                    dateOfBirth = "2011-03-15",
                    guardianName = "Vikram Sharma",
                    guardianPhone = "+91 91234 56780",
                    studentPhone = "9123456780",
                    email = "aarav.sharma@stxaviers.edu",
                    bloodGroup = "O+",
                    attendancePercentage = 94.5f,
                    feeDue = 0.0,
                    feePaid = 65000.0,
                    avatarColorHex = "#0B2545",
                    address = "42 North Avenue, Estate Square"
                )
            )
        }
        if (students.none { it.id == "ADM-2026-002" }) {
            dao.insertStudent(
                StudentEntity(
                    id = "ADM-2026-002",
                    institutionId = defaultTenant,
                    admissionNumber = "ADM-2026-002",
                    name = "Ananya Iyer",
                    grade = "10",
                    section = "A",
                    rollNumber = 2,
                    gender = "Female",
                    dateOfBirth = "2011-06-21",
                    guardianName = "Suresh Iyer",
                    guardianPhone = "+91 91234 56781",
                    studentPhone = "9123456781",
                    email = "ananya.iyer@stxaviers.edu",
                    bloodGroup = "B+",
                    attendancePercentage = 97.2f,
                    feeDue = 0.0,
                    feePaid = 65000.0,
                    avatarColorHex = "#15803D"
                )
            )
        }

        // 5. Ensure Demo Users exist with PBKDF2 hash & plaintext fallback
        val demoUsers = listOf(
            UserEntity(
                id = "FAC-PRIN-01",
                institutionId = defaultTenant,
                username = "principal",
                email = "principal@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_prin_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_prin_2026"),
                role = "PRINCIPAL",
                displayName = "Prof. Robert Vance, Ph.D. (Principal)",
                phoneNumber = "9811122334"
            ),
            UserEntity(
                id = "USR-ADMIN-01",
                institutionId = defaultTenant,
                username = "admin",
                email = "admin@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_adm_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_adm_2026"),
                role = "ADMIN",
                displayName = "System Administrator",
                phoneNumber = "9811122330"
            ),
            UserEntity(
                id = "FAC-TCH-02",
                institutionId = defaultTenant,
                username = "incharge_10a",
                email = "sunita.kapoor@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_inch_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_inch_2026"),
                role = "INCHARGE",
                displayName = "Mrs. Sunita Kapoor (Class Incharge 10-A)",
                phoneNumber = "9876543211"
            ),
            UserEntity(
                id = "FAC-TCH-01",
                institutionId = defaultTenant,
                username = "faculty_meenakshi",
                email = "m.sundaram@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_tch_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_tch_2026"),
                role = "TEACHER",
                displayName = "Dr. Meenakshi Sundaram (Physics Faculty)",
                phoneNumber = "9876543210"
            ),
            UserEntity(
                id = "ADM-2026-001",
                institutionId = defaultTenant,
                username = "aarav_sharma",
                email = "aarav.sharma@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_stu_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_stu_2026"),
                role = "STUDENT",
                displayName = "Aarav Sharma (Grade 10-A)",
                phoneNumber = "9123456780"
            ),
            UserEntity(
                id = "ADM-2026-002",
                institutionId = defaultTenant,
                username = "ananya_iyer",
                email = "ananya.iyer@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_ana_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_ana_2026"),
                role = "STUDENT",
                displayName = "Ananya Iyer (Grade 10-A)",
                phoneNumber = "9123456781"
            ),
            UserEntity(
                id = "USR-PAR-01",
                institutionId = defaultTenant,
                username = "vikram_sharma",
                email = "vikram.sharma@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_par_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_par_2026"),
                role = "PARENT",
                displayName = "Vikram Sharma (Guardian of Aarav)",
                phoneNumber = "+91 91234 56780"
            ),
            UserEntity(
                id = "USR-ACC-01",
                institutionId = defaultTenant,
                username = "accountant_mehta",
                email = "accountant@stxaviers.edu",
                password = "Password@123",
                passwordSalt = "sxa_salt_acc_2026",
                passwordHash = com.example.security.SecurityUtils.hashPasswordPBKDF2("Password@123", "sxa_salt_acc_2026"),
                role = "ACCOUNTANT",
                displayName = "Rajesh Mehta (Senior Accounts Officer)",
                phoneNumber = "9811199887"
            )
        )

        for (u in demoUsers) {
            val existing = dao.getUserById(u.id) ?: dao.getUserByUsernameOrPhone(u.username)
            if (existing == null) {
                dao.insertUser(u)
            }
        }

        // 6. Ensure Timetable Slots for 10-A
        val initialTimetable = listOf(
            TimetableSlotEntity(
                slotId = "TT-10A-MON-1",
                institutionId = defaultTenant,
                classSection = "10-A",
                dayOfWeek = "Monday",
                periodNumber = 1,
                timeRange = "08:30 - 09:15 AM",
                subject = "Mathematics",
                teacherName = "Mrs. Sunita Kapoor",
                teacherId = "FAC-TCH-02",
                roomNumber = "Room 304"
            ),
            TimetableSlotEntity(
                slotId = "TT-10A-MON-2",
                institutionId = defaultTenant,
                classSection = "10-A",
                dayOfWeek = "Monday",
                periodNumber = 2,
                timeRange = "09:15 - 10:00 AM",
                subject = "Physics (Science)",
                teacherName = "Dr. Meenakshi Sundaram",
                teacherId = "FAC-TCH-01",
                roomNumber = "Physics Lab A"
            ),
            TimetableSlotEntity(
                slotId = "TT-10A-MON-3",
                institutionId = defaultTenant,
                classSection = "10-A",
                dayOfWeek = "Monday",
                periodNumber = 3,
                timeRange = "10:00 - 10:45 AM",
                subject = "English Literature",
                teacherName = "David K. Reynolds",
                teacherId = "FAC-TCH-03",
                roomNumber = "Room 304"
            ),
            TimetableSlotEntity(
                slotId = "TT-10A-MON-4",
                institutionId = defaultTenant,
                classSection = "10-A",
                dayOfWeek = "Monday",
                periodNumber = 4,
                timeRange = "11:15 - 12:00 PM",
                subject = "Chemistry",
                teacherName = "Dr. Meenakshi Sundaram",
                teacherId = "FAC-TCH-01",
                roomNumber = "Chemistry Lab"
            )
        )
        dao.insertTimetableSlots(initialTimetable)

        // 7. Ensure Student Leave Requests
        val initialLeaves = listOf(
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-101",
                institutionId = defaultTenant,
                studentId = "ADM-2026-001",
                studentName = "Aarav Sharma",
                classSection = "10-A",
                startDate = "2026-10-12",
                endDate = "2026-10-13",
                reason = "Inter-School Science Olympiad participation in New Delhi.",
                status = "PENDING",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 7200000L
            ),
            StudentLeaveRequestEntity(
                leaveId = "LEV-2026-102",
                institutionId = defaultTenant,
                studentId = "ADM-2026-002",
                studentName = "Ananya Iyer",
                classSection = "10-A",
                startDate = "2026-10-14",
                endDate = "2026-10-15",
                reason = "Medical dental appointment and recovery.",
                status = "APPROVED",
                appliedBy = "PARENT",
                appliedTimestamp = System.currentTimeMillis() - 86400000L,
                reviewedBy = "Mrs. Sunita Kapoor (Class Incharge)",
                reviewerRemarks = "Approved on medical certificate submission.",
                reviewedTimestamp = System.currentTimeMillis() - 43200000L
            )
        )
        dao.insertLeaveRequests(initialLeaves)
    }
}
