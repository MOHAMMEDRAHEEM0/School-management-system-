package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// =========================================================================
// 1. INSTITUTION MASTER PROFILE (One-Time Setup with Unique Institution ID)
// =========================================================================

@Entity(tableName = "institutions")
data class InstitutionEntity(
    @PrimaryKey val institutionId: String = "INST-SXA-2026",
    val registrationCode: String = "3477",
    val name: String = "St. Xavier's International Academy",
    val motto: String = "Excellence in Truth & Service",
    val affiliationCode: String = "CBSE Affiliation No. 1130492 / Autonomous Board",
    val address: String = "Campus Road, Civil Lines, Sector 5",
    val cityCampus: String = "Civil Lines, Sector 5",
    val contactPhone: String = "+91 98111 22334",
    val email: String = "office@stxaviers.edu",
    val website: String = "https://stxaviers.edu",
    val currentAcademicYear: String = "2026-2027",
    val availableBatches: String = "2026-27,2025-26,2024-25",
    val principalName: String = "Prof. Robert Vance, Ph.D.",
    val themeColorHex: String = "#0B2545",
    val logoAssetPath: String = "ic_school_crest",
    val establishedYear: Int = 1984,
    val isSetupCompleted: Boolean = true
)

// =========================================================================
// 2. ACADEMIC STRUCTURE (Classes, Sections, Pre-fed Database)
// =========================================================================

@Entity(tableName = "academic_classes")
data class AcademicClassEntity(
    @PrimaryKey val classId: String, // e.g. "CLS-10A", "CLS-NUR-A", "CLS-1A"
    val institutionId: String = "INST-SXA-2026",
    val className: String, // e.g. "Grade 10", "Grade 9", "UKG", "LKG", "Nursery"
    val section: String, // e.g. "A", "B", "C", "D"
    val classTeacherId: String? = null,
    val classTeacherName: String? = null,
    val roomNumber: String = "Room 304",
    val studentCount: Int = 30,
    val academicYear: String = "2026-2027",
    val isArchived: Boolean = false
)

// =========================================================================
// 3. CORE USERS & MULTI-LEVEL RBAC (Enforced at DB level)
// =========================================================================

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String, // "USR-PRIN-01", "USR-TCH-01", "USR-STU-01"
    val institutionId: String = "INST-SXA-2026",
    val username: String,
    val email: String,
    val password: String = "",
    val passwordSalt: String = "",
    val passwordHash: String = "",
    val role: String, // ADMIN, PRINCIPAL, VICE_PRINCIPAL, INCHARGE, STAFF_MANAGER, TEACHER, TEACHING, ACCOUNTANT, OFFICE, STUDENT, PARENT, NON_TEACHING, LIBRARIAN, TRANSPORT_STAFF
    val displayName: String,
    val phoneNumber: String = "",
    val authPin: String = "",
    val isOtpVerified: Boolean = true,
    val isActive: Boolean = true,
    val isAccountDisabled: Boolean = false,
    val permissions: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)
typealias User = UserEntity

@Entity(tableName = "teachers")
data class TeacherEntity(
    @PrimaryKey val id: String, // e.g. "FAC-TCH-01"
    val institutionId: String = "INST-SXA-2026",
    val name: String,
    val email: String,
    val phone: String,
    val department: String,
    val designation: String,
    val qualification: String,
    val experienceYears: Int,
    val isClassIncharge: Boolean = false,
    val inchargeClassSection: String? = null,
    val assignedSubjects: String = "Mathematics, Physics",
    val govtRegNumber: String = "SXA-FAC-001",
    val roleType: String = "TEACHING",
    val joinedDate: String = "2026-08-01",
    val joinedTimestamp: Long = 1785542400000L
)
typealias Teacher = TeacherEntity

@Entity(tableName = "staff_members")
data class StaffEntity(
    @PrimaryKey val id: String, // e.g. "FAC-PRIN-01", "FAC-TCH-01"
    val institutionId: String = "INST-SXA-2026",
    val name: String,
    val roleType: String, // "PRINCIPAL", "VICE_PRINCIPAL", "INCHARGE", "STAFF_MANAGER", "TEACHING", "ACCOUNTANT", "OFFICE", "NON_TEACHING"
    val designation: String,
    val department: String,
    val phone: String, // Unique authorized phone number
    val email: String,
    val qualification: String,
    val experienceYears: Int,
    val govtRegNumber: String = "SXA-FAC-001",
    val isPreAuthorizedByPrincipal: Boolean = true,
    val isOtpVerified: Boolean = false,
    val authPin: String = "1234",
    val isPresentToday: Boolean = true,
    val checkInTime: String = "08:00 AM",
    val avatarColorHex: String = "#0B2545",
    val isClassIncharge: Boolean = false,
    val inchargeClass: String? = null,
    val assignedSubjects: String = "Mathematics",
    val idCardPhotoUri: String? = null,
    val convertedDigitalIdNumber: String? = null,
    val idCardStatus: String = "APPROVED_BY_PRINCIPAL",
    val idApprovedDate: String? = "2026-09-12",
    val idApprovedBy: String? = "Prof. Robert Vance (Principal)"
)

// =========================================================================
// 4. STUDENTS (Enrolled Scholar Data with Privacy Isolation)
// =========================================================================

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val id: String, // e.g. "ADM-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val admissionNumber: String = id,
    val name: String,
    val grade: String,
    val section: String,
    val rollNumber: Int,
    val gender: String = "Male",
    val dateOfBirth: String = "2011-04-12",
    val guardianName: String = "Guardian",
    val guardianPhone: String = "9876543210",
    val studentPhone: String = guardianPhone,
    val email: String = "student@school.edu",
    val bloodGroup: String = "B+",
    val attendancePercentage: Float = 95.0f,
    val feeDue: Double = 0.0,
    val feePaid: Double = 35000.0,
    val avatarColorHex: String = "#0B2545",
    val address: String = "Civil Lines, Sector 5",
    val realIdPhotoUri: String? = null,
    val convertedDigitalId: String = "SXA-DIGI-STU-001",
    val digitalIdStatus: String = "APPROVED_BY_PRINCIPAL",
    val academicYear: String = "2026-2027",
    val promotionStatus: String = "CURRENT" // CURRENT, PROMOTED, GRADUATED, ARCHIVED
)
typealias Student = StudentEntity

