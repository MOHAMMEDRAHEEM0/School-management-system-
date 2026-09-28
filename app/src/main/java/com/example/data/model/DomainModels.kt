package com.example.data.model

// =========================================================================
// ROLE-BASED ACCESS CONTROL (RBAC) & DOMAIN ENUMS
// =========================================================================

enum class UserRole(
    val label: String,
    val badge: String,
    val clearanceTitle: String,
    val roleClaim: String
) {
    ADMIN(
        label = "System & Institutional Administrator",
        badge = "ADMIN",
        clearanceTitle = "Executive Administration Directorate",
        roleClaim = "ROLE_ADMIN"
    ),
    PRINCIPAL(
        label = "Principal & Executive Admin",
        badge = "PRINCIPAL",
        clearanceTitle = "Executive Directorate",
        roleClaim = "ROLE_PRINCIPAL"
    ),
    VICE_PRINCIPAL(
        label = "Vice Principal & Head Admin",
        badge = "VICE_PRIN",
        clearanceTitle = "Vice Principal Desk",
        roleClaim = "ROLE_VICE_PRINCIPAL"
    ),
    INCHARGE(
        label = "Class In-Charge & Section Head",
        badge = "INCHARGE",
        clearanceTitle = "Class In-Charge Desk",
        roleClaim = "ROLE_INCHARGE"
    ),
    STAFF_MANAGER(
        label = "Staff & Personnel Manager",
        badge = "STAFF_MGR",
        clearanceTitle = "Staff Management Desk",
        roleClaim = "ROLE_STAFF_MANAGER"
    ),
    TEACHER(
        label = "Teacher & Academic Faculty",
        badge = "TEACHER",
        clearanceTitle = "Academic Faculty Portal",
        roleClaim = "ROLE_TEACHER"
    ),
    TEACHING(
        label = "Teacher & Academic Faculty",
        badge = "TEACHER",
        clearanceTitle = "Academic Faculty Portal",
        roleClaim = "ROLE_TEACHER"
    ),
    ACCOUNTANT(
        label = "Accountant & Fee Manager",
        badge = "ACCOUNTANT",
        clearanceTitle = "Finance & Bursar Desk",
        roleClaim = "ROLE_ACCOUNTANT"
    ),
    OFFICE(
        label = "Office & Documentation Desk",
        badge = "OFFICE",
        clearanceTitle = "Admissions & Registrar",
        roleClaim = "ROLE_OFFICE"
    ),
    STUDENT(
        label = "Student Scholar",
        badge = "STUDENT",
        clearanceTitle = "Scholar Student Portal",
        roleClaim = "ROLE_STUDENT"
    ),
    PARENT(
        label = "Parent & Guardian",
        badge = "PARENT",
        clearanceTitle = "Guardian Privacy Passport",
        roleClaim = "ROLE_PARENT"
    ),
    LIBRARIAN(
        label = "Librarian & Learning Resources",
        badge = "LIBRARIAN",
        clearanceTitle = "Library & Resource Center",
        roleClaim = "ROLE_LIBRARIAN"
    ),
    TRANSPORT_STAFF(
        label = "Transport Manager & Logistics Staff",
        badge = "TRANSPORT",
        clearanceTitle = "Fleet & Transport Logistics",
        roleClaim = "ROLE_TRANSPORT_STAFF"
    ),
    NON_TEACHING(
        label = "Operations & Support Staff",
        badge = "OPERATIONS",
        clearanceTitle = "Campus Operations",
        roleClaim = "ROLE_NON_TEACHING"
    )
}

// =========================================================================
// RBAC NAVIGATION CATEGORY
// =========================================================================

enum class RbacRoleCategory(
    val title: String,
    val description: String,
    val clearanceLevel: Int
) {
    ADMIN(
        title = "Admin",
        description = "Executive Administration & Institutional Governance",
        clearanceLevel = 3
    ),
    TEACHER(
        title = "Teacher",
        description = "Academic Faculty, Attendance & Grading Portal",
        clearanceLevel = 2
    ),
    STUDENT(
        title = "Student",
        description = "Scholar Academic Passport, Attendance & Reports",
        clearanceLevel = 1
    )
}

