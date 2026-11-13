package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AcademicPromotionHistoryEntity
import com.example.data.local.entities.AcademicYearEntity
import com.example.data.local.entities.AdmissionApplicationEntity
import com.example.data.local.entities.AiAuditLogEntity
import com.example.data.local.entities.AttendanceCorrectionEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.AutomatedMessageDispatchEntity
import com.example.data.local.entities.CampusEntity
import com.example.data.local.entities.CertificateEntity
import com.example.data.local.entities.CircularEntity
import com.example.data.local.entities.ClassroomEntity
import com.example.data.local.entities.CloudDocumentEntity
import com.example.data.local.entities.DeviceTokenEntity
import com.example.data.local.entities.DisasterRecoveryBackupEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.EmergencyContactEntity
import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.ExamScheduleEntity
import com.example.data.local.entities.FeatureFlagEntity
import com.example.data.local.entities.FeeDiscountEntity
import com.example.data.local.entities.FeeInvoiceEntity
import com.example.data.local.entities.FeePaymentTransactionEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.FeeStructureEntity
import com.example.data.local.entities.FinancialLedgerEntryEntity
import com.example.data.local.entities.GovernmentComplianceEntity
import com.example.data.local.entities.GradingSchemeEntity
import com.example.data.local.entities.HomeworkEntity
import com.example.data.local.entities.HomeworkSubmissionEntity
import com.example.data.local.entities.IdCardRecordEntity
import com.example.data.local.entities.InchargeEditRequestEntity
import com.example.data.local.entities.InstitutionEntity
import com.example.data.local.entities.InstitutionalBillEntity
import com.example.data.local.entities.InventoryItemEntity
import com.example.data.local.entities.InventoryTransactionEntity
import com.example.data.local.entities.LibraryBookEntity
import com.example.data.local.entities.LibraryIssueEntity
import com.example.data.local.entities.NoticeMeetingEntity
import com.example.data.local.entities.NotificationPreferenceEntity
import com.example.data.local.entities.ParentEntity
import com.example.data.local.entities.ParentWardLinkEntity
import com.example.data.local.entities.PaymentOrderEntity
import com.example.data.local.entities.PushNotificationEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.local.entities.RolePermissionEntity
import com.example.data.local.entities.SchoolDiaryEntity
import com.example.data.local.entities.SchoolInstructionEntity
import com.example.data.local.entities.SchoolSettingsEntity
import com.example.data.local.entities.SectionEntity
import com.example.data.local.entities.StaffEntity
import com.example.data.local.entities.StaffLeaveRequestEntity
import com.example.data.local.entities.StudentEnrollmentEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentLeaveRequestEntity
import com.example.data.local.entities.SubjectEntity
import com.example.data.local.entities.SupportTicketEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.TeacherAssignmentEntity
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.TimetableSlotEntity
import com.example.data.local.entities.TransportPickupPointEntity
import com.example.data.local.entities.TransportRouteEntity
import com.example.data.local.entities.TransportVehicleEntity
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
    // 2. ACADEMIC CLASSES & SECTIONS (Pre-fed Database & Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM academic_classes WHERE isArchived = 0 ORDER BY className ASC, section ASC")
    fun getAllClasses(): Flow<List<AcademicClassEntity>>

    @Query("SELECT * FROM academic_classes WHERE isArchived = 0 ORDER BY className ASC, section ASC")
    suspend fun getAllClassesList(): List<AcademicClassEntity>

    @Query("SELECT * FROM academic_classes WHERE institutionId = :institutionId AND isArchived = 0 ORDER BY className ASC, section ASC")
    fun getClassesForInstitution(institutionId: String): Flow<List<AcademicClassEntity>>

    @Query("SELECT * FROM academic_classes WHERE institutionId = :institutionId AND isArchived = 0 ORDER BY className ASC, section ASC")
    suspend fun getClassesListForInstitution(institutionId: String): List<AcademicClassEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicClass(academicClass: AcademicClassEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicClasses(classes: List<AcademicClassEntity>)

    @Update
    suspend fun updateAcademicClass(academicClass: AcademicClassEntity)

    // =========================================================================
    // 3. STAFF MEMBERS (Fed & Managed by Principal - Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM staff_members ORDER BY roleType ASC, name ASC")
    fun getAllStaff(): Flow<List<StaffEntity>>

    @Query("SELECT * FROM staff_members")
    suspend fun getAllStaffList(): List<StaffEntity>

    @Query("SELECT * FROM staff_members WHERE institutionId = :institutionId ORDER BY roleType ASC, name ASC")
    fun getStaffForInstitution(institutionId: String): Flow<List<StaffEntity>>

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

    @Query("SELECT * FROM students WHERE institutionId = :institutionId ORDER BY grade ASC, rollNumber ASC")
    fun getStudentsForInstitution(institutionId: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students ORDER BY grade ASC, rollNumber ASC")
    suspend fun getAllStudentsList(): List<StudentEntity>

    @Query("SELECT * FROM students WHERE institutionId = :institutionId ORDER BY grade ASC, rollNumber ASC")
    suspend fun getStudentsListForInstitution(institutionId: String): List<StudentEntity>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: String): StudentEntity?

    @Query("SELECT * FROM students WHERE (admissionNumber = :admissionNo OR id = :admissionNo) AND (REPLACE(REPLACE(REPLACE(guardianPhone, ' ', ''), '-', ''), '+91', '') = :cleanPhone OR REPLACE(REPLACE(REPLACE(studentPhone, ' ', ''), '-', ''), '+91', '') = :cleanPhone) LIMIT 1")
    suspend fun getStudentByAdmissionAndCleanPhone(admissionNo: String, cleanPhone: String): StudentEntity?

    @Query("SELECT * FROM students WHERE admissionNumber = :admissionNo OR id = :admissionNo LIMIT 1")
    suspend fun getStudentByAdmissionNumber(admissionNo: String): StudentEntity?

    @Query("SELECT * FROM students WHERE institutionId = :institutionId AND (name LIKE '%' || :query || '%' OR admissionNumber LIKE '%' || :query || '%' OR grade LIKE '%' || :query || '%' OR section LIKE '%' || :query || '%') ORDER BY grade ASC, section ASC, rollNumber ASC")
    fun searchStudentsByTenant(institutionId: String, query: String): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE institutionId = :institutionId AND (name LIKE '%' || :query || '%' OR admissionNumber LIKE '%' || :query || '%') ORDER BY grade ASC, section ASC, rollNumber ASC")
    suspend fun searchStudentsByTenantList(institutionId: String, query: String): List<StudentEntity>

    @Query("UPDATE students SET feePaid = :paidRupees, feeDue = :dueRupees WHERE id = :studentId AND institutionId = :institutionId")
    suspend fun updateStudentFeeSummary(studentId: String, institutionId: String, paidRupees: Double, dueRupees: Double)

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

    @Query("UPDATE students SET attendancePercentage = :newPercentage WHERE id = :studentId AND institutionId = :institutionId")
    suspend fun updateStudentAttendancePercentageInTenant(studentId: String, institutionId: String, newPercentage: Float)

    @Query("UPDATE students SET realIdPhotoUri = :photoUri, convertedDigitalId = :digitalId, digitalIdStatus = :status WHERE id = :studentId")
    suspend fun updateStudentIdCard(studentId: String, photoUri: String?, digitalId: String, status: String)

    @Query("UPDATE students SET grade = :newGrade, section = :newSection, promotionStatus = :status WHERE id = :studentId")
    suspend fun updateStudentPromotion(studentId: String, newGrade: String, newSection: String, status: String)

    // =========================================================================
    // 5. ATTENDANCE & ROLL-CALL REGISTERS (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM attendance ORDER BY dateTimestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE institutionId = :institutionId ORDER BY dateTimestamp DESC")
    fun getAttendanceForInstitution(institutionId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY dateTimestamp DESC")
    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE classSection = :classSection ORDER BY dateTimestamp DESC")
    fun getAttendanceForClassSection(classSection: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE classSection = :classSection AND date = :date")
    suspend fun getAttendanceForClassAndDate(classSection: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE institutionId = :institutionId AND classSection = :classSection AND date = :date")
    suspend fun getAttendanceForClassAndDateInTenant(institutionId: String, classSection: String, date: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE classSection = :classSection AND date = :date AND session = :session")
    suspend fun getAttendanceForClassDateAndSession(classSection: String, date: String, session: String): List<AttendanceEntity>

    @Query("SELECT * FROM attendance WHERE institutionId = :institutionId AND classSection = :classSection AND date = :date AND session = :session")
    suspend fun getAttendanceForClassDateAndSessionInTenant(institutionId: String, classSection: String, date: String, session: String): List<AttendanceEntity>

    @Query("DELETE FROM attendance WHERE classSection = :classSection AND date = :date")
    suspend fun deleteAttendanceForClassAndDate(classSection: String, date: String)

    @Query("DELETE FROM attendance WHERE institutionId = :institutionId AND classSection = :classSection AND date = :date")
    suspend fun deleteAttendanceForClassAndDateInTenant(institutionId: String, classSection: String, date: String)

    @Query("DELETE FROM attendance_records WHERE institutionId = :institutionId AND classSection = :classSection AND date = :date AND sourceMethod = 'DAILY_ATTENDANCE_UI'")
    suspend fun deleteDailyUiAttendanceRecordsForClassAndDateInTenant(institutionId: String, classSection: String, date: String)

    @Query("DELETE FROM attendance WHERE classSection = :classSection AND date = :date AND session = :session")
    suspend fun deleteAttendanceForClassDateAndSession(classSection: String, date: String, session: String)

    @Query("SELECT * FROM attendance WHERE dateTimestamp BETWEEN :startEpochMs AND :endEpochMs ORDER BY dateTimestamp DESC")
    fun getAttendanceInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(list: List<AttendanceEntity>)

    @Query("SELECT * FROM attendance_records ORDER BY recordId DESC")
    fun getAllAttendanceRecords(): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE institutionId = :institutionId ORDER BY recordId DESC")
    fun getAttendanceRecordsForInstitution(institutionId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE personId = :personId ORDER BY recordId DESC")
    fun getAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>>

    @Query("SELECT * FROM attendance_records WHERE classSection = :classSection ORDER BY recordId DESC")
    fun getAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>>

    @Query("UPDATE attendance_records SET isSyncedToCentral = 1 WHERE classSection = :classSection AND date = :date AND personId = :personId")
    suspend fun markAttendanceRecordSynced(classSection: String, date: String, personId: String)

    @Query("UPDATE attendance_records SET isSyncedToCentral = 1 WHERE institutionId = :institutionId AND classSection = :classSection AND date = :date AND personId = :personId")
    suspend fun markAttendanceRecordSyncedInTenant(institutionId: String, classSection: String, date: String, personId: String)

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
    // 6. EXAMINATIONS & REPORT CARDS (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM exam_records ORDER BY classSection ASC, rollNumber ASC")
    fun getAllExamRecords(): Flow<List<ExamRecordEntity>>

    @Query("SELECT * FROM exam_records WHERE institutionId = :institutionId ORDER BY classSection ASC, rollNumber ASC")
    fun getExamRecordsForInstitution(institutionId: String): Flow<List<ExamRecordEntity>>

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
    // 7. FEE MANAGEMENT & RECEIPTS (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM fee_records ORDER BY id ASC")
    fun getAllFeeRecords(): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE institutionId = :institutionId ORDER BY id ASC")
    fun getFeeRecordsForInstitution(institutionId: String): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE studentId = :studentId OR admissionNumber = :studentId ORDER BY id ASC")
    fun getFeeRecordsForStudent(studentId: String): Flow<List<FeeRecordEntity>>

    @Query("SELECT * FROM fee_records WHERE id = :feeId LIMIT 1")
    suspend fun getFeeRecordById(feeId: String): FeeRecordEntity?

    @Query("SELECT * FROM fee_records WHERE institutionId = :institutionId ORDER BY id ASC")
    suspend fun getFeeRecordsForInstitutionList(institutionId: String): List<FeeRecordEntity>

    @Query("SELECT * FROM fee_records WHERE institutionId = :institutionId AND (studentId = :studentId OR admissionNumber = :studentId) ORDER BY id ASC")
    suspend fun getFeeRecordsForStudentList(studentId: String, institutionId: String): List<FeeRecordEntity>

    @Query("SELECT * FROM fee_records WHERE institutionId = :institutionId AND (studentId = :studentId OR admissionNumber = :studentId) ORDER BY id ASC")
    fun getFeeRecordsForStudentInTenant(studentId: String, institutionId: String): Flow<List<FeeRecordEntity>>

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
        mode: String = "CASH"
    )

    // =========================================================================
    // 8. CENTRALIZED DOCUMENTS & CERTIFICATES (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM certificates ORDER BY issueDate DESC")
    fun getAllCertificates(): Flow<List<CertificateEntity>>

    @Query("SELECT * FROM certificates WHERE institutionId = :institutionId ORDER BY issueDate DESC")
    fun getCertificatesForInstitution(institutionId: String): Flow<List<CertificateEntity>>

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
    // 9. COMPUTERIZED DIGITAL ID CARDS (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM id_card_records ORDER BY issueDate DESC")
    fun getAllIdCards(): Flow<List<IdCardRecordEntity>>

    @Query("SELECT * FROM id_card_records WHERE institutionId = :institutionId ORDER BY issueDate DESC")
    fun getIdCardsForInstitution(institutionId: String): Flow<List<IdCardRecordEntity>>

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
    // 10. ADMISSION APPLICATIONS (Tenant-Isolated)
    // =========================================================================
    @Query("SELECT * FROM admission_applications ORDER BY applicationDate DESC")
    fun getAllAdmissionApplications(): Flow<List<AdmissionApplicationEntity>>

    @Query("SELECT * FROM admission_applications WHERE institutionId = :institutionId ORDER BY applicationDate DESC")
    fun getAdmissionApplicationsForInstitution(institutionId: String): Flow<List<AdmissionApplicationEntity>>

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

    @Query("SELECT * FROM audit_logs ORDER BY logId ASC")
    suspend fun getAllAuditLogsAsc(): List<AuditLogEntity>

    @Query("SELECT * FROM audit_logs ORDER BY logId DESC LIMIT 1")
    suspend fun getLatestAuditLog(): AuditLogEntity?

    @Query("SELECT * FROM audit_logs WHERE syncStatus = 'PENDING_SYNC' ORDER BY timestamp ASC")
    suspend fun getPendingAuditLogs(): List<AuditLogEntity>

    @Query("SELECT COUNT(*) FROM audit_logs WHERE syncStatus = 'PENDING_SYNC'")
    fun getPendingAuditSyncCount(): Flow<Int>

    @Query("SELECT * FROM audit_logs WHERE interactionType = :type ORDER BY timestamp DESC")
    fun getAuditLogsByInteractionType(type: String): Flow<List<AuditLogEntity>>

    @Query("UPDATE audit_logs SET syncStatus = :status, syncTimestamp = :syncTimestamp, encryptedPayload = :encryptedPayload WHERE logId IN (:logIds)")
    suspend fun markAuditLogsSynced(logIds: List<Long>, status: String, syncTimestamp: Long, encryptedPayload: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    // =========================================================================
    // 17. CORE USERS & TEACHERS
    // =========================================================================
    @Query("SELECT * FROM users ORDER BY displayName ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getUsersList(): List<UserEntity>

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

    // --- Atomic Entity + Outbox Operations ---

    @Transaction
    suspend fun saveDailyAttendanceBatchWithOutbox(
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        records: List<AttendanceRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        deleteAttendanceForClassAndDate(classSection.trim(), date.trim())
        insertAttendanceList(attendances)
        insertAttendanceRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun saveDailyAttendanceBatchWithOutboxInTenant(
        institutionId: String,
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        records: List<AttendanceRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        deleteAttendanceForClassAndDateInTenant(institutionId, classSection.trim(), date.trim())
        deleteDailyUiAttendanceRecordsForClassAndDateInTenant(institutionId, classSection.trim(), date.trim())
        insertAttendanceList(attendances)
        insertAttendanceRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun saveDailyAttendanceSessionBatchWithOutbox(
        classSection: String,
        date: String,
        session: String,
        attendances: List<AttendanceEntity>,
        records: List<AttendanceRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        deleteAttendanceForClassDateAndSession(classSection.trim(), date.trim(), session.trim())
        insertAttendanceList(attendances)
        insertAttendanceRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun insertAttendanceRecordWithOutbox(
        record: AttendanceRecordEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertAttendanceRecord(record)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertAttendanceRecordsWithOutbox(
        records: List<AttendanceRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        insertAttendanceRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun commitRegisterScanWithOutbox(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        insertRegisterBatch(batch)
        insertAttendanceRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun inchargeVerifyBatchWithOutbox(
        batchId: String,
        auditLog: AuditLogEntity?,
        outboxItem: DurableOutboxEntity
    ) {
        verifyBatchByIncharge(batchId)
        auditLog?.let { insertAuditLog(it) }
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun principalApproveBatchWithOutbox(
        batchId: String,
        auditLog: AuditLogEntity?,
        outboxItem: DurableOutboxEntity
    ) {
        approveBatchByPrincipal(batchId)
        auditLog?.let { insertAuditLog(it) }
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertExamRecordWithOutbox(
        examRecord: ExamRecordEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertExamRecord(examRecord)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertExamRecordsWithOutbox(
        records: List<ExamRecordEntity>,
        outboxItems: List<DurableOutboxEntity>
    ) {
        insertExamRecords(records)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun recordFeePaymentWithOutbox(
        feeId: String,
        paid: Long,
        due: Long,
        status: String,
        receipt: String,
        date: String,
        mode: String,
        outboxItem: DurableOutboxEntity
    ) {
        recordFeePayment(feeId, paid, due, status, receipt, date, mode)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertFeeRecordWithOutbox(
        fee: FeeRecordEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertFeeRecord(fee)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertStudentWithOutbox(
        student: StudentEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertStudent(student)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun insertAdmissionApplicationWithOutbox(
        app: AdmissionApplicationEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertAdmissionApplication(app)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun enrollStudentFromAdmissionWithOutbox(
        app: AdmissionApplicationEntity,
        student: StudentEntity,
        outboxItems: List<DurableOutboxEntity>
    ) {
        insertStudent(student)
        updateAdmissionApplication(app)
        insertOutboxItems(outboxItems)
    }

    @Transaction
    suspend fun insertCertificateWithOutbox(
        certificate: CertificateEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertCertificate(certificate)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun inchargeVerifyCertificateWithOutbox(
        certId: String,
        status: String,
        incharge: String,
        remarks: String,
        outboxItem: DurableOutboxEntity
    ) {
        inchargeVerifyCertificate(certId, status, incharge, remarks)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun principalApproveCertificateWithOutbox(
        certId: String,
        status: String,
        principal: String,
        remarks: String,
        unlocked: Boolean,
        token: String,
        outboxItem: DurableOutboxEntity
    ) {
        principalApproveCertificate(certId, status, principal, remarks, unlocked, token)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun saveIdCardWithOutbox(
        idCard: IdCardRecordEntity,
        outboxItem: DurableOutboxEntity
    ) {
        insertIdCard(idCard)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun inchargeVerifyIdCardWithOutbox(
        cardId: String,
        status: String,
        remarks: String,
        outboxItem: DurableOutboxEntity
    ) {
        inchargeVerifyIdCard(cardId, status, remarks)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun principalApproveIdCardWithOutbox(
        cardId: String,
        status: String,
        remarks: String,
        approvedBy: String,
        outboxItem: DurableOutboxEntity
    ) {
        principalApproveIdCard(cardId, status, remarks, approvedBy)
        insertOutbox(outboxItem)
    }

    // =========================================================================
    // 19. HOMEWORK & ASSIGNMENT MANAGEMENT
    // =========================================================================
    @Query("SELECT * FROM homework_assignments WHERE institutionId = :institutionId ORDER BY createdAt DESC")
    fun getHomeworkForInstitution(institutionId: String): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework_assignments WHERE classSection = :classSection AND institutionId = :institutionId ORDER BY dueDate ASC")
    fun getHomeworkForClass(classSection: String, institutionId: String): Flow<List<HomeworkEntity>>

    @Query("SELECT * FROM homework_assignments WHERE homeworkId = :homeworkId LIMIT 1")
    suspend fun getHomeworkById(homeworkId: String): HomeworkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomework(homework: HomeworkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkList(list: List<HomeworkEntity>)

    @Query("DELETE FROM homework_assignments WHERE homeworkId = :homeworkId")
    suspend fun deleteHomework(homeworkId: String)

    @Query("SELECT * FROM homework_submissions WHERE homeworkId = :homeworkId ORDER BY submissionTimestamp ASC")
    fun getSubmissionsForHomework(homeworkId: String): Flow<List<HomeworkSubmissionEntity>>

    @Query("SELECT * FROM homework_submissions WHERE studentId = :studentId ORDER BY submissionTimestamp DESC")
    fun getSubmissionsForStudent(studentId: String): Flow<List<HomeworkSubmissionEntity>>

    @Query("SELECT * FROM homework_submissions WHERE homeworkId = :homeworkId AND studentId = :studentId LIMIT 1")
    suspend fun getSubmission(homeworkId: String, studentId: String): HomeworkSubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomeworkSubmission(submission: HomeworkSubmissionEntity)

    @Query("UPDATE homework_submissions SET status = :status, marksObtained = :marks, teacherFeedback = :feedback, gradedAt = :gradedAt WHERE submissionId = :submissionId")
    suspend fun gradeHomeworkSubmission(
        submissionId: String,
        status: String,
        marks: Int,
        feedback: String,
        gradedAt: Long = System.currentTimeMillis()
    )

    // =========================================================================
    // 20. STUDENT & STAFF LEAVE REQUESTS
    // =========================================================================
    @Query("SELECT * FROM leave_requests WHERE institutionId = :institutionId ORDER BY appliedTimestamp DESC")
    fun getLeaveRequestsForInstitution(institutionId: String): Flow<List<StudentLeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE studentId = :studentId ORDER BY appliedTimestamp DESC")
    fun getLeaveRequestsForStudent(studentId: String): Flow<List<StudentLeaveRequestEntity>>

    @Query("SELECT * FROM leave_requests WHERE classSection = :classSection ORDER BY appliedTimestamp DESC")
    fun getLeaveRequestsForClass(classSection: String): Flow<List<StudentLeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveRequest(request: StudentLeaveRequestEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaveRequests(requests: List<StudentLeaveRequestEntity>)

    @Query("UPDATE leave_requests SET status = :status, reviewedBy = :reviewer, reviewerRemarks = :remarks, reviewedTimestamp = :timestamp WHERE leaveId = :leaveId")
    suspend fun updateLeaveRequestStatus(
        leaveId: String,
        status: String,
        reviewer: String,
        remarks: String,
        timestamp: Long = System.currentTimeMillis()
    )

    // =========================================================================
    // 21. FEE STRUCTURES & TRANSACTIONS
    // =========================================================================
    @Query("SELECT * FROM fee_structures WHERE institutionId = :institutionId ORDER BY grade ASC")
    fun getFeeStructures(institutionId: String): Flow<List<FeeStructureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeStructure(structure: FeeStructureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeStructures(structures: List<FeeStructureEntity>)

    @Query("SELECT * FROM fee_payment_transactions ORDER BY timestamp DESC")
    fun getAllPaymentTransactions(): Flow<List<FeePaymentTransactionEntity>>

    @Query("SELECT * FROM fee_payment_transactions WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getPaymentTransactions(institutionId: String): Flow<List<FeePaymentTransactionEntity>>

    @Query("SELECT * FROM fee_payment_transactions WHERE studentId = :studentId ORDER BY timestamp DESC")
    fun getPaymentTransactionsForStudent(studentId: String): Flow<List<FeePaymentTransactionEntity>>

    @Query("SELECT * FROM fee_payment_transactions WHERE institutionId = :institutionId AND studentId = :studentId ORDER BY timestamp DESC")
    fun getPaymentTransactionsForStudentInTenant(studentId: String, institutionId: String): Flow<List<FeePaymentTransactionEntity>>

    @Query("SELECT * FROM fee_payment_transactions WHERE feeRecordId = :feeRecordId AND institutionId = :institutionId AND status NOT IN ('REFUNDED', 'VOID', 'FAILED') ORDER BY timestamp ASC")
    suspend fun getConfirmedTransactionsForFeeRecord(feeRecordId: String, institutionId: String): List<FeePaymentTransactionEntity>

    @Query("SELECT * FROM fee_payment_transactions WHERE feeRecordId = :feeRecordId ORDER BY timestamp DESC")
    suspend fun getPaymentTransactionsForFeeRecordList(feeRecordId: String): List<FeePaymentTransactionEntity>

    @Query("SELECT * FROM fee_payment_transactions WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getPaymentTransactionByIdempotencyKey(idempotencyKey: String): FeePaymentTransactionEntity?

    @Query("SELECT * FROM fee_payment_transactions WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getPaymentTransactionByReceiptNumber(receiptNumber: String): FeePaymentTransactionEntity?

    @Query("SELECT * FROM fee_payment_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getPaymentTransactionById(transactionId: String): FeePaymentTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPaymentTransactionStrict(txn: FeePaymentTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentTransaction(txn: FeePaymentTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentTransactions(txns: List<FeePaymentTransactionEntity>)

    @Query("UPDATE fee_payment_transactions SET isSyncedToCentral = 1, syncStatus = 'SYNCED', serverAckToken = :ackToken WHERE transactionId = :transactionId")
    suspend fun markPaymentTransactionSynced(transactionId: String, ackToken: String)

    @Query("UPDATE fee_payment_transactions SET isSyncedToCentral = 0, syncStatus = 'CONFLICT' WHERE transactionId = :transactionId")
    suspend fun markPaymentTransactionConflict(transactionId: String)

    // =========================================================================
    // 22. PUSH NOTIFICATIONS
    // =========================================================================
    @Query("SELECT * FROM push_notifications WHERE institutionId = :institutionId AND (recipientUserId = 'ALL' OR recipientUserId = :userId) ORDER BY timestamp DESC")
    fun getNotificationsForUser(institutionId: String, userId: String): Flow<List<PushNotificationEntity>>

    @Query("SELECT COUNT(*) FROM push_notifications WHERE institutionId = :institutionId AND (recipientUserId = 'ALL' OR recipientUserId = :userId) AND isRead = 0")
    fun getUnreadNotificationCount(institutionId: String, userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: PushNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<PushNotificationEntity>)

    @Query("UPDATE push_notifications SET isRead = 1 WHERE notificationId = :notificationId")
    suspend fun markNotificationAsRead(notificationId: String)

    @Query("UPDATE push_notifications SET isRead = 1 WHERE institutionId = :institutionId")
    suspend fun markAllNotificationsAsRead(institutionId: String)

    // =========================================================================
    // 23. AI AUDIT LOGS
    // =========================================================================
    @Query("SELECT * FROM ai_audit_logs WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getAiAuditLogs(institutionId: String): Flow<List<AiAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiAuditLog(log: AiAuditLogEntity)

    // =========================================================================
    // 24. SUBJECTS MASTER
    // =========================================================================
    @Query("SELECT * FROM subjects WHERE institutionId = :institutionId ORDER BY classSection ASC, subjectName ASC")
    fun getSubjectsForInstitution(institutionId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE classSection = :classSection AND institutionId = :institutionId ORDER BY subjectName ASC")
    fun getSubjectsForClass(classSection: String, institutionId: String): Flow<List<SubjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    // =========================================================================
    // 25. PARENT WARD LINKS
    // =========================================================================
    @Query("SELECT * FROM parent_ward_links WHERE parentUserId = :parentUserId AND institutionId = :institutionId")
    fun getWardsForParent(parentUserId: String, institutionId: String): Flow<List<ParentWardLinkEntity>>

    @Query("SELECT * FROM parent_ward_links WHERE parentUserId = :parentUserId AND institutionId = :institutionId")
    suspend fun getWardsForParentList(parentUserId: String, institutionId: String): List<ParentWardLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentWardLink(link: ParentWardLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParentWardLinks(links: List<ParentWardLinkEntity>)

    // =========================================================================
    // 26. ATOMIC TRANSACTION WORKFLOWS
    // =========================================================================
    @Transaction
    suspend fun createFeeAssessmentWithOutbox(
        fee: FeeRecordEntity,
        auditLog: AuditLogEntity,
        outboxItem: DurableOutboxEntity,
        studentPaidRupees: Double,
        studentDueRupees: Double
    ) {
        insertFeeRecord(fee)
        updateStudentFeeSummary(fee.studentId, fee.institutionId, studentPaidRupees, studentDueRupees)
        insertAuditLog(auditLog)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun processFeePaymentAtomicWithOutbox(
        feeId: String,
        transaction: FeePaymentTransactionEntity,
        paidPaise: Long,
        duePaise: Long,
        status: String,
        auditLog: AuditLogEntity,
        outboxItem: DurableOutboxEntity,
        studentTotalPaidRupees: Double? = null,
        studentTotalDueRupees: Double? = null
    ) {
        insertPaymentTransactionStrict(transaction)
        recordFeePayment(
            feeId = feeId,
            paid = paidPaise,
            due = duePaise,
            status = status,
            receipt = transaction.receiptNumber,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(transaction.timestamp)),
            mode = transaction.paymentMode
        )
        if (studentTotalPaidRupees != null && studentTotalDueRupees != null) {
            updateStudentFeeSummary(transaction.studentId, transaction.institutionId, studentTotalPaidRupees, studentTotalDueRupees)
        }
        insertAuditLog(auditLog)
        insertOutbox(outboxItem)
    }

    @Transaction
    suspend fun processFeePaymentAtomic(
        feeId: String,
        transaction: FeePaymentTransactionEntity,
        paidPaise: Long,
        duePaise: Long,
        status: String,
        auditLog: AuditLogEntity
    ) {
        insertPaymentTransaction(transaction)
        recordFeePayment(
            feeId = feeId,
            paid = paidPaise,
            due = duePaise,
            status = status,
            receipt = transaction.receiptNumber,
            date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(transaction.timestamp)),
            mode = transaction.paymentMode
        )
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun gradeHomeworkWithAudit(
        submissionId: String,
        status: String,
        marks: Int,
        feedback: String,
        auditLog: AuditLogEntity
    ) {
        gradeHomeworkSubmission(submissionId, status, marks, feedback)
        insertAuditLog(auditLog)
    }

    @Transaction
    suspend fun reviewLeaveRequestWithAudit(
        leaveId: String,
        status: String,
        reviewer: String,
        remarks: String,
        auditLog: AuditLogEntity
    ) {
        updateLeaveRequestStatus(leaveId, status, reviewer, remarks)
        insertAuditLog(auditLog)
    }

    // =========================================================================
    // 25. CAMPUS & ACADEMIC YEARS
    // =========================================================================
    @Query("SELECT * FROM campuses WHERE institutionId = :institutionId")
    fun getCampuses(institutionId: String): Flow<List<CampusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampus(campus: CampusEntity)

    @Query("SELECT * FROM academic_years WHERE institutionId = :institutionId ORDER BY startDate DESC")
    fun getAcademicYears(institutionId: String): Flow<List<AcademicYearEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAcademicYear(year: AcademicYearEntity)

    @Query("SELECT * FROM academic_years WHERE institutionId = :institutionId AND isCurrent = 1 LIMIT 1")
    suspend fun getCurrentAcademicYear(institutionId: String): AcademicYearEntity?

    // =========================================================================
    // 26. ROLE PERMISSIONS (RBAC)
    // =========================================================================
    @Query("SELECT * FROM role_permissions WHERE institutionId = :institutionId AND role = :role")
    fun getPermissionsForRole(institutionId: String, role: String): Flow<List<RolePermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRolePermissions(permissions: List<RolePermissionEntity>)

    // =========================================================================
    // 27. PARENTS & EMERGENCY CONTACTS
    // =========================================================================
    @Query("SELECT * FROM parents WHERE institutionId = :institutionId")
    fun getParents(institutionId: String): Flow<List<ParentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParent(parent: ParentEntity)

    @Query("SELECT * FROM emergency_contacts WHERE studentId = :studentId ORDER BY priorityOrder ASC")
    fun getEmergencyContactsForStudent(studentId: String): Flow<List<EmergencyContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyContact(contact: EmergencyContactEntity)

    // =========================================================================
    // 28. STUDENT ENROLLMENTS, SECTIONS & CLASSROOMS
    // =========================================================================
    @Query("SELECT * FROM student_enrollments WHERE academicYear = :academicYear AND institutionId = :institutionId")
    fun getEnrollmentsByYear(academicYear: String, institutionId: String): Flow<List<StudentEnrollmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEnrollment(enrollment: StudentEnrollmentEntity)

    @Query("SELECT * FROM sections WHERE institutionId = :institutionId")
    fun getSections(institutionId: String): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity)

    @Query("SELECT * FROM classrooms WHERE institutionId = :institutionId")
    fun getClassrooms(institutionId: String): Flow<List<ClassroomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity)

    // =========================================================================
    // 29. TEACHER ASSIGNMENTS & ATTENDANCE CORRECTIONS
    // =========================================================================
    @Query("SELECT * FROM teacher_assignments WHERE institutionId = :institutionId AND academicYear = :academicYear")
    fun getTeacherAssignments(institutionId: String, academicYear: String): Flow<List<TeacherAssignmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacherAssignment(assignment: TeacherAssignmentEntity)

    @Query("SELECT * FROM attendance_corrections WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getAttendanceCorrections(institutionId: String): Flow<List<AttendanceCorrectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceCorrection(correction: AttendanceCorrectionEntity)

    // =========================================================================
    // 30. STAFF LEAVE REQUESTS
    // =========================================================================
    @Query("SELECT * FROM staff_leave_requests WHERE institutionId = :institutionId ORDER BY appliedTimestamp DESC")
    fun getStaffLeaveRequests(institutionId: String): Flow<List<StaffLeaveRequestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaffLeaveRequest(leave: StaffLeaveRequestEntity)

    // =========================================================================
    // 31. EXAM SCHEDULES & GRADING SCHEMES
    // =========================================================================
    @Query("SELECT * FROM exam_schedules WHERE institutionId = :institutionId AND academicYear = :academicYear ORDER BY examDate ASC")
    fun getExamSchedules(institutionId: String, academicYear: String): Flow<List<ExamScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamSchedule(schedule: ExamScheduleEntity)

    @Query("SELECT * FROM grading_schemes WHERE institutionId = :institutionId ORDER BY minPercentage DESC")
    fun getGradingSchemes(institutionId: String): Flow<List<GradingSchemeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingSchemes(schemes: List<GradingSchemeEntity>)

    // =========================================================================
    // 32. FEE DISCOUNTS & INVOICES
    // =========================================================================
    @Query("SELECT * FROM fee_discounts WHERE studentId = :studentId AND institutionId = :institutionId")
    fun getDiscountsForStudent(studentId: String, institutionId: String): Flow<List<FeeDiscountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeDiscount(discount: FeeDiscountEntity)

    @Query("SELECT * FROM fee_invoices WHERE institutionId = :institutionId ORDER BY dueDate ASC")
    fun getInvoices(institutionId: String): Flow<List<FeeInvoiceEntity>>

    @Query("SELECT * FROM fee_invoices WHERE studentId = :studentId ORDER BY dueDate ASC")
    fun getInvoicesForStudent(studentId: String): Flow<List<FeeInvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: FeeInvoiceEntity)

    // =========================================================================
    // 33. DEVICE TOKENS (PUSH NOTIFICATIONS)
    // =========================================================================
    @Query("SELECT * FROM device_tokens WHERE userId = :userId LIMIT 1")
    suspend fun getDeviceToken(userId: String): DeviceTokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceToken(token: DeviceTokenEntity)

    // =========================================================================
    // 34. LIBRARY BOOKS & ISSUES
    // =========================================================================
    @Query("SELECT * FROM library_books WHERE institutionId = :institutionId ORDER BY title ASC")
    fun getLibraryBooks(institutionId: String): Flow<List<LibraryBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryBook(book: LibraryBookEntity)

    @Query("SELECT * FROM library_issues WHERE institutionId = :institutionId ORDER BY issueDate DESC")
    fun getLibraryIssues(institutionId: String): Flow<List<LibraryIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryIssue(issue: LibraryIssueEntity)

    // =========================================================================
    // 35. TRANSPORTATION ROUTES, VEHICLES & STOPS
    // =========================================================================
    @Query("SELECT * FROM transport_routes WHERE institutionId = :institutionId")
    fun getTransportRoutes(institutionId: String): Flow<List<TransportRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransportRoute(route: TransportRouteEntity)

    @Query("SELECT * FROM transport_vehicles WHERE institutionId = :institutionId")
    fun getTransportVehicles(institutionId: String): Flow<List<TransportVehicleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransportVehicle(vehicle: TransportVehicleEntity)

    @Query("SELECT * FROM transport_pickup_points WHERE routeId = :routeId")
    fun getPickupPoints(routeId: String): Flow<List<TransportPickupPointEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPickupPoint(point: TransportPickupPointEntity)

    // =========================================================================
    // 36. INVENTORY ITEMS & TRANSACTIONS
    // =========================================================================
    @Query("SELECT * FROM inventory_items WHERE institutionId = :institutionId")
    fun getInventoryItems(institutionId: String): Flow<List<InventoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItemEntity)

    @Query("SELECT * FROM inventory_transactions WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getInventoryTransactions(institutionId: String): Flow<List<InventoryTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryTransaction(txn: InventoryTransactionEntity)

    // =========================================================================
    // 37. SUPPORT TICKETS
    // =========================================================================
    @Query("SELECT * FROM support_tickets WHERE institutionId = :institutionId ORDER BY createdAt DESC")
    fun getSupportTickets(institutionId: String): Flow<List<SupportTicketEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupportTicket(ticket: SupportTicketEntity)

    // =========================================================================
    // 38. SCHOOL SETTINGS & FEATURE FLAGS
    // =========================================================================
    @Query("SELECT * FROM school_settings WHERE institutionId = :institutionId")
    fun getSchoolSettings(institutionId: String): Flow<List<SchoolSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchoolSetting(setting: SchoolSettingsEntity)

    @Query("SELECT * FROM feature_flags WHERE institutionId = :institutionId")
    fun getFeatureFlags(institutionId: String): Flow<List<FeatureFlagEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatureFlag(flag: FeatureFlagEntity)

    // =========================================================================
    // 39. ENTERPRISE DURABLE OUTBOX QUEUE
    // =========================================================================
    @Query("SELECT * FROM durable_outbox WHERE institutionId = :institutionId AND (status = 'PENDING' OR status = 'FAILED' OR status = 'IN_FLIGHT' OR status = 'PROCESSING' OR status = 'RETRYABLE_FAILURE' OR status = 'PERMANENT_FAILURE' OR status = 'CONFLICT') ORDER BY createdAt ASC")
    fun getPendingOutbox(institutionId: String): Flow<List<DurableOutboxEntity>>

    @Query("SELECT * FROM durable_outbox WHERE institutionId = :institutionId AND (status = 'PENDING' OR status = 'RETRYABLE_FAILURE' OR status = 'FAILED') AND nextAttemptAt <= :now ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getReadyOutboxItems(institutionId: String, now: Long, limit: Int = 50): List<DurableOutboxEntity>

    @Query("SELECT * FROM durable_outbox WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getOutboxByIdempotencyKey(key: String): DurableOutboxEntity?

    @Query("SELECT DISTINCT institutionId FROM durable_outbox WHERE status IN ('PENDING', 'RETRYABLE_FAILURE', 'FAILED')")
    suspend fun getPendingOutboxInstitutionIds(): List<String>

    @Query("SELECT COUNT(*) FROM durable_outbox WHERE institutionId = :institutionId AND (status = 'PENDING' OR status = 'FAILED' OR status = 'RETRYABLE_FAILURE' OR status = 'PROCESSING')")
    fun getPendingOutboxCount(institutionId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutbox(item: DurableOutboxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutboxItems(items: List<DurableOutboxEntity>)

    @Update
    suspend fun updateOutbox(item: DurableOutboxEntity)

    @Query("UPDATE durable_outbox SET status = 'PROCESSING', lastAttemptAt = :attemptTime WHERE outboxId = :outboxId")
    suspend fun markOutboxItemProcessing(outboxId: String, attemptTime: Long)

    @Query("UPDATE durable_outbox SET status = 'ACKNOWLEDGED', serverAckToken = :ackToken, serverAckTimestamp = :ackTimestamp, errorMessage = null WHERE outboxId = :outboxId")
    suspend fun acknowledgeOutboxItem(outboxId: String, ackToken: String, ackTimestamp: Long)

    @Query("UPDATE durable_outbox SET status = :status, errorMessage = :error, retryCount = :retryCount, nextAttemptAt = :nextAttempt, lastAttemptAt = :lastAttempt WHERE outboxId = :outboxId")
    suspend fun markOutboxItemFailedWithStatus(outboxId: String, status: String, error: String, retryCount: Int, nextAttempt: Long, lastAttempt: Long)

    @Query("UPDATE durable_outbox SET status = 'FAILED', errorMessage = :error, retryCount = :retryCount, nextAttemptAt = :nextAttempt, lastAttemptAt = :lastAttempt WHERE outboxId = :outboxId")
    suspend fun markOutboxItemFailed(outboxId: String, error: String, retryCount: Int, nextAttempt: Long, lastAttempt: Long)

    @Query("UPDATE durable_outbox SET status = 'CONFLICT', errorMessage = :error WHERE outboxId = :outboxId")
    suspend fun markOutboxItemConflict(outboxId: String, error: String)

    @Query("DELETE FROM durable_outbox WHERE outboxId = :outboxId")
    suspend fun deleteOutboxItem(outboxId: String)

    @Query("DELETE FROM durable_outbox WHERE status = 'ACKNOWLEDGED' AND serverAckTimestamp < :cutoffTime")
    suspend fun purgeOldAcknowledgedOutbox(cutoffTime: Long)

    // =========================================================================
    // 40. SECURE CLOUD DOCUMENT STORAGE
    // =========================================================================
    @Query("SELECT * FROM cloud_documents WHERE institutionId = :institutionId AND ownerId = :ownerId ORDER BY uploadedTimestamp DESC")
    fun getDocumentsForOwner(institutionId: String, ownerId: String): Flow<List<CloudDocumentEntity>>

    @Query("SELECT * FROM cloud_documents WHERE institutionId = :institutionId AND category = :category ORDER BY uploadedTimestamp DESC")
    fun getDocumentsByCategory(institutionId: String, category: String): Flow<List<CloudDocumentEntity>>

    @Query("SELECT * FROM cloud_documents WHERE institutionId = :institutionId ORDER BY uploadedTimestamp DESC")
    fun getAllDocuments(institutionId: String): Flow<List<CloudDocumentEntity>>

    @Query("SELECT * FROM cloud_documents WHERE documentId = :documentId LIMIT 1")
    suspend fun getDocumentById(documentId: String): CloudDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: CloudDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<CloudDocumentEntity>)

    @Query("UPDATE cloud_documents SET isVerified = :isVerified, verifiedByUserId = :verifierId WHERE documentId = :documentId")
    suspend fun verifyDocument(documentId: String, isVerified: Boolean, verifierId: String)

    @Query("DELETE FROM cloud_documents WHERE documentId = :documentId")
    suspend fun deleteDocument(documentId: String)

    // =========================================================================
    // 41. FCM NOTIFICATION PREFERENCES
    // =========================================================================
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    fun getNotificationPreferences(userId: String): Flow<NotificationPreferenceEntity?>

    @Query("SELECT * FROM notification_preferences WHERE userId = :userId LIMIT 1")
    suspend fun getNotificationPreferencesDirect(userId: String): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificationPreferences(prefs: NotificationPreferenceEntity)

    // =========================================================================
    // 42. DISASTER RECOVERY & AUTOMATED BACKUP AUDIT
    // =========================================================================
    @Query("SELECT * FROM disaster_recovery_backups WHERE institutionId = :institutionId ORDER BY snapshotTimestamp DESC")
    fun getAllBackups(institutionId: String): Flow<List<DisasterRecoveryBackupEntity>>

    @Query("SELECT * FROM disaster_recovery_backups WHERE backupId = :backupId LIMIT 1")
    suspend fun getBackupById(backupId: String): DisasterRecoveryBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: DisasterRecoveryBackupEntity)

    @Query("UPDATE disaster_recovery_backups SET restorationTested = :tested, restoreTestedTimestamp = :testedTime WHERE backupId = :backupId")
    suspend fun markBackupRestoreTested(backupId: String, tested: Boolean, testedTime: Long)

    // =========================================================================
    // 43. ACADEMIC PROMOTION HISTORY & SESSION ISOLATION
    // =========================================================================
    @Query("SELECT * FROM academic_promotion_history WHERE institutionId = :institutionId AND studentId = :studentId ORDER BY timestamp DESC")
    fun getPromotionHistoryForStudent(institutionId: String, studentId: String): Flow<List<AcademicPromotionHistoryEntity>>

    @Query("SELECT * FROM academic_promotion_history WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getAllPromotionHistory(institutionId: String): Flow<List<AcademicPromotionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionRecord(record: AcademicPromotionHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromotionRecords(records: List<AcademicPromotionHistoryEntity>)

    @Query("UPDATE academic_years SET isLocked = :isLocked WHERE academicYearId = :yearId")
    suspend fun setAcademicYearLock(yearId: String, isLocked: Boolean)

    @Query("UPDATE academic_years SET isCurrent = CASE WHEN academicYearId = :yearId THEN 1 ELSE 0 END WHERE institutionId = :institutionId")
    suspend fun setCurrentAcademicYear(institutionId: String, yearId: String)

    @Transaction
    suspend fun promoteStudentWithAudit(
        oldEnrollmentId: String,
        newEnrollment: StudentEnrollmentEntity,
        promotionRecord: AcademicPromotionHistoryEntity,
        auditLog: AuditLogEntity
    ) {
        updateStudentEnrollmentStatus(oldEnrollmentId, "PROMOTED")
        insertEnrollment(newEnrollment)
        insertPromotionRecord(promotionRecord)
        insertAuditLog(auditLog)
    }

    @Query("UPDATE student_enrollments SET enrollmentStatus = :status WHERE enrollmentId = :enrollmentId")
    suspend fun updateStudentEnrollmentStatus(enrollmentId: String, status: String)

    @Query("SELECT * FROM student_enrollments WHERE academicYear = :academicYear AND institutionId = :institutionId")
    fun getEnrollmentsForAcademicYear(academicYear: String, institutionId: String): Flow<List<StudentEnrollmentEntity>>

    // =========================================================================
    // 44. ZERO-TRUST PAYMENT ORDERS (FEES & SAAS SUBSCRIPTION BILLING)
    // =========================================================================
    @Query("SELECT * FROM payment_orders WHERE orderId = :orderId LIMIT 1")
    suspend fun getPaymentOrderById(orderId: String): PaymentOrderEntity?

    @Query("SELECT * FROM payment_orders WHERE invoiceId = :invoiceId LIMIT 1")
    suspend fun getPaymentOrderByInvoiceId(invoiceId: String): PaymentOrderEntity?

    @Query("SELECT * FROM payment_orders WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getPaymentOrderByIdempotencyKey(key: String): PaymentOrderEntity?

    @Query("SELECT * FROM payment_orders WHERE institutionId = :institutionId ORDER BY createdAt DESC")
    fun getPaymentOrders(institutionId: String): Flow<List<PaymentOrderEntity>>

    @Query("SELECT * FROM payment_orders WHERE institutionId = :institutionId AND orderType = :orderType ORDER BY createdAt DESC")
    fun getPaymentOrdersByType(institutionId: String, orderType: String): Flow<List<PaymentOrderEntity>>

    @Query("SELECT * FROM payment_orders WHERE institutionId = :institutionId AND payerUserId = :payerUserId ORDER BY createdAt DESC")
    fun getPaymentOrdersForPayer(institutionId: String, payerUserId: String): Flow<List<PaymentOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentOrder(order: PaymentOrderEntity)

    @Query("UPDATE payment_orders SET status = :status, completedAt = :completedAt, failureReason = :failureReason, gatewayPaymentId = :gatewayPaymentId WHERE orderId = :orderId")
    suspend fun updatePaymentOrderStatus(orderId: String, status: String, completedAt: Long?, failureReason: String?, gatewayPaymentId: String?)

    // =========================================================================
    // 45. DOUBLE-ENTRY FINANCIAL LEDGER
    // =========================================================================
    @Query("SELECT * FROM financial_ledger WHERE institutionId = :institutionId AND ledgerDomain = :ledgerDomain ORDER BY timestamp DESC")
    fun getLedgerEntries(institutionId: String, ledgerDomain: String): Flow<List<FinancialLedgerEntryEntity>>

    @Query("SELECT * FROM financial_ledger WHERE institutionId = :institutionId ORDER BY timestamp DESC")
    fun getAllLedgerEntries(institutionId: String): Flow<List<FinancialLedgerEntryEntity>>

    @Query("SELECT balanceAfterPaise FROM financial_ledger WHERE institutionId = :institutionId AND accountType = :accountType ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLedgerBalance(institutionId: String, accountType: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entry: FinancialLedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(entries: List<FinancialLedgerEntryEntity>)

    // =========================================================================
    // 46. AUTOMATED MESSAGE DISPATCH LOGS
    // =========================================================================
    @Query("SELECT * FROM automated_dispatch_logs WHERE institutionId = :institutionId AND status = 'QUEUED' ORDER BY priority DESC, dispatchId ASC LIMIT :limit")
    suspend fun getQueuedDispatches(institutionId: String, limit: Int = 50): List<AutomatedMessageDispatchEntity>

    @Query("SELECT * FROM automated_dispatch_logs WHERE institutionId = :institutionId ORDER BY dispatchId DESC")
    fun getDispatchHistory(institutionId: String): Flow<List<AutomatedMessageDispatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispatchLog(dispatch: AutomatedMessageDispatchEntity)

    @Query("UPDATE automated_dispatch_logs SET status = :status, dispatchedAt = :dispatchedAt, deliveredAt = :deliveredAt, errorMessage = :errorMessage, attemptCount = :attemptCount WHERE dispatchId = :dispatchId")
    suspend fun updateDispatchStatus(dispatchId: String, status: String, dispatchedAt: Long?, deliveredAt: Long?, errorMessage: String?, attemptCount: Int)
}