// =========================================================================
// 5. ATTENDANCE SYSTEM (Roll-Call, Camera OCR, Incharge Check, Principal Seal)
// =========================================================================

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val studentName: String,
    val classSection: String,
    val date: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val status: String, // "PRESENT", "ABSENT", "LATE", "LEAVE"
    val session: String = "MORNING_ASSEMBLY",
    val markedByTeacherId: String = "FAC-TCH-01",
    val remarks: String = "",
    val isVerifiedByIncharge: Boolean = true,
    val isApprovedByPrincipal: Boolean = true
)
typealias Attendance = AttendanceEntity

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val recordId: Long = 0,
    val institutionId: String = "INST-SXA-2026",
    val personId: String,
    val personName: String,
    val roleType: String, // "STUDENT", "TEACHING", "NON_TEACHING"
    val date: String,
    val status: String, // "PRESENT", "ABSENT", "LATE", "LEAVE"
    val timeMarked: String,
    val remarks: String = "",
    val markedByRole: String = "TEACHER",
    val markedByName: String = "Dr. Meenakshi Sundaram",
    val classSection: String = "10-A",
    val sessionType: String = "Morning Assembly Roll Call",
    val sourceMethod: String = "CAMERA_REGISTER_OCR",
    val isSyncedToCentral: Boolean = true
)

@Entity(tableName = "register_scan_batches")
data class RegisterScanBatchEntity(
    @PrimaryKey val batchId: String,
    val institutionId: String = "INST-SXA-2026",
    val classSection: String,
    val session: String,
    val date: String,
    val teacherName: String,
    val totalStudents: Int,
    val presentCount: Int,
    val absentCount: Int,
    val absentRolls: String,
    val inchargeVerified: Boolean = true,
    val principalApproved: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMMITTED_CENTRAL"
)

// =========================================================================
// 6. EXAMINATIONS & REPORT CARDS (Subject-level marks, grades, report card)
// =========================================================================

@Entity(tableName = "exam_records")
data class ExamRecordEntity(
    @PrimaryKey val examId: String = "EXAM-${System.currentTimeMillis()}", // e.g. "EXAM-10A-MATH-001"
    val institutionId: String = "INST-SXA-2026",
    val academicYear: String = "2026-2027",
    val examType: String = "UNIT_TEST", // "UNIT_TEST", "FORMATIVE_ASSESSMENT", "SUMMATIVE_ASSESSMENT", "HALF_YEARLY", "FINAL_EXAM"
    val examName: String = "Term 1 Assessment", // "Term 1 Summative Assessment"
    val classSection: String = "10-A", // "10-A"
    val subject: String, // "Mathematics", "Physics", "English"
    val studentId: String,
    val studentName: String = "Scholar",
    val rollNumber: Int = 1,
    val marksObtained: Float,
    val maxMarks: Float = 100f,
    val grade: String = "A",
    val teacherRemarks: String = "Good academic performance",
    val enteredByStaffId: String = "FAC-TCH-02",
    val isPublished: Boolean = true
) {
    val id: String get() = examId
    val percentage: Float get() = if (maxMarks > 0) (marksObtained / maxMarks) * 100f else 0f

    constructor(
        id: String,
        studentId: String,
        subject: String,
        marksObtained: Double,
        maxMarks: Double = 100.0,
        grade: String = "A",
        term: String = "Term 1"
    ) : this(
        examId = id,
        subject = subject,
        studentId = studentId,
        marksObtained = marksObtained.toFloat(),
        maxMarks = maxMarks.toFloat(),
        grade = grade,
        examName = term
    )
}

// =========================================================================
// 7. FEE MANAGEMENT & RECEIPTS (Unique receipt numbers, official printable layout)
// =========================================================================

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val admissionNumber: String = studentId,
    val studentName: String,
    val gradeSection: String,
    val term: String = "Term 1: 2026-27", // "Term 1: 2026-27"
    val feeType: String = "Composite Tuition & Lab Fee", // "Composite Tuition & Lab Fee"
    val totalAmount: Long = 4500000L, // In paise/cents to prevent floating point error
    val paidAmount: Long = 4500000L,
    val dueAmount: Long = 0L,
    val status: String = "PAID", // "PAID", "PARTIAL", "DUE", "OVERDUE"
    val receiptNumber: String? = null,
    val paidDate: String? = null,
    val paymentMode: String = "ONLINE", // "ONLINE", "CHEQUE", "CASH", "BANK_TRANSFER"
    val verifiedByIncharge: Boolean = true
) {
    val totalRupees: Long get() = if (totalAmount >= 100000L) totalAmount / 100L else totalAmount
    val paidRupees: Long get() = if (paidAmount >= 100000L) paidAmount / 100L else paidAmount
    val dueRupees: Long get() = if (dueAmount >= 100000L) dueAmount / 100L else dueAmount

    constructor(
        id: String,
        studentId: String,
        studentName: String,
        gradeSection: String,
        totalRupees: Double,
        paidRupees: Double,
        dueRupees: Double,
        status: String,
        term: String
    ) : this(
        id = id,
        studentId = studentId,
        studentName = studentName,
        gradeSection = gradeSection,
        term = term,
        totalAmount = (totalRupees * 100).toLong(),
        paidAmount = (paidRupees * 100).toLong(),
        dueAmount = (dueRupees * 100).toLong(),
        status = status
    )
}

// =========================================================================
// 8. CENTRALIZED DOCUMENT MANAGEMENT & CERTIFICATES
// =========================================================================

@Entity(tableName = "certificates")
data class CertificateEntity(
    @PrimaryKey val certificateId: String,
    val institutionId: String = "INST-SXA-2026",
    val certificateType: String, // "BONAFIDE", "TRANSFER", "CONDUCT", "CHARACTER", "MERIT", "FEE"
    val studentName: String,
    val studentId: String,
    val grade: String,
    val issueDate: String,
    val academicYear: String,
    val purposeOrAchievement: String,
    val verifiedBy: String = "Prof. Robert Vance (Principal)",
    val digitalBarcode: String = "SXA-CERT-9942-VERIFIED",
    val applicationStatus: String = "APPROVED_BY_PRINCIPAL", // "PENDING_INCHARGE", "VERIFIED_BY_INCHARGE", "APPROVED_BY_PRINCIPAL", "REJECTED"
    val appliedBy: String = "Student",
    val appliedDate: String = "2026-09-10",
    val verifiedByIncharge: String = "Mrs. Sunita Kapoor (Class Incharge)",
    val approvedByPrincipal: String = "Prof. Robert Vance (Principal)",
    val inchargeRemarks: String = "Attendance above 75%, Conduct exemplary",
    val principalRemarks: String = "Approved under Executive Master Seal",
    val isDownloadUnlocked: Boolean = true,
    val secureWhatsAppToken: String = "WHT-SXA-CERT-7731"
)

