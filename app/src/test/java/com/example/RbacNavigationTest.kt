package com.example

import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.RbacRoleCategory
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.model.getDefaultPermissionsForRole
import com.example.data.model.parseUserRole
import com.example.data.model.rbacCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RbacNavigationTest {

    @Test
    fun testRbacCategoryMapping() {
        // Level 3: Admin category
        assertEquals(RbacRoleCategory.ADMIN, UserRole.ADMIN.rbacCategory)
        assertEquals(RbacRoleCategory.ADMIN, UserRole.PRINCIPAL.rbacCategory)
        assertEquals(3, UserRole.ADMIN.rbacCategory.clearanceLevel)

        // Level 2: Teacher category
        assertEquals(RbacRoleCategory.TEACHER, UserRole.TEACHER.rbacCategory)
        assertEquals(RbacRoleCategory.TEACHER, UserRole.TEACHING.rbacCategory)
        assertEquals(RbacRoleCategory.TEACHER, UserRole.INCHARGE.rbacCategory)
        assertEquals(2, UserRole.TEACHER.rbacCategory.clearanceLevel)

        // Level 1: Student category
        assertEquals(RbacRoleCategory.STUDENT, UserRole.STUDENT.rbacCategory)
        assertEquals(RbacRoleCategory.STUDENT, UserRole.PARENT.rbacCategory)
        assertEquals(1, UserRole.STUDENT.rbacCategory.clearanceLevel)
    }

    @Test
    fun testParseUserRoleHelper() {
        assertEquals(UserRole.ADMIN, parseUserRole("admin"))
        assertEquals(UserRole.ADMIN, parseUserRole("ADMIN"))
        assertEquals(UserRole.ADMIN, parseUserRole("administrator"))
        assertEquals(UserRole.ADMIN, parseUserRole("super_admin"))

        assertEquals(UserRole.TEACHER, parseUserRole("teacher"))
        assertEquals(UserRole.TEACHER, parseUserRole("TEACHER"))
        assertEquals(UserRole.TEACHER, parseUserRole("faculty"))
        assertEquals(UserRole.TEACHING, parseUserRole("TEACHING"))

        assertEquals(UserRole.STUDENT, parseUserRole("student"))
        assertEquals(UserRole.STUDENT, parseUserRole("STUDENT"))
        assertEquals(UserRole.STUDENT, parseUserRole("scholar"))

        // Unknown role safely defaults to STUDENT
        assertEquals(UserRole.STUDENT, parseUserRole("unknown_external_guest"))
    }

    @Test
    fun testPermissionsByRole() {
        val adminPermissions = getDefaultPermissionsForRole(UserRole.ADMIN)
        assertTrue(adminPermissions.contains(SchoolPermissions.VIEW_AUDIT_LOGS))
        assertTrue(adminPermissions.contains(SchoolPermissions.MANAGE_INSTITUTION))
        assertTrue(adminPermissions.contains(SchoolPermissions.VIEW_ROSTER_ALL))

        val teacherPermissions = getDefaultPermissionsForRole(UserRole.TEACHER)
        assertTrue(teacherPermissions.contains(SchoolPermissions.MARK_ATTENDANCE_SECTION))
        assertTrue(teacherPermissions.contains(SchoolPermissions.SCAN_REGISTER_CAMERA))
        assertFalse(teacherPermissions.contains(SchoolPermissions.VIEW_AUDIT_LOGS))

        val studentPermissions = getDefaultPermissionsForRole(UserRole.STUDENT)
        assertTrue(studentPermissions.contains(SchoolPermissions.READ_DIARY))
        assertFalse(studentPermissions.contains(SchoolPermissions.VIEW_AUDIT_LOGS))
        assertFalse(studentPermissions.contains(SchoolPermissions.SCAN_REGISTER_CAMERA))
    }

    @Test
    fun testSecurityClaimsAccessControl() {
        val adminClaims = SecurityClaims(
            userId = "USR-ADM-01",
            role = UserRole.ADMIN,
            roleClaim = "ROLE_ADMIN",
            displayName = "Administrator",
            permissions = getDefaultPermissionsForRole(UserRole.ADMIN)
        )
        assertTrue(adminClaims.hasPermission(SchoolPermissions.VIEW_AUDIT_LOGS))
        assertTrue(adminClaims.hasPermission(SchoolPermissions.SCAN_REGISTER_CAMERA))

        val studentClaims = SecurityClaims(
            userId = "USR-STU-01",
            role = UserRole.STUDENT,
            roleClaim = "ROLE_STUDENT",
            displayName = "Student Scholar",
            permissions = getDefaultPermissionsForRole(UserRole.STUDENT)
        )
        assertFalse(studentClaims.hasPermission(SchoolPermissions.VIEW_AUDIT_LOGS))
        assertFalse(studentClaims.hasPermission(SchoolPermissions.SCAN_REGISTER_CAMERA))
    }

    @Test
    fun testDatabaseSchemaEntities() {
        // UserEntity with permissions field
        val userEntity = UserEntity(
            id = "USR-ADM-99",
            institutionId = "INST-SXA-2026",
            username = "admin_director",
            email = "director@stxaviers.edu",
            password = "Password@123",
            role = "ADMIN",
            displayName = "Director of Admin",
            permissions = "VIEW_AUDIT_LOGS,MANAGE_INSTITUTION"
        )
        assertEquals("ADMIN", userEntity.role)
        assertEquals("VIEW_AUDIT_LOGS,MANAGE_INSTITUTION", userEntity.permissions)

        // StudentEntity with default constructor
        val student = StudentEntity(
            id = "ADM-2026-TEST",
            name = "Test Scholar",
            grade = "10",
            section = "A",
            rollNumber = 99
        )
        assertEquals("ADM-2026-TEST", student.admissionNumber)
        assertEquals("10", student.grade)
        assertEquals("A", student.section)

        // ExamRecordEntity constructor overload
        val examRecord = ExamRecordEntity(
            id = "EXAM-101",
            studentId = "ADM-2026-TEST",
            subject = "Physics",
            marksObtained = 95.0,
            maxMarks = 100.0,
            grade = "A+",
            term = "Term 1"
        )
        assertEquals(95f, examRecord.marksObtained, 0.01f)
        assertEquals(95f, examRecord.percentage, 0.01f)

        // FeeRecordEntity constructor overload
        val feeRecord = FeeRecordEntity(
            id = "FEE-101",
            studentId = "ADM-2026-TEST",
            studentName = "Test Scholar",
            gradeSection = "10-A",
            totalRupees = 50000.0,
            paidRupees = 50000.0,
            dueRupees = 0.0,
            status = "PAID",
            term = "Term 1"
        )
        assertEquals(50000L, feeRecord.totalRupees)
        assertEquals(0L, feeRecord.dueRupees)
    }
}
