package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AdmissionApplicationEntity
import com.example.data.local.entities.AiAuditLogEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchoolRepository @Inject constructor(val dao: SchoolDao) {

    val userRepository: UserRepository = UserRepositoryImpl(dao)
    val attendanceRepository: AttendanceRepository = AttendanceRepositoryImpl(dao)

    // =========================================================================
    // 1. INSTITUTION MASTER PROFILE
    // =========================================================================
    val institution: Flow<InstitutionEntity?> = dao.getInstitutionFlow()
    suspend fun getInstitution(): InstitutionEntity? = dao.getInstitution()
    suspend fun updateInstitution(institution: InstitutionEntity) = dao.updateInstitution(institution)
    suspend fun insertInstitution(institution: InstitutionEntity) = dao.insertInstitution(institution)

    // =========================================================================
    // 2. ACADEMIC CLASSES
    // =========================================================================
    val allClasses: Flow<List<AcademicClassEntity>> = dao.getAllClasses()
    suspend fun insertClass(academicClass: AcademicClassEntity) = dao.insertAcademicClass(academicClass)
    suspend fun updateClass(academicClass: AcademicClassEntity) = dao.updateAcademicClass(academicClass)

    // =========================================================================
    // 3. STAFF MANAGEMENT (Unique Mobile Number Rule Enforced)
    // =========================================================================
    val allStaff: Flow<List<StaffEntity>> = dao.getAllStaff()
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
    // 4. STUDENTS MANAGEMENT (Privacy & Isolation)
    // =========================================================================
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudents()
    suspend fun getAllStudentsList(): List<StudentEntity> = dao.getAllStudentsList()
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
    // 5. ATTENDANCE & REGISTER SCAN BATCHES
    // =========================================================================
    val allAttendanceRecords: Flow<List<AttendanceRecordEntity>> = dao.getAllAttendanceRecords()
    val allRegisterBatches: Flow<List<RegisterScanBatchEntity>> = dao.getAllRegisterBatches()
    fun getAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForPerson(personId)
    fun getAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForClass(classSection)
    suspend fun commitRegisterScan(batch: RegisterScanBatchEntity, records: List<AttendanceRecordEntity>) {
        dao.insertRegisterBatch(batch)
        dao.insertAttendanceRecords(records)
    }
    suspend fun inchargeVerifyBatch(batchId: String) = dao.verifyBatchByIncharge(batchId)
    suspend fun principalApproveBatch(batchId: String) = dao.approveBatchByPrincipal(batchId)

    // =========================================================================
    // 6. EXAMINATIONS & REPORT CARDS
    // =========================================================================
    val allExamRecords: Flow<List<ExamRecordEntity>> = dao.getAllExamRecords()
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
    // 7. FEE MANAGEMENT & RECEIPTS
    // =========================================================================
    val allFeeRecords: Flow<List<FeeRecordEntity>> = dao.getAllFeeRecords()
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
    // 8. CENTRALIZED DOCUMENTS & CERTIFICATES
    // =========================================================================
    val allCertificates: Flow<List<CertificateEntity>> = dao.getAllCertificates()
    fun getCertificatesForStudent(studentId: String): Flow<List<CertificateEntity>> =
        dao.getCertificatesForStudent(studentId)
    suspend fun getCertificateById(certId: String): CertificateEntity? = dao.getCertificateById(certId)
    suspend fun insertCertificate(certificate: CertificateEntity) = dao.insertCertificate(certificate)
    suspend fun inchargeVerifyCertificate(id: String, incharge: String, remarks: String) =
        dao.inchargeVerifyCertificate(id, "VERIFIED_BY_INCHARGE", incharge, remarks)
    suspend fun principalApproveCertificate(id: String, principal: String, remarks: String, token: String) =
        dao.principalApproveCertificate(id, "APPROVED_BY_PRINCIPAL", principal, remarks, true, token)

    // =========================================================================
    // 9. COMPUTERIZED DIGITAL ID CARDS
    // =========================================================================
    val allIdCards: Flow<List<IdCardRecordEntity>> = dao.getAllIdCards()
    suspend fun getIdCardForPerson(personId: String): IdCardRecordEntity? = dao.getIdCardForPerson(personId)
    fun observeIdCardForPerson(personId: String): Flow<List<IdCardRecordEntity>> = dao.observeIdCardForPerson(personId)
    suspend fun saveIdCard(idCard: IdCardRecordEntity) = dao.insertIdCard(idCard)
    suspend fun inchargeVerifyIdCard(cardId: String, status: String, remarks: String) =
        dao.inchargeVerifyIdCard(cardId, status, remarks)
    suspend fun principalApproveIdCard(cardId: String, status: String, remarks: String, approvedBy: String) =
        dao.principalApproveIdCard(cardId, status, remarks, approvedBy)

    // =========================================================================
    // 10. ADMISSIONS
    // =========================================================================
    val allAdmissionApplications: Flow<List<AdmissionApplicationEntity>> = dao.getAllAdmissionApplications()
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

        var user = dao.getUserByCredentials(cleanUsername, institution.institutionId)
            ?: dao.getUserByUsernameOrPhone(cleanUsername)

        // Fallback resolution for Default Testing Credentials
        if (user == null) {
            val testUser = getDefaultTestUser(cleanUsername, institution.institutionId)
            if (testUser != null) {
                dao.insertUser(testUser)
                user = testUser
            }
        }

        if (user == null) {
            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institution.institutionId,
                    action = "AUTH_FAILED_USER_NOT_FOUND",
                    entityType = "UserAuth",
                    entityId = cleanUsername,
                    actorUserId = cleanUsername,
                    actorName = cleanUsername,
                    actorRole = "ANONYMOUS",
                    previousState = null,
                    newState = "FAILURE",
                    remarks = "Failed login attempt for nonexistent user '$cleanUsername' at ${institution.name}"
                )
            )
            return@withContext com.example.data.model.AuthResult.Error("Invalid username or account not found at ${institution.name}.")
        }

        if (!user.isActive) {
            return@withContext com.example.data.model.AuthResult.Error("Account is inactive or suspended. Please contact school administration.")
        }

        val isPasswordCorrect = com.example.security.SecurityUtils.verifyPassword(
            rawPassword = cleanPassword,
            salt = user.passwordSalt,
            storedHash = user.passwordHash,
            fallbackPlain = user.password
        ) || (user.authPin == cleanPassword)

        if (!isPasswordCorrect) {
            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institution.institutionId,
                    action = "AUTH_FAILED_BAD_CREDENTIALS",
                    entityType = "UserAuth",
                    entityId = user.id,
                    actorUserId = user.id,
                    actorName = user.displayName,
                    actorRole = user.role,
                    previousState = null,
                    newState = "FAILURE",
                    remarks = "Failed password attempt for user '${user.username}' at ${institution.name}",
                    securityVerificationHash = com.example.security.SecurityUtils.generateAuditSignature(
                        user.id, institution.institutionId, "AUTH_FAILED", "UserAuth", user.id, System.currentTimeMillis()
                    )
                )
            )
            return@withContext com.example.data.model.AuthResult.Error("Incorrect password. Please verify your credentials or use Forgot Password.")
        }

        val parsedRole = com.example.data.model.parseUserRole(user.role)

        val sessionToken = com.example.security.SecurityUtils.generateSessionToken(user.id, institution.institutionId)
        val session = com.example.data.model.AuthSession(
            token = sessionToken,
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
                remarks = "User '${user.username}' logged in successfully as ${parsedRole.label} for batch $batch",
                securityVerificationHash = com.example.security.SecurityUtils.generateAuditSignature(
                    user.id, institution.institutionId, "LOGIN_SUCCESS", "SessionRegistry", sessionToken, System.currentTimeMillis()
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

    private fun getDefaultTestUser(query: String, institutionId: String): UserEntity? {
        val q = query.trim().lowercase()
        return when {
            q == "admin" || q == "super_admin" || q == "administrator" -> {
                UserEntity(
                    id = "USR-ADM-01",
                    institutionId = institutionId,
                    username = "admin",
                    email = "admin@stxaviers.edu",
                    password = "Password@123",
                    role = "ADMIN",
                    displayName = "Dr. Elena Vance (System Administrator)",
                    phoneNumber = "9800000000",
                    authPin = "9876",
                    isOtpVerified = true,
                    permissions = "MANAGE_INSTITUTION,CONFIGURE_ACADEMIC_YEAR,MANAGE_CLASSES,MANAGE_STAFF_DBMS,ASSIGN_TEACHERS,VIEW_ROSTER_ALL,VIEW_AUDIT_LOGS"
                )
            }
            q == "teacher" || q == "faculty" -> {
                UserEntity(
                    id = "USR-TCH-DEMO",
                    institutionId = institutionId,
                    username = "teacher",
                    email = "teacher@stxaviers.edu",
                    password = "Password@123",
                    role = "TEACHER",
                    displayName = "Mrs. Sunita Kapoor (Senior Teacher)",
                    phoneNumber = "9876543212",
                    authPin = "1234",
                    isOtpVerified = true,
                    permissions = "VIEW_ROSTER_SECTION,MARK_ATTENDANCE_SECTION,SCAN_REGISTER_CAMERA,ENTER_MARKS_ASSIGNED_SUBJECT,WRITE_CLASS_DIARY"
                )
            }
            q == "student" || q == "scholar" -> {
                UserEntity(
                    id = "USR-STU-DEMO",
                    institutionId = institutionId,
                    username = "student",
                    email = "student@stxaviers.edu",
                    password = "Password@123",
                    role = "STUDENT",
                    displayName = "Aarav Sharma (Scholar Roll #1)",
                    phoneNumber = "9123456781",
                    authPin = "1234",
                    isOtpVerified = true,
                    permissions = "READ_DIARY,VIEW_TIMETABLE_ASSIGNED"
                )
            }
            q == "principal" || q == "fac-prin-01" || q == "usr-prin-01" -> {
                UserEntity(
                    id = "USR-PRIN-01",
                    institutionId = institutionId,
                    username = "principal",
                    email = "principal@stxaviers.edu",
                    password = "Password@123",
                    role = "PRINCIPAL",
                    displayName = "Prof. Robert Vance, Ph.D. (Principal)",
                    phoneNumber = "9811122334",
                    authPin = "9876",
                    isOtpVerified = true
                )
            }
            q == "incharge" || q == "incharge_10a" || q == "fac-tch-02" || q == "fac-math-02" || q == "usr-inch-01" -> {
                UserEntity(
                    id = "USR-INCH-01",
                    institutionId = institutionId,
                    username = "incharge_10a",
                    email = "sunita.kapoor@stxaviers.edu",
                    password = "Password@123",
                    role = "INCHARGE",
                    displayName = "Mrs. Sunita Kapoor (Class In-Charge 10-A)",
                    phoneNumber = "9876543211",
                    authPin = "1234",
                    isOtpVerified = true
                )
            }
            q == "teacher" || q == "faculty" || q == "faculty_meenakshi" || q == "fac-phy-03" || q == "fac-tch-01" || q == "usr-tch-01" -> {
                UserEntity(
                    id = "USR-TCH-01",
                    institutionId = institutionId,
                    username = "faculty_meenakshi",
                    email = "m.sundaram@stxaviers.edu",
                    password = "Password@123",
                    role = "TEACHING",
                    displayName = "Dr. Meenakshi Sundaram (Physics Faculty)",
                    phoneNumber = "9876543210",
                    authPin = "1234",
                    isOtpVerified = true
                )
            }
            q == "student" || q == "aarav" || q == "aarav_sharma" || q == "adm-2026-001" || q == "usr-stu-01" -> {
                UserEntity(
                    id = "USR-STU-01",
                    institutionId = institutionId,
                    username = "aarav_sharma",
                    email = "aarav.sharma@stxaviers.edu",
                    password = "Password@123",
                    role = "STUDENT",
                    displayName = "Aarav Sharma (Scholar Roll #1)",
                    phoneNumber = "9123456780",
                    authPin = "1234",
                    isOtpVerified = true
                )
            }
            q == "parent" || q == "vikram" || q == "vikram_sharma" || q == "par-2026-001" || q == "usr-par-01" -> {
                UserEntity(
                    id = "USR-PAR-01",
                    institutionId = institutionId,
                    username = "vikram_sharma",
                    email = "vikram.sharma@stxaviers.edu",
                    password = "Password@123",
                    role = "PARENT",
                    displayName = "Vikram Sharma (Parent of Aarav)",
                    phoneNumber = "9123456780",
                    authPin = "1234",
                    isOtpVerified = true
                )
            }
            q == "accountant" || q == "accountant_mehta" || q == "accounts" || q == "bursar" || q == "fac-acc-01" || q == "usr-acc-01" -> {
                UserEntity(
                    id = "USR-ACC-01",
                    institutionId = institutionId,
                    username = "accountant_mehta",
                    email = "accounts@stxaviers.edu",
                    password = "Password@123",
                    role = "ACCOUNTANT",
                    displayName = "Suresh Mehta (Accounts & Bursar)",
                    phoneNumber = "9844455667",
                    authPin = "1234",
                    isOtpVerified = true
                )
            }
            else -> null
        }
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
}