// =========================================================================
// 9. COMPUTER-GENERATED DIGITAL ID CARDS (Students & Staff)
// =========================================================================

@Entity(tableName = "id_card_records")
data class IdCardRecordEntity(
    @PrimaryKey val cardId: String,
    val institutionId: String = "INST-SXA-2026",
    val personId: String,
    val personName: String,
    val roleCategory: String, // "TEACHING", "NON_TEACHING", "STUDENT"
    val designationOrGrade: String,
    val department: String,
    val phone: String,
    val bloodGroup: String = "O+",
    val capturedRealPhotoUri: String? = null,
    val convertedDigitalCardId: String,
    val barcodeNumber: String,
    val issueDate: String,
    val validityText: String = "2026-2027 (PERMANENT OFFICIAL RECORD)",
    val verificationStatus: String, // "PENDING_INCHARGE", "VERIFIED_BY_INCHARGE", "APPROVED_BY_PRINCIPAL"
    val inchargeRemarks: String = "",
    val principalRemarks: String = "",
    val approvedBy: String = "Prof. Robert Vance (Principal)",
    val securitySealNumber: String = "SXA-SEAL-9984"
)

// =========================================================================
// 10. ADMISSIONS MANAGEMENT SYSTEM (Applications, Verification, Enrollment)
// =========================================================================

@Entity(tableName = "admission_applications")
data class AdmissionApplicationEntity(
    @PrimaryKey val applicationId: String, // e.g. "ADM-APP-2026-081"
    val institutionId: String = "INST-SXA-2026",
    val studentName: String,
    val dob: String,
    val gender: String,
    val requestedClass: String, // e.g. "Grade 1", "Grade 10"
    val guardianName: String,
    val guardianPhone: String,
    val guardianEmail: String,
    val address: String,
    val previousSchool: String,
    val status: String = "PENDING_VERIFICATION", // "PENDING_VERIFICATION", "VERIFIED_OFFICE", "APPROVED_BY_PRINCIPAL", "REJECTED"
    val applicationDate: String = "2026-09-14",
    val assignedAdmissionNumber: String? = null,
    val assignedClassSection: String? = null,
    val remarks: String = "Documents submitted: Birth Certificate, Previous TC"
)

// =========================================================================
// 11. TIMETABLE SYSTEM & CONFLICT DETECTION
// =========================================================================

@Entity(tableName = "timetable_slots")
data class TimetableSlotEntity(
    @PrimaryKey val slotId: String, // e.g. "TT-10A-MON-P1"
    val institutionId: String = "INST-SXA-2026",
    val classSection: String, // "10-A"
    val dayOfWeek: String, // "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
    val periodNumber: Int, // 1 to 8
    val timeRange: String, // "08:30 - 09:15 AM"
    val subject: String, // "Mathematics", "Physics", "English"
    val teacherName: String, // "Dr. Meenakshi Sundaram"
    val teacherId: String, // "FAC-TCH-01"
    val roomNumber: String = "Room 304"
)

// =========================================================================
// 12. CIRCULARS & NOTICES & ANNOUNCEMENTS
// =========================================================================

@Entity(tableName = "notices_and_meetings")
data class NoticeMeetingEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val title: String,
    val type: String, // "CIRCULAR", "MEETING", "HOLIDAY", "EXAM_ALERT"
    val date: String,
    val time: String,
    val venueOrAudience: String, // "ALL", "TEACHING_STAFF", "PARENTS", "STUDENTS"
    val description: String,
    val isUrgent: Boolean = false,
    val organizer: String = "Office of the Principal, St. Xavier's International Academy"
)

@Entity(tableName = "circulars")
data class CircularEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val title: String,
    val content: String,
    val targetRole: String = "ALL",
    val category: String = "GENERAL",
    val issuedDate: String,
    val issuedTimestamp: Long = System.currentTimeMillis(),
    val isUrgent: Boolean = false,
    val issuedBy: String = "Office of the Principal, St. Xavier's International Academy"
)
typealias Circular = CircularEntity

// =========================================================================
// 13. DIGITAL SCHOOL DIARY & INSTRUCTIONS
// =========================================================================

@Entity(tableName = "school_diary")
data class SchoolDiaryEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val date: String,
    val classSection: String,
    val subject: String,
    val topicCovered: String,
    val homeworkAssigned: String,
    val specialInstructions: String = "Complete exercises in practice notebook.",
    val teacherName: String = "Dr. Meenakshi Sundaram",
    val inchargeVerified: Boolean = true
)

@Entity(tableName = "school_instructions")
data class SchoolInstructionEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val title: String,
    val category: String, // "GENERAL_REGULATIONS", "BELL_SCHEDULE", "UNIFORM_CODE", "EXAMINATION_CODE"
    val content: String,
    val orderIndex: Int
)

// =========================================================================
// 14. INSTITUTIONAL BILLING & GOVERNMENT COMPLIANCE
// =========================================================================

@Entity(tableName = "institutional_bills")
data class InstitutionalBillEntity(
    @PrimaryKey val billId: String, // e.g. "BILL-ELEC-2026-08"
    val institutionId: String = "INST-SXA-2026",
    val billType: String, // "ELECTRICITY", "WATER", "INTERNET", "TELEPHONE", "RENT", "MAINTENANCE"
    val vendorName: String,
    val billNumber: String,
    val billingMonth: String,
    val amountRupees: Long,
    val dueDate: String,
    val paidDate: String? = null,
    val paymentStatus: String = "PAID", // "PAID", "PENDING"
    val receiptRef: String = "DISCOM-RCP-9921"
)