val UserRole.rbacCategory: RbacRoleCategory
    get() = when (this) {
        UserRole.ADMIN, UserRole.PRINCIPAL, UserRole.VICE_PRINCIPAL,
        UserRole.STAFF_MANAGER, UserRole.OFFICE, UserRole.ACCOUNTANT,
        UserRole.LIBRARIAN -> RbacRoleCategory.ADMIN

        UserRole.TEACHER, UserRole.TEACHING, UserRole.INCHARGE -> RbacRoleCategory.TEACHER

        UserRole.STUDENT, UserRole.PARENT, UserRole.NON_TEACHING,
        UserRole.TRANSPORT_STAFF -> RbacRoleCategory.STUDENT
    }

fun parseUserRole(roleStr: String): UserRole {
    return when (roleStr.trim().uppercase()) {
        "ADMIN", "SUPER_ADMIN", "ADMINISTRATOR" -> UserRole.ADMIN
        "PRINCIPAL" -> UserRole.PRINCIPAL
        "VICE_PRINCIPAL" -> UserRole.VICE_PRINCIPAL
        "TEACHER", "FACULTY" -> UserRole.TEACHER
        "TEACHING" -> UserRole.TEACHING
        "INCHARGE" -> UserRole.INCHARGE
        "STUDENT", "SCHOLAR" -> UserRole.STUDENT
        "PARENT", "GUARDIAN" -> UserRole.PARENT
        "ACCOUNTANT" -> UserRole.ACCOUNTANT
        "OFFICE" -> UserRole.OFFICE
        "STAFF_MANAGER" -> UserRole.STAFF_MANAGER
        "LIBRARIAN" -> UserRole.LIBRARIAN
        "TRANSPORT", "TRANSPORT_STAFF", "DRIVER" -> UserRole.TRANSPORT_STAFF
        "NON_TEACHING" -> UserRole.NON_TEACHING
        else -> try {
            UserRole.valueOf(roleStr.trim().uppercase())
        } catch (e: Exception) {
            UserRole.STUDENT
        }
    }
}

object SchoolPermissions {
    // Institutional Management
    const val MANAGE_INSTITUTION = "MANAGE_INSTITUTION"
    const val CONFIGURE_ACADEMIC_YEAR = "CONFIGURE_ACADEMIC_YEAR"
    const val MANAGE_CLASSES = "MANAGE_CLASSES"
    const val MANAGE_STAFF_DBMS = "MANAGE_STAFF_DBMS"
    const val ASSIGN_TEACHERS = "ASSIGN_TEACHERS"

    // Roster & Attendance
    const val VIEW_ROSTER_ALL = "VIEW_ROSTER_ALL"
    const val VIEW_ROSTER_SECTION = "VIEW_ROSTER_SECTION"
    const val MARK_ATTENDANCE_SECTION = "MARK_ATTENDANCE_SECTION"
    const val SCAN_REGISTER_CAMERA = "SCAN_REGISTER_CAMERA"
    const val VERIFY_ATTENDANCE_INCHARGE = "VERIFY_ATTENDANCE_INCHARGE"
    const val APPROVE_ATTENDANCE_PRINCIPAL = "APPROVE_ATTENDANCE_PRINCIPAL"
    const val VIEW_ATTENDANCE_SELF = "VIEW_ATTENDANCE_SELF"
    const val VIEW_ATTENDANCE_WARD = "VIEW_ATTENDANCE_WARD"
    const val SUBMIT_LEAVE_EXCUSE = "SUBMIT_LEAVE_EXCUSE"

    // Examinations & Marks
    const val ENTER_MARKS_ASSIGNED_SUBJECT = "ENTER_MARKS_ASSIGNED_SUBJECT"
    const val REVIEW_REPORT_CARDS = "REVIEW_REPORT_CARDS"
    const val PUBLISH_EXAM_RESULTS = "PUBLISH_EXAM_RESULTS"
    const val VIEW_REPORT_CARD_SELF = "VIEW_REPORT_CARD_SELF"
    const val VIEW_REPORT_CARD_WARD = "VIEW_REPORT_CARD_WARD"

