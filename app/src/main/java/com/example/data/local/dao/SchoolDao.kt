package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AdmissionApplicationEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.CertificateEntity
import com.example.data.local.entities.CircularEntity
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.GovernmentComplianceEntity
import com.example.data.local.entities.IdCardRecordEntity
import com.example.data.local.entities.InchargeEditRequestEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.InstitutionalBillEntity
import com.example.data.local.entities.NoticeMeetingEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.local.entities.SchoolDiaryEntity
import com.example.data.local.entities.SchoolInstructionEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.TimetableSlotEntity
import com.example.data.local.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    // =========================================================================
    // 1. INSTITUTION MASTER PROFILE
    // =========================================================================
    @Query("SELECT * FROM institutions LIMIT 1")
    fun getInstitutionFlow(): Flow<InstitutionEntity?>

    @Query("SELECT * FROM institutions LIMIT 1")
    suspend fun getInstitution(): InstitutionEntity?

    @Query("SELECT * FROM institutions ORDER BY name ASC")
    fun getAllInstitutionsFlow(): Flow<List<InstitutionEntity>>

    @Query("SELECT * FROM institutions ORDER BY name ASC")
    suspend fun getAllInstitutions(): List<InstitutionEntity>

    @Query("SELECT * FROM institutions WHERE registrationCode = :code OR institutionId = :code OR affiliationCode LIKE '%' || :code || '%' OR LOWER(name) LIKE '%' || LOWER(:code) || '%' LIMIT 1")
    suspend fun getInstitutionByCode(code: String): InstitutionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitution(institution: InstitutionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitutions(institutions: List<InstitutionEntity>)

    @Update
    suspend fun updateInstitution(institution: InstitutionEntity)

    // =========================================================================
    // 2. ACADEMIC CLASSES & SECTIONS (Pre-fed Database)
    // =========================================================================
    @Query("SELECT * FROM academic_classes WHERE isArchived = 0 ORDER BY className ASC, section ASC")
    fun getAllClasses(): Flow<List<AcademicClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicClass(academicClass: AcademicClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicClasses(classes: List<AcademicClassEntity>)

    @Update
    suspend fun updateAcademicClass(academicClass: AcademicClassEntity)

    // =========================================================================
    // 3. STAFF MEMBERS (Fed & Managed by Principal)
    // =========================================================================
    @Query("SELECT * FROM staff_members ORDER BY roleType ASC, name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members WHERE roleType = :roleType ORDER BY name ASC")
    fun getStaffByRole(roleType: String): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '+91', '') = :cleanPhone LIMIT 1")
    suspend fun getStaffByCleanPhone(cleanPhone: String): StaffEntity?

    @Query("SELECT COUNT(*) FROM staff_members WHERE REPLACE(REPLACE(REPLACE(phone, ' ', ''), '-', ''), '+91', '') = :cleanPhone AND (:excludeId IS NULL OR id != :excludeId)")
    suspend fun countStaffWithPhone(cleanPhone: String, excludeId: String?): Int

    @Query("SELECT * FROM staff_members WHERE id = :id LIMIT 1")
    suspend fun getStaffById(id: String): StaffEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: StaffEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffList(staffList: List<StaffEntity>)

    @Update
    suspend fun updateStaff(staff: StaffEntity)

    @Query("DELETE FROM staff_members WHERE id = :id")
    suspend fun deleteStaff(id: String)

    @Query("UPDATE staff_members SET isOtpVerified = :verified WHERE id = :staffId")
    suspend fun updateStaffOtpVerified(staffId: String, verified: Boolean)

    @Query("UPDATE staff_members SET isPresentToday = :isPresent, checkInTime = :time WHERE id = :staffId")
    suspend fun updateStaffAttendance(staffId: String, isPresent: Boolean, time: String)

    @Query("UPDATE staff_members SET idCardStatus = :status, idCardPhotoUri = :photoUri, convertedDigitalIdNumber = :digitalId, idApprovedBy = :approvedBy, idApprovedDate = :approvedDate WHERE id = :staffId")
    suspend fun updateStaffIdCardStatus(
        staffId: String,
        status: String,
        photoUri: String?,
        digitalId: String?,
        approvedBy: String?,
        approvedDate: String?
    )

    // =========================================================================
    // 4. STUDENTS (Enrolled Scholar Data & Strict Isolation)
    // =========================================================================
    @Query("SELECT * FROM students ORDER BY grade ASC, rollNumber ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY grade ASC, rollNumber ASC")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE (admissionNumber = :admissionNo OR id = :admissionNo) AND (REPLACE(REPLACE(REPLACE(guardianPhone, ' ', ''), '-', ''), '+91', '') = :cleanPhone OR REPLACE(REPLACE(REPLACE(studentPhone, ' ', ''), '-', ''), '+91', '') = :cleanPhone) LIMIT 1")
    suspend fun getStudentByAdmissionAndCleanPhone(admissionNo: String, cleanPhone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE admissionNumber = :admissionNo OR id = :admissionNo LIMIT 1")
    suspend fun getStudentByAdmissionNumber(admissionNo: String): StudentEntity?

    @Query("SELECT * FROM students WHERE grade = :grade ORDER BY rollNumber ASC")
    fun getStudentsByGrade(grade: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE grade = :grade AND section = :section ORDER BY rollNumber ASC")
    fun getStudentsByClassSection(grade: String, section: String): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :id")
    suspend fun deleteStudent(id: String)

    @Query("UPDATE students SET attendancePercentage = :newPercentage WHERE id = :studentId")
    suspend fun updateStudentAttendancePercentage(studentId: String, newPercentage: Float)

    @Query("UPDATE students SET realIdPhotoUri = :photoUri, convertedDigitalId = :digitalId, digitalIdStatus = :status WHERE id = :studentId")
    suspend fun updateStudentIdCard(studentId: String, photoUri: String?, digitalId: String, status: String)

    @Query("UPDATE students SET grade = :newGrade, section = :newSection, promotionStatus = :status WHERE id = :studentId")
    suspend fun updateStudentPromotion(studentId: String, newGrade: String, newSection: String, status: String)

    // =========================================================================
    // 5. ATTENDANCE & ROLL-CALL REGISTERS
    // =========================================================================
    @Query("SELECT * FROM attendance ORDER BY dateTimestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY dateTimestamp DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE classSection = :classSection ORDER BY dateTimestamp DESC")
    fun getAttendanceForClassSection(classSection: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE classSection = :classSection AND date = :date")
    suspend fun getAttendanceForClassAndDate(classSection: String, date: String): List<AttendanceEntity>

    @Query("DELETE FROM attendance WHERE classSection = :classSection AND date = :date")
    suspend fun deleteAttendanceForClassAndDate(classSection: String, date: String)

    @Query("SELECT * FROM attendance WHERE dateTimestamp BETWEEN :startEpochMs AND :endEpochMs ORDER BY dateTimestamp DESC")
    fun getAttendanceInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(list: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance_records ORDER BY recordId DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE personId = :personId ORDER BY recordId DESC")
    fun getAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classSection = :classSection ORDER BY recordId DESC")
    fun getAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecord(record: AttendanceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)

    @Query("SELECT * FROM register_scan_batches ORDER BY timestamp DESC")
    fun getAllRegisterBatches(): Flow<List<RegisterScanBatchEntity>>

    @Query("SELECT * FROM register_scan_batches WHERE batchId = :batchId LIMIT 1")
    suspend fun getRegisterBatchById(batchId: String): RegisterScanBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegisterBatch(batch: RegisterScanBatchEntity)

    @Query("UPDATE register_scan_batches SET inchargeVerified = 1 WHERE batchId = :batchId")
    suspend fun verifyBatchByIncharge(batchId: String)

    @Query("UPDATE register_scan_batches SET principalApproved = 1 WHERE batchId = :batchId")
    suspend fun approveBatchByPrincipal(batchId: String)

    // =========================================================================
    // 6. EXAMINATIONS & REPORT CARDS
    // =========================================================================
    @Query("SELECT * FROM exam_records ORDER BY classSection ASC, rollNumber ASC")
    fun getAllExamRecords(): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE studentId = :studentId ORDER BY examType ASC, subject ASC")
    fun getExamRecordsForStudent(studentId: String): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE classSection = :classSection ORDER BY rollNumber ASC")
    fun getExamRecordsForClass(classSection: String): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE classSection = :classSection AND subject = :subject ORDER BY rollNumber ASC")
    fun getExamRecordsForClassAndSubject(classSection: String, subject: String): Flow<List<ExamRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamRecord(examRecord: ExamRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamRecords(records: List<ExamRecordEntity>)

    @Update
    suspend fun updateExamRecord(examRecord: ExamRecordEntity)

    // =========================================================================
    // 7. FEE MANAGEMENT & RECEIPTS
    // =========================================================================
    @Query("SELECT * FROM fee_records ORDER BY id ASC")
    fun getAllFeeRecords(): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE studentId = :studentId OR admissionNumber = :studentId ORDER BY id ASC")
    fun getFeeRecordsForStudent(studentId: String): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecord(fee: FeeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecords(fees: List<FeeRecordEntity>)

    @Query("UPDATE fee_records SET paidAmount = :paid, dueAmount = :due, status = :status, receiptNumber = :receipt, paidDate = :date, paymentMode = :mode WHERE id = :feeId")
    suspend fun recordFeePayment(
        feeId: String,
        paid: Long,
        due: Long,
        status: String,
        receipt: String,
        date: String,
        mode: String = "OFFLINE_ENTRY"
    )

    // =========================================================================
    // 8. CENTRALIZED DOCUMENTS & CERTIFICATES
    // =========================================================================
    @Query("SELECT * FROM certificates ORDER BY issueDate DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE studentId = :studentId ORDER BY issueDate DESC")
    fun getCertificatesForStudent(studentId: String): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE certificateId = :certificateId LIMIT 1")
    suspend fun getCertificateById(certificateId: String): CertificateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCertificate(certificate: CertificateEntity)

    @Update
    suspend fun updateCertificate(certificate: CertificateEntity)

    @Query("UPDATE certificates SET applicationStatus = :status, verifiedByIncharge = :incharge, inchargeRemarks = :remarks WHERE certificateId = :id")
    suspend fun inchargeVerifyCertificate(id: String, status: String, incharge: String, remarks: String)

    @Query("UPDATE certificates SET applicationStatus = :status, approvedByPrincipal = :principal, principalRemarks = :remarks, isDownloadUnlocked = :unlocked, secureWhatsAppToken = :token WHERE certificateId = :id")
    suspend fun principalApproveCertificate(
        id: String,
        status: String,
        principal: String,
        remarks: String,
        unlocked: Boolean,
        token: String
    )

    // =========================================================================
    // 9. COMPUTERIZED DIGITAL ID CARDS
    // =========================================================================
    @Query("SELECT * FROM id_card_records ORDER BY issueDate DESC")
    fun getAllIdCards(): Flow<List<IdCardRecordEntity>>

    @Query("SELECT * FROM id_card_records WHERE personId = :personId LIMIT 1")
    suspend fun getIdCardForPerson(personId: String): IdCardRecordEntity?

    @Query("SELECT * FROM id_card_records WHERE personId = :personId")
    fun observeIdCardForPerson(personId: String): Flow<List<IdCardRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIdCard(idCard: IdCardRecordEntity)

    @Update
    suspend fun updateIdCard(idCard: IdCardRecordEntity)

    @Query("UPDATE id_card_records SET verificationStatus = :status, inchargeRemarks = :remarks WHERE cardId = :cardId")
    suspend fun inchargeVerifyIdCard(cardId: String, status: String, remarks: String)

    @Query("UPDATE id_card_records SET verificationStatus = :status, principalRemarks = :remarks, approvedBy = :approvedBy WHERE cardId = :cardId")
    suspend fun principalApproveIdCard(cardId: String, status: String, remarks: String, approvedBy: String)

    // =========================================================================
    // 10. ADMISSION APPLICATIONS
    // =========================================================================
    @Query("SELECT * FROM admission_applications ORDER BY applicationDate DESC")
    fun getAllAdmissionApplications(): Flow<List<AdmissionApplicationEntity>>

    @Query("SELECT * FROM admission_applications WHERE applicationId = :appId LIMIT 1")
    suspend fun getAdmissionApplicationById(appId: String): AdmissionApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmissionApplication(application: AdmissionApplicationEntity)

    @Update
    suspend fun updateAdmissionApplication(application: AdmissionApplicationEntity)

    // =========================================================================
    // 11. TIMETABLE
    // =========================================================================
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, periodNumber ASC")
    fun getAllTimetableSlots(): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE classSection = :classSection ORDER BY dayOfWeek ASC, periodNumber ASC")
    fun getTimetableForClass(classSection: String): Flow<List<TimetableSlotEntity>>

    @Query("SELECT * FROM timetable_slots WHERE teacherId = :teacherId ORDER BY dayOfWeek ASC, periodNumber ASC")
    fun getTimetableForTeacher(teacherId: String): Flow<List<TimetableSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlot(slot: TimetableSlotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimetableSlots(slots: List<TimetableSlotEntity>)

    // =========================================================================
    // 12. NOTICES & CIRCULARS
    // =========================================================================
    @Query("SELECT * FROM notices_and_meetings ORDER BY date DESC")
    fun getAllNotices(): Flow<List<NoticeMeetingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: NoticeMeetingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeMeetingEntity>)

    @Query("SELECT * FROM circulars ORDER BY issuedTimestamp DESC")
    fun getAllCirculars(): Flow<List<CircularEntity>>

    @Query("SELECT * FROM circulars WHERE targetRole = :role OR targetRole = 'ALL' ORDER BY issuedTimestamp DESC")
    fun getCircularsForRole(role: String): Flow<List<CircularEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCircular(circular: CircularEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCirculars(circulars: List<CircularEntity>)

    // =========================================================================
    // 13. SCHOOL DIARY & INSTRUCTIONS
    // =========================================================================
    @Query("SELECT * FROM school_diary ORDER BY id DESC")
    fun getAllDiaryEntries(): Flow<List<SchoolDiaryEntity>>

    @Query("SELECT * FROM school_diary WHERE classSection = :classSection ORDER BY id DESC")
    fun getDiaryEntriesForClass(classSection: String): Flow<List<SchoolDiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: SchoolDiaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntries(entries: List<SchoolDiaryEntity>)

    @Query("SELECT * FROM school_instructions ORDER BY orderIndex ASC")
    fun getAllInstructions(): Flow<List<SchoolInstructionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstructions(instructions: List<SchoolInstructionEntity>)

    // =========================================================================
    // 14. INSTITUTIONAL BILLS & GOVERNMENT COMPLIANCE
    // =========================================================================
    @Query("SELECT * FROM institutional_bills ORDER BY dueDate DESC")
    fun getAllBills(): Flow<List<InstitutionalBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: InstitutionalBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<InstitutionalBillEntity>)

    @Update
    suspend fun updateBill(bill: InstitutionalBillEntity)

    @Query("SELECT * FROM government_compliance ORDER BY expiryDate ASC")
    fun getAllComplianceDocuments(): Flow<List<GovernmentComplianceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplianceDocument(compliance: GovernmentComplianceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplianceDocuments(documents: List<GovernmentComplianceEntity>)

    // =========================================================================
    // 15. OFFLINE SYNCHRONIZATION QUEUE
    // =========================================================================
    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED' ORDER BY timestamp ASC")
    fun getPendingSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus = 'PENDING' OR syncStatus = 'FAILED'")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueue(item: SyncQueueEntity)

    @Update
    suspend fun updateSyncQueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE syncId = :syncId")
    suspend fun deleteSyncQueueItem(syncId: Long)

    // =========================================================================
    // 16. INCHARGE EDIT REQUESTS & AUDIT LOGS
    // =========================================================================
    @Query("SELECT * FROM incharge_edit_requests ORDER BY requestTimestamp DESC")
    fun getAllInchargeRequests(): Flow<List<InchargeEditRequestEntity>>

    @Query("SELECT * FROM incharge_edit_requests WHERE requestId = :requestId LIMIT 1")
    suspend fun getInchargeRequestById(requestId: String): InchargeEditRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInchargeRequest(request: InchargeEditRequestEntity)

    @Query("UPDATE incharge_edit_requests SET status = :status, principalRemarks = :remarks, principalAuthTimestamp = :timestamp WHERE requestId = :requestId")
    suspend fun authenticateInchargeRequest(requestId: String, status: String, remarks: String, timestamp: Long)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    // =========================================================================
    // 17. CORE USERS & TEACHERS
    // =========================================================================
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE (LOWER(username) = LOWER(:username) OR LOWER(email) = LOWER(:username) OR phoneNumber = :username OR id = :username) AND institutionId = :institutionId LIMIT 1")
    suspend fun getUserByCredentials(username: String, institutionId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) OR LOWER(email) = LOWER(:username) OR phoneNumber = :username OR id = :username LIMIT 1")
    suspend fun getUserByUsernameOrPhone(username: String): UserEntity?

    @Query("UPDATE users SET password = :newPassword WHERE id = :userId")
    suspend fun updateUserPassword(userId: String, newPassword: String)

    @Query("SELECT * FROM users WHERE role = :role ORDER BY displayName ASC")
    fun getUsersByRole(role: String): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE id = :teacherId LIMIT 1")
    suspend fun getTeacherById(teacherId: String): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)

    // =========================================================================
    // 18. TRANSACTIONAL OPERATIONS
    // =========================================================================
    @Transaction
    suspend fun applyBatchAttendanceAndSyncRoster(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>,
        percentageUpdates: Map<String, Float>
    ) {
        insertRegisterBatch(batch)
        insertAttendanceRecords(records)
        percentageUpdates.forEach { (studentId, pct) ->
            updateStudentAttendancePercentage(studentId, pct)
        }
    }

    @Transaction
    suspend fun approveAndApplyStudentInchargeEdit(
        requestId: String,
        remarks: String,
        timestamp: Long,
        updatedStudent: StudentEntity
    ) {
        authenticateInchargeRequest(requestId, "AUTHENTICATED_BY_PRINCIPAL", remarks, timestamp)
        updateStudent(updatedStudent)
    }

    @Transaction
    suspend fun inchargeVerifyBatchWithAudit(
        batchId: String,
        auditLog: AuditLogEntity
    ) {
        verifyBatchByIncharge(batchId)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun principalApproveBatchWithAudit(
        batchId: String,
        auditLog: AuditLogEntity
    ) {
        approveBatchByPrincipal(batchId)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun inchargeVerifyCertificateWithAudit(
        certId: String,
        status: String,
        incharge: String,
        remarks: String,
        auditLog: AuditLogEntity
    ) {
        inchargeVerifyCertificate(certId, status, incharge, remarks)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun principalApproveCertificateWithAudit(
        certId: String,
        status: String,
        principal: String,
        remarks: String,
        unlocked: Boolean,
        token: String,
        auditLog: AuditLogEntity
    ) {
        principalApproveCertificate(certId, status, principal, remarks, unlocked, token)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun inchargeVerifyIdCardWithAudit(
        cardId: String,
        status: String,
        remarks: String,
        auditLog: AuditLogEntity
    ) {
        inchargeVerifyIdCard(cardId, status, remarks)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun principalApproveIdCardWithAudit(
        cardId: String,
        status: String,
        remarks: String,
        approvedBy: String,
        auditLog: AuditLogEntity
    ) {
        principalApproveIdCard(cardId, status, remarks, approvedBy)
        insertAuditLog(auditLog)
    }
}