@Entity(tableName = "government_compliance")
data class GovernmentComplianceEntity(
    @PrimaryKey val complianceId: String, // e.g. "COMPL-FIRE-2026"
    val institutionId: String = "INST-SXA-2026",
    val documentTitle: String, // e.g. "State Fire Safety Clearance Certificate"
    val authorityName: String, // "Municipal Fire Services"
    val registrationNumber: String,
    val issueDate: String,
    val expiryDate: String,
    val renewalReminderDays: Int = 30,
    val status: String = "ACTIVE", // "ACTIVE", "EXPIRING_SOON", "RENEWED"
    val responsibleStaff: String = "Rajesh Verma (Campus Operations Head)"
)

// =========================================================================
// 15. OFFLINE SYNCHRONIZATION QUEUE & CONFLICT HANDLING
// =========================================================================

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val syncId: Long = 0,
    val institutionId: String = "INST-SXA-2026",
    val entityType: String, // "ATTENDANCE", "MARKS", "DIARY", "FEE", "CERTIFICATE"
    val entityId: String,
    val operation: String, // "INSERT", "UPDATE", "DELETE"
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCING", "SYNCED", "FAILED"
    val errorMessage: String? = null
)

// =========================================================================
// 16. INCHARGE EDIT REQUESTS & NON-REPUDIABLE AUDIT LOGS
// =========================================================================

@Entity(tableName = "incharge_edit_requests")
data class InchargeEditRequestEntity(
    @PrimaryKey val requestId: String,
    val inchargeId: String,
    val inchargeName: String,
    val classSection: String,
    val targetType: String,
    val targetId: String,
    val targetName: String,
    val descriptionOfEdit: String,
    val proposedValue: String,
    val status: String = "PENDING_PRINCIPAL_AUTH",
    val requestTimestamp: Long = System.currentTimeMillis(),
    val principalAuthTimestamp: Long? = null,
    val principalRemarks: String = ""
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val institutionId: String = "INST-SXA-2026",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "2026-09-15T08:30:00Z",
    val action: String,
    val entityType: String,
    val entityId: String,
    val actorUserId: String,
    val actorName: String,
    val actorRole: String,
    val previousState: String?,
    val newState: String,
    val remarks: String = "",
    val securityVerificationHash: String = "SXA-HASH-${System.currentTimeMillis() % 100000}",
    val previousHash: String = "GENESIS_HASH_0000000000000000",
    val currentHash: String = "",
    val syncStatus: String = "PENDING_SYNC",
    val syncTimestamp: Long? = null,
    val encryptedPayload: String? = null,
    val interactionType: String = "DATABASE_TX"
)

// =========================================================================
// 17. HOMEWORK & ASSIGNMENT MANAGEMENT
// =========================================================================

@Entity(tableName = "homework_assignments")
data class HomeworkEntity(
    @PrimaryKey val homeworkId: String, // e.g. "HW-10A-MATH-001"
    val institutionId: String = "INST-SXA-2026",
    val academicYear: String = "2026-2027",
    val classSection: String, // "10-A"
    val subject: String, // "Mathematics"
    val title: String,
    val description: String,
    val teacherId: String = "FAC-TCH-02",
    val teacherName: String = "Mrs. Sunita Kapoor",
    val assignedDate: String = "2026-09-24",
    val dueDate: String = "2026-09-28",
    val maxMarks: Int = 20,
    val isPublished: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "homework_submissions")
data class HomeworkSubmissionEntity(
    @PrimaryKey val submissionId: String, // e.g. "SUB-HW-001-STU-001"
    val homeworkId: String,
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val studentName: String,
    val classSection: String = "10-A",
    val submissionText: String,
    val submissionTimestamp: Long = System.currentTimeMillis(),
    val status: String = "SUBMITTED", // "SUBMITTED", "EVALUATED", "LATE"
    val marksObtained: Int? = null,
    val teacherFeedback: String? = null,
    val gradedAt: Long? = null
)

// =========================================================================
// 18. STUDENT & STAFF LEAVE REQUESTS
// =========================================================================

@Entity(tableName = "leave_requests")
data class StudentLeaveRequestEntity(
    @PrimaryKey val leaveId: String, // e.g. "LEV-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val studentName: String,
    val classSection: String = "10-A",
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: String = "PENDING", // "PENDING", "APPROVED", "REJECTED"
    val appliedBy: String = "PARENT", // "PARENT", "STUDENT"
    val appliedTimestamp: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null,
    val reviewerRemarks: String? = null,
    val reviewedTimestamp: Long? = null
)

// =========================================================================
// 19. CONFIGURABLE FEE STRUCTURES & TRANSACTIONS
// =========================================================================

@Entity(tableName = "fee_structures")
data class FeeStructureEntity(
    @PrimaryKey val structureId: String, // e.g. "FEE-STR-10-2026"
    val institutionId: String = "INST-SXA-2026",
    val academicYear: String = "2026-2027",
    val grade: String = "10",
    val feeHead: String = "Composite Tuition & Laboratory Fee",
    val term: String = "Term 1",
    val amountPaise: Long = 4500000L, //  45,000.00
    val dueDate: String = "2026-10-15",
    val isMandatory: Boolean = true
)

@Entity(tableName = "fee_payment_transactions")
data class FeePaymentTransactionEntity(
    @PrimaryKey val transactionId: String, // e.g. "TXN-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val feeRecordId: String,
    val studentId: String,
    val studentName: String,
    val amountPaidPaise: Long,
    val paymentMode: String = "ONLINE", // "ONLINE", "CASH", "CHEQUE", "BANK_TRANSFER"
    val transactionReference: String,
    val receiptNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String = "Suresh Mehta (Accounts Bursar)",
    val status: String = "SUCCESS" // "SUCCESS", "RECONCILED", "REFUNDED"
)

// =========================================================================
// 20. PUSH NOTIFICATIONS & MESSAGING
// =========================================================================

@Entity(tableName = "push_notifications")
data class PushNotificationEntity(
    @PrimaryKey val notificationId: String, // e.g. "NOTIF-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val recipientUserId: String = "ALL", // "ALL", specific userId, or role "TEACHERS"
    val title: String,
    val body: String,
    val category: String = "ANNOUNCEMENT", // "ANNOUNCEMENT", "ATTENDANCE_ALERT", "EXAM", "FEE_DUE"
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val deepLink: String? = null
)