    // Fees & Finance
    const val MANAGE_FEE_STRUCTURE = "MANAGE_FEE_STRUCTURE"
    const val RECORD_OFFLINE_FEE = "RECORD_OFFLINE_FEE"
    const val REMIT_ONLINE_FEE = "REMIT_ONLINE_FEE"
    const val VIEW_FEE_SELF = "VIEW_FEE_SELF"
    const val VIEW_FEE_WARD = "VIEW_FEE_WARD"
    const val VIEW_INSTITUTIONAL_FINANCES = "VIEW_INSTITUTIONAL_FINANCES"
    const val MANAGE_INSTITUTIONAL_BILLS = "MANAGE_INSTITUTIONAL_BILLS"

    // Documents & Certificates
    const val REQUEST_CERTIFICATE = "REQUEST_CERTIFICATE"
    const val VERIFY_DOCUMENT_INCHARGE = "VERIFY_DOCUMENT_INCHARGE"
    const val APPROVE_DOCUMENT_PRINCIPAL = "APPROVE_DOCUMENT_PRINCIPAL"
    const val GENERATE_OFFICIAL_PDF = "GENERATE_OFFICIAL_PDF"
    const val MANAGE_GOVT_COMPLIANCE = "MANAGE_GOVT_COMPLIANCE"

    // Admissions
    const val SUBMIT_ADMISSION_APPLICATION = "SUBMIT_ADMISSION_APPLICATION"
    const val VERIFY_ADMISSION = "VERIFY_ADMISSION"
    const val APPROVE_ADMISSION_ENROLL = "APPROVE_ADMISSION_ENROLL"

    // ID Cards
    const val CAPTURE_REAL_ID_PHOTO = "CAPTURE_REAL_ID_PHOTO"
    const val VERIFY_ID_CARD = "VERIFY_ID_CARD"
    const val APPROVE_ID_CARD = "APPROVE_ID_CARD"
    const val VIEW_ID_CARD_SELF = "VIEW_ID_CARD_SELF"
    const val VIEW_ID_CARD_WARD = "VIEW_ID_CARD_WARD"

    // Diary & Timetable & Circulars
    const val WRITE_CLASS_DIARY = "WRITE_CLASS_DIARY"
    const val READ_DIARY = "READ_DIARY"
    const val MANAGE_TIMETABLE = "MANAGE_TIMETABLE"
    const val VIEW_TIMETABLE_ASSIGNED = "VIEW_TIMETABLE_ASSIGNED"
    const val PUBLISH_CIRCULAR = "PUBLISH_CIRCULAR"
    const val VIEW_AUDIT_LOGS = "VIEW_AUDIT_LOGS"

    // Library Management
    const val MANAGE_LIBRARY = "MANAGE_LIBRARY"
    const val ISSUE_LIBRARY_BOOK = "ISSUE_LIBRARY_BOOK"
    const val RETURN_LIBRARY_BOOK = "RETURN_LIBRARY_BOOK"
    const val VIEW_LIBRARY_CATALOG = "VIEW_LIBRARY_CATALOG"

    // Transport Logistics
    const val MANAGE_TRANSPORT = "MANAGE_TRANSPORT"
    const val VIEW_TRANSPORT_ROUTES = "VIEW_TRANSPORT_ROUTES"
    const val UPDATE_TRANSPORT_STATUS = "UPDATE_TRANSPORT_STATUS"
}

data class SecurityClaims(
    val userId: String,
    val role: UserRole,
    val roleClaim: String,
    val displayName: String,
    val phone: String = "+91 98765 43210",
    val email: String = "user@stxaviers.edu",
    val designationOrGrade: String = "",
    val assignedClassSection: String? = null,
    val assignedSubjects: List<String> = emptyList(),
    val permissions: Set<String>,
    val sessionToken: String = "SESSION-TOK-DEFAULT",
    val authenticatedAt: Long = System.currentTimeMillis()
) {
    fun hasPermission(permission: String): Boolean = permissions.contains(permission)
}

