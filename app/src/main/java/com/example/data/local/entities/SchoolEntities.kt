package com.example.data.local.entities

import androidx.room.Entity
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
    val password: String = "Password@123",
    val role: String, // PRINCIPAL, VICE_PRINCIPAL, INCHARGE, STAFF_MANAGER, TEACHING, ACCOUNTANT, OFFICE, STUDENT, PARENT, NON_TEACHING
    val displayName: String,
    val phoneNumber: String,
    val authPin: String = "1234",
    val isOtpVerified: Boolean = true,
    val isActive: Boolean = true,
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
    val gender: String,
    val dateOfBirth: String = "2011-04-12",
    val guardianName: String,
    val guardianPhone: String,
    val studentPhone: String = guardianPhone,
    val email: String,
    val bloodGroup: String,
    val attendancePercentage: Float,
    val feeDue: Double,
    val feePaid: Double,
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
    @PrimaryKey val examId: String, // e.g. "EXAM-10A-MATH-001"
    val institutionId: String = "INST-SXA-2026",
    val academicYear: String = "2026-2027",
    val examType: String, // "UNIT_TEST", "FORMATIVE_ASSESSMENT", "SUMMATIVE_ASSESSMENT", "HALF_YEARLY", "FINAL_EXAM"
    val examName: String, // "Term 1 Summative Assessment"
    val classSection: String, // "10-A"
    val subject: String, // "Mathematics", "Physics", "English"
    val studentId: String,
    val studentName: String,
    val rollNumber: Int,
    val marksObtained: Float,
    val maxMarks: Float = 100f,
    val grade: String = "A",
    val teacherRemarks: String = "Excellent problem-solving methodology",
    val enteredByStaffId: String = "FAC-TCH-02",
    val isPublished: Boolean = true
) {
    val percentage: Float get() = if (maxMarks > 0) (marksObtained / maxMarks) * 100f else 0f
}

// =========================================================================
// 7. FEE MANAGEMENT & RECEIPTS (Unique receipt numbers, official printable layout)
// =========================================================================

@Entity(tableName = "fee_records")
data class FeeRecordEntity(
    @PrimaryKey val id: String,
    val institutionId: String = "INST-SXA-2026",
    val studentId: String,
    val admissionNumber: String,
    val studentName: String,
    val gradeSection: String,
    val term: String, // "Term 1: 2026-27"
    val feeType: String, // "Composite Tuition & Lab Fee"
    val totalAmount: Long, // In paise/cents to prevent floating point error
    val paidAmount: Long,
    val dueAmount: Long,
    val status: String, // "PAID", "PARTIAL", "DUE"
    val receiptNumber: String?,
    val paidDate: String?,
    val paymentMode: String = "ONLINE", // "ONLINE", "CHEQUE", "CASH", "BANK_TRANSFER"
    val verifiedByIncharge: Boolean = true
) {
    val totalRupees: Long get() = if (totalAmount >= 100000L) totalAmount / 100L else totalAmount
    val paidRupees: Long get() = if (paidAmount >= 100000L) paidAmount / 100L else paidAmount
    val dueRupees: Long get() = if (dueAmount >= 100000L) dueAmount / 100L else dueAmount
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
    val syncStatus: String = "SYNCED", // "PENDING", "SYNCING", "SYNCED", "FAILED"
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
    val securityVerificationHash: String = "SXA-HASH-${System.currentTimeMillis() % 100000}"
)