// =========================================================================
// 21. AI ACADEMIC ASSISTANT AUDIT & TOKEN TRACKING
// =========================================================================

@Entity(tableName = "ai_audit_logs")
data class AiAuditLogEntity(
    @PrimaryKey val queryId: String, // e.g. "AI-LOG-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val actorUserId: String,
    val actorRole: String,
    val queryType: String, // "ATTENDANCE_SUMMARY", "DEFICIT_ANALYSIS", "PARENT_MESSAGE_DRAFT", "STUDY_GUIDE"
    val promptSummary: String,
    val tokenEstimate: Int = 150,
    val timestamp: Long = System.currentTimeMillis(),
    val wasSuccessful: Boolean = true,
    val securityContextHash: String = "SEC-AI-OK"
)

// =========================================================================
// 22. SUBJECT MASTER & TEACHER ASSIGNMENT
// =========================================================================

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String, // e.g. "SUBJ-10A-MATH"
    val institutionId: String = "INST-SXA-2026",
    val classSection: String = "10-A",
    val subjectName: String = "Mathematics",
    val subjectCode: String = "MATH-10",
    val department: String = "Mathematical Sciences",
    val facultyStaffId: String = "FAC-TCH-02",
    val facultyName: String = "Mrs. Sunita Kapoor",
    val weeklyPeriods: Int = 6
)

// =========================================================================
// 23. PARENT-WARD LINK (MULTI-STUDENT PARENT ACCESS)
// =========================================================================

@Entity(tableName = "parent_ward_links")
data class ParentWardLinkEntity(
    @PrimaryKey val linkId: String, // e.g. "LINK-PAR-STU-001"
    val institutionId: String = "INST-SXA-2026",
    val parentUserId: String = "USR-PAR-01",
    val studentId: String = "ADM-2026-001",
    val studentName: String = "Aarav Sharma",
    val relationship: String = "FATHER", // "FATHER", "MOTHER", "GUARDIAN"
    val isPrimaryContact: Boolean = true
)

// =========================================================================
// 24. CAMPUS MULTI-BRANCH ENTITY
// =========================================================================

@Entity(tableName = "campuses")
data class CampusEntity(
    @PrimaryKey val campusId: String, // e.g. "CAMP-MAIN", "CAMP-NORTH"
    val institutionId: String = "INST-SXA-2026",
    val campusName: String,
    val code: String,
    val address: String,
    val contactPhone: String = "+91 98111 22334",
    val headOfCampus: String = "Prof. Robert Vance, Ph.D.",
    val isActive: Boolean = true
)

// =========================================================================
// 25. ACADEMIC YEARS & SESSIONS (ISOLATION & PROMOTIONS)
// =========================================================================

@Entity(tableName = "academic_years")
data class AcademicYearEntity(
    @PrimaryKey val academicYearId: String, // e.g. "AY-2026-2027"
    val institutionId: String = "INST-SXA-2026",
    val yearName: String = "2026-2027",
    val startDate: String = "2026-04-01",
    val endDate: String = "2027-03-31",
    val isCurrent: Boolean = true,
    val isLocked: Boolean = false
)

// =========================================================================
// 26. GRANULAR ROLE PERMISSIONS (RBAC)
// =========================================================================

@Entity(tableName = "role_permissions")
data class RolePermissionEntity(
    @PrimaryKey val permissionId: String, // e.g. "PERM-TCH-MARKS_WRITE"
    val institutionId: String = "INST-SXA-2026",
    val role: String,
    val permissionKey: String,
    val isGranted: Boolean = true
)

// =========================================================================
// 27. PARENT MASTER & EMERGENCY CONTACTS
// =========================================================================

@Entity(tableName = "parents")
data class ParentEntity(
    @PrimaryKey val parentId: String, // e.g. "PAR-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val userId: String = "USR-PAR-01",
    val name: String,
    val relationship: String = "Father",
    val phone: String,
    val email: String,
    val occupation: String = "Professional",
    val address: String = "Civil Lines, Sector 5"
)

@Entity(tableName = "emergency_contacts")
data class EmergencyContactEntity(
    @PrimaryKey val contactId: String, // e.g. "EMG-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val contactName: String,
    val relationship: String,
    val phone: String,
    val priorityOrder: Int = 1
)

// =========================================================================
// 28. HISTORICAL STUDENT ENROLLMENTS
// =========================================================================

@Entity(tableName = "student_enrollments")
data class StudentEnrollmentEntity(
    @PrimaryKey val enrollmentId: String, // e.g. "ENR-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val academicYear: String = "2026-2027",
    val classId: String = "CLS-10A",
    val section: String = "A",
    val rollNumber: Int = 1,
    val enrollmentStatus: String = "ACTIVE", // ACTIVE, PROMOTED, TRANSFERRED, WITHDRAWN
    val enrollmentDate: String = "2026-04-01"
)

// =========================================================================
// 29. SECTIONS & CLASSROOMS
// =========================================================================

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val sectionId: String, // e.g. "SEC-10A"
    val institutionId: String = "INST-SXA-2026",
    val classId: String = "CLS-10",
    val sectionName: String = "A",
    val maxCapacity: Int = 40,
    val roomNumber: String = "Room 304"
)

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey val roomId: String, // e.g. "ROOM-304"
    val institutionId: String = "INST-SXA-2026",
    val campusId: String = "CAMP-MAIN",
    val roomNumber: String = "304",
    val buildingName: String = "Main Academic Wing",
    val floor: Int = 3,
    val capacity: Int = 45,
    val hasSmartBoard: Boolean = true
)

// =========================================================================
// 30. TEACHER ASSIGNMENTS
// =========================================================================

@Entity(tableName = "teacher_assignments")
data class TeacherAssignmentEntity(
    @PrimaryKey val assignmentId: String, // e.g. "TA-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val teacherId: String,
    val subjectId: String,
    val classSection: String,
    val academicYear: String = "2026-2027",
    val weeklyPeriods: Int = 6
)

// =========================================================================
// 31. ATTENDANCE CORRECTIONS & STAFF LEAVE
// =========================================================================