fun getDefaultPermissionsForRole(role: UserRole): Set<String> {
    return when (role) {
        UserRole.ADMIN, UserRole.PRINCIPAL -> setOf(
            SchoolPermissions.MANAGE_INSTITUTION,
            SchoolPermissions.CONFIGURE_ACADEMIC_YEAR,
            SchoolPermissions.MANAGE_CLASSES,
            SchoolPermissions.MANAGE_STAFF_DBMS,
            SchoolPermissions.ASSIGN_TEACHERS,
            SchoolPermissions.VIEW_ROSTER_ALL,
            SchoolPermissions.VIEW_ROSTER_SECTION,
            SchoolPermissions.MARK_ATTENDANCE_SECTION,
            SchoolPermissions.SCAN_REGISTER_CAMERA,
            SchoolPermissions.VERIFY_ATTENDANCE_INCHARGE,
            SchoolPermissions.APPROVE_ATTENDANCE_PRINCIPAL,
            SchoolPermissions.ENTER_MARKS_ASSIGNED_SUBJECT,
            SchoolPermissions.REVIEW_REPORT_CARDS,
            SchoolPermissions.PUBLISH_EXAM_RESULTS,
            SchoolPermissions.MANAGE_FEE_STRUCTURE,
            SchoolPermissions.RECORD_OFFLINE_FEE,
            SchoolPermissions.VIEW_INSTITUTIONAL_FINANCES,
            SchoolPermissions.MANAGE_INSTITUTIONAL_BILLS,
            SchoolPermissions.VERIFY_DOCUMENT_INCHARGE,
            SchoolPermissions.APPROVE_DOCUMENT_PRINCIPAL,
            SchoolPermissions.GENERATE_OFFICIAL_PDF,
            SchoolPermissions.MANAGE_GOVT_COMPLIANCE,
            SchoolPermissions.VERIFY_ADMISSION,
            SchoolPermissions.APPROVE_ADMISSION_ENROLL,
            SchoolPermissions.CAPTURE_REAL_ID_PHOTO,
            SchoolPermissions.VERIFY_ID_CARD,
            SchoolPermissions.APPROVE_ID_CARD,
            SchoolPermissions.WRITE_CLASS_DIARY,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.MANAGE_TIMETABLE,
            SchoolPermissions.PUBLISH_CIRCULAR,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.VICE_PRINCIPAL -> setOf(
            SchoolPermissions.VIEW_ROSTER_ALL,
            SchoolPermissions.VIEW_ROSTER_SECTION,
            SchoolPermissions.MARK_ATTENDANCE_SECTION,
            SchoolPermissions.SCAN_REGISTER_CAMERA,
            SchoolPermissions.VERIFY_ATTENDANCE_INCHARGE,
            SchoolPermissions.REVIEW_REPORT_CARDS,
            SchoolPermissions.RECORD_OFFLINE_FEE,
            SchoolPermissions.VERIFY_DOCUMENT_INCHARGE,
            SchoolPermissions.VERIFY_ADMISSION,
            SchoolPermissions.APPROVE_ID_CARD,
            SchoolPermissions.WRITE_CLASS_DIARY,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.MANAGE_TIMETABLE,
            SchoolPermissions.PUBLISH_CIRCULAR,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.INCHARGE -> setOf(
            SchoolPermissions.VIEW_ROSTER_SECTION,
            SchoolPermissions.MARK_ATTENDANCE_SECTION,
            SchoolPermissions.SCAN_REGISTER_CAMERA,
            SchoolPermissions.VERIFY_ATTENDANCE_INCHARGE,
            SchoolPermissions.ENTER_MARKS_ASSIGNED_SUBJECT,
            SchoolPermissions.REVIEW_REPORT_CARDS,
            SchoolPermissions.RECORD_OFFLINE_FEE,
            SchoolPermissions.VERIFY_DOCUMENT_INCHARGE,
            SchoolPermissions.VERIFY_ID_CARD,
            SchoolPermissions.WRITE_CLASS_DIARY,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.PUBLISH_CIRCULAR,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.STAFF_MANAGER -> setOf(
            SchoolPermissions.MANAGE_STAFF_DBMS,
            SchoolPermissions.ASSIGN_TEACHERS,
            SchoolPermissions.CAPTURE_REAL_ID_PHOTO,
            SchoolPermissions.VERIFY_ID_CARD,
            SchoolPermissions.PUBLISH_CIRCULAR,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.TEACHER, UserRole.TEACHING -> setOf(
            SchoolPermissions.VIEW_ROSTER_SECTION,
            SchoolPermissions.MARK_ATTENDANCE_SECTION,
            SchoolPermissions.SCAN_REGISTER_CAMERA,
            SchoolPermissions.ENTER_MARKS_ASSIGNED_SUBJECT,
            SchoolPermissions.CAPTURE_REAL_ID_PHOTO,
            SchoolPermissions.WRITE_CLASS_DIARY,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.VIEW_TIMETABLE_ASSIGNED,
            SchoolPermissions.PUBLISH_CIRCULAR
        )
        UserRole.ACCOUNTANT -> setOf(
            SchoolPermissions.RECORD_OFFLINE_FEE,
            SchoolPermissions.VIEW_INSTITUTIONAL_FINANCES,
            SchoolPermissions.MANAGE_INSTITUTIONAL_BILLS,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.OFFICE -> setOf(
            SchoolPermissions.SUBMIT_ADMISSION_APPLICATION,
            SchoolPermissions.VERIFY_ADMISSION,
            SchoolPermissions.REQUEST_CERTIFICATE,
            SchoolPermissions.VERIFY_DOCUMENT_INCHARGE,
            SchoolPermissions.GENERATE_OFFICIAL_PDF,
            SchoolPermissions.MANAGE_GOVT_COMPLIANCE
        )
        UserRole.STUDENT -> setOf(
            SchoolPermissions.VIEW_ATTENDANCE_SELF,
            SchoolPermissions.VIEW_ID_CARD_SELF,
            SchoolPermissions.VIEW_REPORT_CARD_SELF,
            SchoolPermissions.VIEW_FEE_SELF,
            SchoolPermissions.REQUEST_CERTIFICATE,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.SUBMIT_LEAVE_EXCUSE
        )
        UserRole.PARENT -> setOf(
            SchoolPermissions.VIEW_ATTENDANCE_WARD,
            SchoolPermissions.VIEW_ID_CARD_WARD,
            SchoolPermissions.VIEW_REPORT_CARD_WARD,
            SchoolPermissions.VIEW_FEE_WARD,
            SchoolPermissions.REMIT_ONLINE_FEE,
            SchoolPermissions.REQUEST_CERTIFICATE,
            SchoolPermissions.READ_DIARY,
            SchoolPermissions.SUBMIT_LEAVE_EXCUSE
        )
        UserRole.NON_TEACHING -> setOf(
            SchoolPermissions.VIEW_ATTENDANCE_SELF
        )
        UserRole.LIBRARIAN -> setOf(
            SchoolPermissions.MANAGE_LIBRARY,
            SchoolPermissions.ISSUE_LIBRARY_BOOK,
            SchoolPermissions.RETURN_LIBRARY_BOOK,
            SchoolPermissions.VIEW_LIBRARY_CATALOG,
            SchoolPermissions.VIEW_AUDIT_LOGS
        )
        UserRole.TRANSPORT_STAFF -> setOf(
            SchoolPermissions.MANAGE_TRANSPORT,
            SchoolPermissions.VIEW_TRANSPORT_ROUTES,
            SchoolPermissions.UPDATE_TRANSPORT_STATUS
        )
    }
}

// =========================================================================
// AUTHENTICATION & SECURE SESSION MODELS
// =========================================================================

data class AuthSession(
    val token: String,
    val user: com.example.data.local.entities.UserEntity,
    val role: UserRole,
    val institution: com.example.data.local.entities.InstitutionEntity,
    val academicBatch: String,
    val loginTimestamp: Long = System.currentTimeMillis(),
    val expiryTimestamp: Long = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiryTimestamp
}

sealed class AuthResult {
    data class Success(val session: AuthSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