@Entity(tableName = "attendance_corrections")
data class AttendanceCorrectionEntity(
    @PrimaryKey val correctionId: String, // e.g. "CORR-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val attendanceRecordId: Long,
    val studentId: String,
    val date: String,
    val originalStatus: String,
    val correctedStatus: String,
    val reason: String,
    val requestedBy: String,
    val approvedBy: String? = null,
    val status: String = "APPROVED",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "staff_leave_requests")
data class StaffLeaveRequestEntity(
    @PrimaryKey val leaveId: String, // e.g. "STF-LEV-001"
    val institutionId: String = "INST-SXA-2026",
    val staffId: String,
    val staffName: String,
    val leaveType: String = "CASUAL", // CASUAL, SICK, MATERNITY, EARNED
    val startDate: String,
    val endDate: String,
    val reason: String,
    val status: String = "PENDING",
    val appliedTimestamp: Long = System.currentTimeMillis(),
    val approvedBy: String? = null,
    val remarks: String = ""
)

// =========================================================================
// 32. EXAM SCHEDULES & GRADING SCHEMES
// =========================================================================

@Entity(tableName = "exam_schedules")
data class ExamScheduleEntity(
    @PrimaryKey val scheduleId: String, // e.g. "SCHED-2026-TERM1-MATH"
    val institutionId: String = "INST-SXA-2026",
    val academicYear: String = "2026-2027",
    val examName: String,
    val classSection: String,
    val subject: String,
    val examDate: String,
    val startTime: String = "09:00 AM",
    val endTime: String = "12:00 PM",
    val maxMarks: Float = 100f,
    val passingMarks: Float = 35f,
    val roomNumber: String = "Room 304"
)

@Entity(tableName = "grading_schemes")
data class GradingSchemeEntity(
    @PrimaryKey val schemeId: String, // e.g. "GRADE-A1"
    val institutionId: String = "INST-SXA-2026",
    val gradeName: String, // e.g. "A1", "A2", "B1", "B2", "C1", "C2", "D", "E"
    val minPercentage: Float,
    val maxPercentage: Float,
    val gradePoint: Float,
    val remarks: String
)

// =========================================================================
// 33. FEE DISCOUNTS & INVOICES
// =========================================================================

@Entity(tableName = "fee_discounts")
data class FeeDiscountEntity(
    @PrimaryKey val discountId: String, // e.g. "DISC-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val discountName: String, // "Sibling Concession", "Merit Scholarship"
    val discountPercentage: Float = 0f,
    val discountPaise: Long = 0L,
    val academicYear: String = "2026-2027",
    val approvedBy: String = "Prof. Robert Vance (Principal)"
)

@Entity(tableName = "fee_invoices")
data class FeeInvoiceEntity(
    @PrimaryKey val invoiceId: String, // e.g. "INV-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val studentName: String,
    val invoiceNumber: String,
    val term: String = "Term 1: 2026-27",
    val totalAmountPaise: Long,
    val discountPaise: Long = 0L,
    val lateFeePaise: Long = 0L,
    val netAmountPaise: Long,
    val paidAmountPaise: Long = 0L,
    val balanceAmountPaise: Long,
    val dueDate: String,
    val status: String = "DUE", // DUE, PARTIALLY_PAID, PAID, OVERDUE
    val generatedDate: String = "2026-04-01"
)

// =========================================================================
// 34. DEVICE TOKENS FOR PUSH NOTIFICATIONS
// =========================================================================

@Entity(tableName = "device_tokens")
data class DeviceTokenEntity(
    @PrimaryKey val tokenId: String, // e.g. "TOK-USR-001"
    val institutionId: String = "INST-SXA-2026",
    val userId: String,
    val fcmToken: String,
    val deviceModel: String = "Android Device",
    val osVersion: String = "Android 14",
    val lastUpdated: Long = System.currentTimeMillis()
)

// =========================================================================
// 35. LIBRARY BOOKS & CIRCULATION ISSUES
// =========================================================================

@Entity(tableName = "library_books")
data class LibraryBookEntity(
    @PrimaryKey val bookId: String, // e.g. "LIB-BK-001"
    val institutionId: String = "INST-SXA-2026",
    val isbn: String = "978-0134685991",
    val title: String,
    val author: String,
    val publisher: String = "Academic Press",
    val category: String = "Science & Technology",
    val totalCopies: Int = 10,
    val availableCopies: Int = 8,
    val shelfLocation: String = "Shelf B-4"
)

@Entity(tableName = "library_issues")
data class LibraryIssueEntity(
    @PrimaryKey val issueId: String, // e.g. "ISSUE-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val bookId: String,
    val bookTitle: String,
    val borrowerId: String,
    val borrowerName: String,
    val borrowerRole: String = "STUDENT",
    val issueDate: String = "2026-09-15",
    val dueDate: String = "2026-09-29",
    val returnDate: String? = null,
    val status: String = "ISSUED", // ISSUED, RETURNED, OVERDUE
    val fineAmountPaise: Long = 0L
)

// =========================================================================
// 36. TRANSPORTATION FLEET, ROUTES & PICKUP STOPS
// =========================================================================

@Entity(tableName = "transport_routes")
data class TransportRouteEntity(
    @PrimaryKey val routeId: String, // e.g. "ROUTE-01"
    val institutionId: String = "INST-SXA-2026",
    val routeNumber: String = "Route 1",
    val routeName: String = "Civil Lines - Sector 5 Express",
    val vehicleId: String = "VEH-BUS-01",
    val driverName: String = "Harish Chandra",
    val driverPhone: String = "+91 98444 55667",
    val startLocation: String = "Central Bus Depot",
    val endLocation: String = "St. Xavier's Main Campus"
)

@Entity(tableName = "transport_vehicles")
data class TransportVehicleEntity(
    @PrimaryKey val vehicleId: String, // e.g. "VEH-BUS-01"
    val institutionId: String = "INST-SXA-2026",
    val registrationNumber: String = "DL-01-AB-1234",
    val capacity: Int = 42,
    val driverName: String = "Harish Chandra",
    val driverPhone: String = "+91 98444 55667",
    val insuranceValidUntil: String = "2027-03-31",
    val fitnessCertValidUntil: String = "2027-03-31"
)

@Entity(tableName = "transport_pickup_points")
data class TransportPickupPointEntity(
    @PrimaryKey val pointId: String, // e.g. "STOP-01"
    val institutionId: String = "INST-SXA-2026",
    val routeId: String = "ROUTE-01",
    val stopName: String = "Sector 5 Metro Junction",
    val morningPickupTime: String = "07:35 AM",
    val eveningDropTime: String = "03:45 PM",
    val monthlyFeePaise: Long = 180000L
)

// =========================================================================
// 37. INVENTORY, ASSET ITEMS & AUDIT TRANSACTIONS
// =========================================================================

@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val itemId: String, // e.g. "INV-SCI-001"
    val institutionId: String = "INST-SXA-2026",
    val itemName: String,
    val category: String = "LAB_EQUIPMENT", // LAB_EQUIPMENT, STATIONERY, FURNITURE, SPORTS, UNIFORM
    val quantityInStock: Int = 50,
    val unit: String = "Units",
    val reorderLevel: Int = 10,
    val unitPricePaise: Long = 120000L,
    val location: String = "Central Physics Lab"
)

@Entity(tableName = "inventory_transactions")
data class InventoryTransactionEntity(
    @PrimaryKey val txnId: String, // e.g. "ITXN-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val itemId: String,
    val itemName: String,
    val transactionType: String = "OUTWARD", // INWARD, OUTWARD, RETURN, SCRAP
    val quantity: Int = 2,
    val issuedToPersonId: String? = "FAC-TCH-01",
    val issuedToName: String? = "Dr. Meenakshi Sundaram",
    val timestamp: Long = System.currentTimeMillis(),
    val remarks: String = "Issued for class demonstration"
)

// =========================================================================
// 38. SUPPORT & GRIEVANCE TICKETS
// =========================================================================

@Entity(tableName = "support_tickets")
data class SupportTicketEntity(
    @PrimaryKey val ticketId: String, // e.g. "TCK-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val raisedByUserId: String,
    val raisedByName: String,
    val raisedByRole: String,
    val category: String = "ACADEMIC", // ACADEMIC, FEE_BILLING, TRANSPORT, IT_SYSTEM, DISCIPLINE
    val subject: String,
    val description: String,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    val assignedToStaffId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null,
    val resolutionNotes: String = ""
)

// =========================================================================
// 39. INSTITUTION SETTINGS & FEATURE FLAGS
// =========================================================================

@Entity(tableName = "school_settings")
data class SchoolSettingsEntity(
    @PrimaryKey val settingKey: String, // e.g. "SETTING_SMS_ENABLED"
    val institutionId: String = "INST-SXA-2026",
    val category: String = "GENERAL",
    val value: String = "true",
    val description: String = "Enable SMS alerts for parents",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "feature_flags")
data class FeatureFlagEntity(
    @PrimaryKey val flagKey: String, // e.g. "FLAG_ONLINE_PAYMENTS"
    val institutionId: String = "INST-SXA-2026",
    val isEnabled: Boolean = true,
    val description: String = "Enable online payment gateway checkout",
    val minAppVersion: Int = 1
)

// =========================================================================
// 40. ENTERPRISE DURABLE OUTBOX QUEUE (OFFLINE-FIRST WITH IDEMPOTENCY)
// =========================================================================

@Entity(
    tableName = "durable_outbox",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["institutionId", "status", "nextAttemptAt"])
    ]
)
data class DurableOutboxEntity(
    @PrimaryKey val outboxId: String, // UUID
    val idempotencyKey: String, // Unique operation idempotency key
    val institutionId: String = "INST-SXA-2026",
    val entityType: String, // "ATTENDANCE", "MARKS", "FEE_TRANSACTION", "HOMEWORK", "LEAVE", "ADMISSION"
    val entityId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val payloadJson: String,
    val payloadChecksum: String, // SHA-256 hash of payload
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long = 0L,
    val nextAttemptAt: Long = 0L,
    val retryCount: Int = 0,
    val maxRetries: Int = 5,
    val status: String = STATUS_PENDING,
    val payloadVersion: Int = 1,
    val serverAckToken: String? = null,
    val serverAckTimestamp: Long? = null,
    val conflictResolutionPolicy: String = "LAST_WRITE_WINS", // "SERVER_WINS", "CLIENT_WINS", "LAST_WRITE_WINS", "HIGH_INTEGRITY_MANUAL"
    val isHighIntegrity: Boolean = false, // true for financial, marks, and attendance
    val errorMessage: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_IN_FLIGHT = "IN_FLIGHT"
        const val STATUS_ACKNOWLEDGED = "ACKNOWLEDGED"
        const val STATUS_RETRYABLE_FAILURE = "RETRYABLE_FAILURE"
        const val STATUS_PERMANENT_FAILURE = "PERMANENT_FAILURE"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CONFLICT = "CONFLICT"
    }
}

// =========================================================================
// 41. SECURE CLOUD DOCUMENT STORAGE (TENANT-SCOPED VAULT)
// =========================================================================

@Entity(
    tableName = "cloud_documents",
    indices = [
        Index(value = ["institutionId", "ownerId", "category"])
    ]
)
data class CloudDocumentEntity(
    @PrimaryKey val documentId: String, // e.g. "DOC-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val ownerId: String, // studentId or staffId
    val ownerType: String = "STUDENT", // "STUDENT", "STAFF", "INSTITUTION"
    val category: String, // "STUDENT_ID_CARD", "TRANSFER_CERTIFICATE", "REPORT_CARD", "BIRTH_CERTIFICATE", "MEDICAL_RECORD", "GOV_AFFILIATION"
    val title: String,
    val fileName: String,
    val cloudStoragePath: String, // "INST-SXA-2026/students/ADM-2026-001/certificates/transfer_cert.pdf"
    val mimeType: String, // "application/pdf", "image/jpeg", "image/png", "image/webp"
    val fileSizeBytes: Long,
    val sha256Checksum: String,
    val uploadedByUserId: String,
    val uploadedTimestamp: Long = System.currentTimeMillis(),
    val isVerified: Boolean = false,
    val verifiedByUserId: String? = null,
    val downloadCount: Int = 0,
    val expiryTimestamp: Long? = null
)

// =========================================================================
// 42. NOTIFICATION PREFERENCES (FCM USER-LEVEL PREFERENCES)
// =========================================================================

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey val userId: String, // e.g. "USR-PAR-01"
    val institutionId: String = "INST-SXA-2026",
    val attendanceAlerts: Boolean = true,
    val homeworkAlerts: Boolean = true,
    val examResultsAlerts: Boolean = true,
    val feeDueAlerts: Boolean = true,
    val circularAnnouncements: Boolean = true,
    val emergencyAlerts: Boolean = true,
    val timetableChanges: Boolean = true,
    val leaveApprovalAlerts: Boolean = true,
    val pushEnabled: Boolean = true,
    val emailEnabled: Boolean = true,
    val whatsappEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

// =========================================================================
// 43. DISASTER RECOVERY & AUTOMATED BACKUP AUDIT
// =========================================================================

@Entity(tableName = "disaster_recovery_backups")
data class DisasterRecoveryBackupEntity(
    @PrimaryKey val backupId: String, // e.g. "BCK-2026-09-26-001"
    val institutionId: String = "INST-SXA-2026",
    val backupType: String = "SCHEDULED_DAILY", // "SCHEDULED_DAILY", "MANUAL_SNAPSHOT", "PRE_MIGRATION", "DISASTER_RECOVERY"
    val snapshotTimestamp: Long = System.currentTimeMillis(),
    val academicYear: String = "2026-2027",
    val recordCount: Int,
    val backupSizeBytes: Long,
    val checksumSha256: String,
    val storageBucketUri: String,
    val status: String = "COMPLETED", // "COMPLETED", "VERIFIED", "IN_PROGRESS", "FAILED"
    val restorationTested: Boolean = false,
    val restoreTestedTimestamp: Long? = null,
    val retentionDays: Int = 365
)

// =========================================================================
// 44. ACADEMIC PROMOTION & TRANSFER HISTORY
// =========================================================================

@Entity(
    tableName = "academic_promotion_history",
    indices = [
        Index(value = ["institutionId", "studentId", "fromAcademicYear"])
    ]
)
data class AcademicPromotionHistoryEntity(
    @PrimaryKey val promotionId: String, // e.g. "PROM-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val studentName: String,
    val fromAcademicYear: String,
    val toAcademicYear: String,
    val fromClassSection: String,
    val toClassSection: String,
    val promotionType: String = "ANNUAL_PROMOTION", // "ANNUAL_PROMOTION", "DOUBLE_PROMOTION", "RETAINED", "SECTION_TRANSFER", "CAMPUS_TRANSFER", "WITHDRAWAL"
    val finalPercentage: Float = 0f,
    val remarks: String = "Promoted successfully to next academic session",
    val authorizedByUserId: String = "USR-PRN-01",
    val timestamp: Long = System.currentTimeMillis()
)

// =========================================================================
// 45. ZERO-TRUST PAYMENT ORDERS (FEES & SAAS BILLING)
// =========================================================================

@Entity(
    tableName = "payment_orders",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["institutionId", "orderType", "status"])
    ]
)
data class PaymentOrderEntity(
    @PrimaryKey val orderId: String, // e.g. "ORD-FEE-2026-001" or "ORD-SAAS-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val orderType: String = "SCHOOL_FEE", // "SCHOOL_FEE" or "SAAS_SUBSCRIPTION"
    val invoiceId: String,
    val payerUserId: String,
    val payerName: String,
    val studentId: String? = null,
    val subscriptionPlanId: String? = null, // e.g. "PLAN-ENTERPRISE-ANNUAL"
    val amountPaise: Long,
    val currency: String = "INR",
    val receiptNumber: String,
    val gatewayProvider: String = "RAZORPAY", // "RAZORPAY", "CASHFREE", "UPI_DIRECT"
    val gatewayOrderId: String? = null,
    val gatewayPaymentId: String? = null,
    val idempotencyKey: String,
    val status: String = "CREATED", // "CREATED", "PENDING", "SUCCESS", "FAILED", "REFUNDED"
    val upiPaymentString: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (30 * 60 * 1000L),
    val completedAt: Long? = null,
    val failureReason: String? = null,
    val checksumSha256: String
)

// =========================================================================
// 46. DOUBLE-ENTRY FINANCIAL LEDGER (FEES VS SAAS SUBSCRIPTIONS)
// =========================================================================

@Entity(
    tableName = "financial_ledger",
    indices = [
        Index(value = ["institutionId", "ledgerDomain", "accountType"])
    ]
)
data class FinancialLedgerEntryEntity(
    @PrimaryKey val entryId: String, // e.g. "LEDG-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val ledgerDomain: String = "SCHOOL_FEE_COLLECTION", // "SCHOOL_FEE_COLLECTION" vs "SAAS_SUBSCRIPTION_BILLING"
    val accountType: String, // "CASH_AT_BANK", "FEES_RECEIVABLE", "ESCROW_CLEARING", "PLATFORM_REVENUE"
    val transactionType: String, // "CREDIT", "DEBIT"
    val amountPaise: Long,
    val referenceOrderId: String,
    val referenceInvoiceId: String,
    val description: String,
    val balanceAfterPaise: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isReconciled: Boolean = true,
    val checksum: String
)

// =========================================================================
// 47. AUTOMATED MESSAGE DISPATCH LOGS (WHATSAPP/SMS AUTOMATION & RATE LIMITING)
// =========================================================================

@Entity(
    tableName = "automated_dispatch_logs",
    indices = [
        Index(value = ["institutionId", "status", "priority"])
    ]
)
data class AutomatedMessageDispatchEntity(
    @PrimaryKey val dispatchId: String, // e.g. "DISP-2026-001"
    val institutionId: String = "INST-SXA-2026",
    val channel: String = "WHATSAPP_AUTOMATION", // "WHATSAPP_AUTOMATION", "SMS_GATEWAY", "FCM_FALLBACK"
    val recipientPhone: String,
    val recipientName: String,
    val templateId: String, // e.g. "ATTENDANCE_ABSENT_ALERT"
    val renderedContent: String,
    val status: String = "QUEUED", // "QUEUED", "DISPATCHED", "DELIVERED", "FAILED"
    val priority: String = "HIGH", // "EMERGENCY", "HIGH", "NORMAL"
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3,
    val isQuietHoursDeferred: Boolean = false,
    val hasOptInConsent: Boolean = true,
    val dispatchedAt: Long? = null,
    val deliveredAt: Long? = null,
    val errorMessage: String? = null
)



