package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AcademicClassEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.UserEntity
import com.example.data.model.SchoolPermissions
import com.example.data.model.SecurityClaims
import com.example.data.model.UserRole
import com.example.data.remote.sync.CloudOutboxDispatcher
import com.example.data.remote.sync.DurableOutboxManager
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.SchoolRepository
import com.example.data.repository.UserSession
import com.example.security.SecureSessionManager
import com.example.security.TenantSecurityContext
import com.example.security.TenantSecurityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class DailyAttendanceTest {

    private lateinit var db: AppDatabase
    private lateinit var schoolRepository: SchoolRepository
    private lateinit var attendanceRepository: AttendanceRepositoryImpl
    private lateinit var outboxManager: DurableOutboxManager

    private val tenantId = "INST-SXA-2026"
    private val teacherId = "FAC-TCH-01"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        outboxManager = DurableOutboxManager(
            dao = db.schoolDao(),
            defaultInstitutionId = tenantId
        )

        attendanceRepository = AttendanceRepositoryImpl(
            dao = db.schoolDao(),
            outboxManager = outboxManager
        )
        schoolRepository = SchoolRepository(
            dao = db.schoolDao(),
            outboxManager = outboxManager,
            attendanceRepository = attendanceRepository
        )

        // Set default authenticated session as assigned class teacher
        setSession(userId = teacherId, role = UserRole.TEACHER, institution = tenantId)

        // Pre-seed academic class 10-A assigned to FAC-TCH-01
        runBlocking {
            db.schoolDao().insertAcademicClass(
                AcademicClassEntity(
                    classId = "CLS-10A",
                    institutionId = tenantId,
                    className = "Grade 10",
                    section = "A",
                    classTeacherId = teacherId,
                    classTeacherName = "Mrs. Sunita Kapoor"
                )
            )
            db.schoolDao().insertAcademicClass(
                AcademicClassEntity(
                    classId = "CLS-11B",
                    institutionId = tenantId,
                    className = "Grade 11",
                    section = "B",
                    classTeacherId = "FAC-OTHER-99",
                    classTeacherName = "Mr. Verma"
                )
            )
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        SecureSessionManager.clearSession()
        TenantSecurityContext.clearContext()
        db.close()
    }

    private fun setSession(
        userId: String,
        role: UserRole,
        institution: String,
        permissions: Set<String> = setOf(SchoolPermissions.MARK_ATTENDANCE_SECTION)
    ) {
        val user = UserEntity(
            id = userId,
            institutionId = institution,
            username = "user_$userId",
            email = "$userId@school.edu",
            displayName = "User $userId",
            role = role.name,
            password = "",
            passwordSalt = "",
            passwordHash = ""
        )
        val claims = SecurityClaims(
            userId = user.id,
            role = role,
            roleClaim = role.name,
            displayName = user.displayName,
            email = user.email,
            permissions = permissions,
            sessionToken = "TOKEN-$userId"
        )
        val session = UserSession(
            userId = user.id,
            institutionId = institution,
            email = user.email,
            displayName = user.displayName,
            role = role,
            sessionToken = "TOKEN-$userId",
            claims = claims
        )
        SecureSessionManager.saveSession(session)
    }

    // =========================================================================
    // 1. ATTENDANCE CREATION & ATOMIC ROOM PERSISTENCE
    // =========================================================================

    @Test
    fun testAttendanceSavingAndRoomPersistence() = runBlocking {
        // 1. Insert test scholars into Room database
        val s1 = StudentEntity(
            id = "STU-10A-01",
            institutionId = tenantId,
            name = "Aarav Sharma",
            grade = "10",
            section = "A",
            rollNumber = 1,
            admissionNumber = "SXA-2026-1001",
            gender = "Male",
            guardianName = "Vikram Sharma",
            guardianPhone = "+91 98765 43210",
            email = "aarav.sharma@example.com",
            bloodGroup = "O+",
            attendancePercentage = 85.0f,
            feeDue = 0.0,
            feePaid = 25000.0
        )
        val s2 = StudentEntity(
            id = "STU-10A-02",
            institutionId = tenantId,
            name = "Diya Patel",
            grade = "10",
            section = "A",
            rollNumber = 2,
            admissionNumber = "SXA-2026-1002",
            gender = "Female",
            guardianName = "Kiran Patel",
            guardianPhone = "+91 98765 43211",
            email = "diya.patel@example.com",
            bloodGroup = "B+",
            attendancePercentage = 90.0f,
            feeDue = 0.0,
            feePaid = 25000.0
        )
        schoolRepository.insertStudent(s1)
        schoolRepository.insertStudent(s2)

        // 2. Save batch attendance directly to Room database
        val dateKey = "2026-09-20"
        val attendances = listOf(
            AttendanceEntity(
                institutionId = tenantId,
                studentId = s1.id,
                studentName = s1.name,
                classSection = "10-A",
                date = dateKey,
                dateTimestamp = System.currentTimeMillis(),
                status = "PRESENT",
                session = "Morning Assembly",
                markedByTeacherId = teacherId,
                remarks = "On time",
                isVerifiedByIncharge = true,
                isApprovedByPrincipal = true
            ),
            AttendanceEntity(
                institutionId = tenantId,
                studentId = s2.id,
                studentName = s2.name,
                classSection = "10-A",
                date = dateKey,
                dateTimestamp = System.currentTimeMillis(),
                status = "ABSENT",
                session = "Morning Assembly",
                markedByTeacherId = teacherId,
                remarks = "Sick leave reported by guardian",
                isVerifiedByIncharge = true,
                isApprovedByPrincipal = true
            )
        )

        attendanceRepository.saveDailyAttendanceBatch(
            classSection = "10-A",
            date = dateKey,
            attendances = attendances,
            updateStudentPercentages = true
        )

        // 3. Query Room database and verify records
        val savedInDb = attendanceRepository.getAttendanceForClassAndDate("10-A", dateKey)
        assertEquals(2, savedInDb.size)

        val aaravRecord = savedInDb.find { it.studentId == s1.id }
        assertNotNull(aaravRecord)
        assertEquals("PRESENT", aaravRecord?.status)
        assertEquals("On time", aaravRecord?.remarks)

        val diyaRecord = savedInDb.find { it.studentId == s2.id }
        assertNotNull(diyaRecord)
        assertEquals("ABSENT", diyaRecord?.status)
        assertEquals("Sick leave reported by guardian", diyaRecord?.remarks)

        // 4. Verify mirrored records created in attendance_records with isSyncedToCentral = false
        val records = attendanceRepository.observeAttendanceForPerson(s1.id).first()
        assertTrue(records.any { it.status == "PRESENT" && it.sourceMethod == "DAILY_ATTENDANCE_UI" })
        val record = records.first { it.status == "PRESENT" }
        assertFalse("Must not claim synced until server ack", record.isSyncedToCentral)

        // 5. Verify atomic outbox queue entries
        val pendingOutbox = db.schoolDao().getPendingOutbox(tenantId).first()
        assertEquals(2, pendingOutbox.size)
        assertTrue(pendingOutbox.all { it.status == DurableOutboxEntity.STATUS_PENDING })
    }

    // =========================================================================
    // 2. STATUS VALIDATION
    // =========================================================================

    @Test
    fun testStatusValidationRejectsInvalidStatus() = runBlocking {
        val s1 = StudentEntity(
            id = "STU-VAL-01",
            institutionId = tenantId,
            name = "Test Student",
            grade = "10",
            section = "A",
            rollNumber = 1,
            admissionNumber = "VAL-01",
            gender = "Male",
            guardianName = "Guardian",
            guardianPhone = "1234567890",
            email = "val@test.com"
        )
        schoolRepository.insertStudent(s1)

        val invalidAttendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = s1.id,
            studentName = s1.name,
            classSection = "10-A",
            date = "2026-09-20",
            status = "MAYBE" // INVALID STATUS
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch(
                classSection = "10-A",
                date = "2026-09-20",
                attendances = listOf(invalidAttendance)
            )
            fail("Expected IllegalArgumentException for invalid status")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Invalid status 'MAYBE'") == true)
        }
    }

    @Test
    fun testStatusValidationRejectsEmptyRoster() = runBlocking {
        try {
            attendanceRepository.saveDailyAttendanceBatch(
                classSection = "10-A",
                date = "2026-09-20",
                attendances = emptyList()
            )
            fail("Expected IllegalArgumentException for empty attendance batch")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Attendance roster cannot be empty") == true)
        }
    }

    // =========================================================================
    // 3. TEACHER AUTHORIZATION & CLASS ASSIGNMENT
    // =========================================================================

    @Test
    fun testTeacherAuthorizationAssignedClassPermitted() = runBlocking {
        val s1 = StudentEntity(
            id = "STU-AUTH-01",
            institutionId = tenantId,
            name = "Assigned Class Scholar",
            grade = "10",
            section = "A",
            rollNumber = 1,
            admissionNumber = "AUTH-01",
            gender = "Male",
            guardianName = "Parent",
            guardianPhone = "1234567890",
            email = "auth@test.com"
        )
        schoolRepository.insertStudent(s1)

        // Teacher FAC-TCH-01 is assigned to 10-A
        val attendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = s1.id,
            studentName = s1.name,
            classSection = "10-A",
            date = "2026-09-20",
            status = "PRESENT"
        )

        attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(attendance))
        val saved = attendanceRepository.getAttendanceForClassAndDate("10-A", "2026-09-20")
        assertEquals(1, saved.size)
    }

    @Test
    fun testTeacherAuthorizationUnassignedClassRejected() = runBlocking {
        val s1 = StudentEntity(
            id = "STU-AUTH-02",
            institutionId = tenantId,
            name = "Unassigned Class Scholar",
            grade = "11",
            section = "B",
            rollNumber = 1,
            admissionNumber = "AUTH-02",
            gender = "Male",
            guardianName = "Parent",
            guardianPhone = "1234567890",
            email = "auth2@test.com"
        )
        schoolRepository.insertStudent(s1)

        // Teacher FAC-TCH-01 is NOT assigned to 11-B
        val attendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = s1.id,
            studentName = s1.name,
            classSection = "11-B",
            date = "2026-09-20",
            status = "PRESENT"
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch("11-B", "2026-09-20", listOf(attendance))
            fail("Expected SecurityException when teacher attempts to mark unassigned class")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("is not assigned to class '11-B'") == true)
        }
    }

    @Test
    fun testUnauthorizedRoleRejectedFromMarkingAttendance() = runBlocking {
        // Set session to STUDENT role
        setSession(userId = "STU-001", role = UserRole.STUDENT, institution = tenantId, permissions = emptySet())

        val attendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = "STU-001",
            studentName = "Self Mark",
            classSection = "10-A",
            date = "2026-09-20",
            status = "PRESENT"
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(attendance))
            fail("Expected SecurityException for unauthorized role")
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("does not have permission to mark attendance") == true)
        }
    }

    // =========================================================================
    // 4. STUDENT CLASS MEMBERSHIP VALIDATION
    // =========================================================================

    @Test
    fun testStudentMembershipValidationRejectsNonEnrolledStudent() = runBlocking {
        // Scholar belongs to Grade 9 Section A
        val sWrongClass = StudentEntity(
            id = "STU-GRADE9-01",
            institutionId = tenantId,
            name = "Junior Student",
            grade = "9",
            section = "A",
            rollNumber = 1,
            admissionNumber = "JRN-01",
            gender = "Male",
            guardianName = "Parent",
            guardianPhone = "1234567890",
            email = "jr@test.com"
        )
        schoolRepository.insertStudent(sWrongClass)

        // Attempting to submit attendance for Grade 9 student under 10-A batch
        val attendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = sWrongClass.id,
            studentName = sWrongClass.name,
            classSection = "10-A",
            date = "2026-09-20",
            status = "PRESENT"
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(attendance))
            fail("Expected IllegalArgumentException for student not belonging to class 10-A")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("is enrolled in 9-A, not in class '10-A'") == true)
        }
    }

    // =========================================================================
    // 5. DUPLICATE ATTENDANCE DETECTION
    // =========================================================================

    @Test
    fun testDuplicateAttendanceDetectionAndSessionLookup() = runBlocking {
        val s1 = StudentEntity(
            id = "STU-DUP-CHECK",
            institutionId = tenantId,
            name = "Session Scholar",
            grade = "10",
            section = "A",
            rollNumber = 5,
            admissionNumber = "DUP-05",
            gender = "Male",
            guardianName = "Parent",
            guardianPhone = "1234567890",
            email = "dup@test.com"
        )
        schoolRepository.insertStudent(s1)

        val morningAttendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = s1.id,
            studentName = s1.name,
            classSection = "10-A",
            date = "2026-09-20",
            session = "Morning Assembly",
            status = "PRESENT"
        )

        attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(morningAttendance))

        // Check duplicate detection lookup
        val existingSession = attendanceRepository.getAttendanceForClassDateAndSession(
            classSection = "10-A",
            date = "2026-09-20",
            session = "Morning Assembly"
        )
        assertEquals(1, existingSession.size)
        assertEquals("Morning Assembly", existingSession[0].session)

        // Different session should return empty
        val afternoonSession = attendanceRepository.getAttendanceForClassDateAndSession(
            classSection = "10-A",
            date = "2026-09-20",
            session = "Afternoon Session"
        )
        assertTrue(afternoonSession.isEmpty())
    }

    // =========================================================================
    // 6. SERVER ACKNOWLEDGEMENT & NO FAKE ACKS
    // =========================================================================

    @Test
    fun testServerAcknowledgementMarksAttendanceRecordSynced() = runBlocking {
        val confirmedToken = "ACK-FS-CONFIRMED-999"
        val serverTimestamp = System.currentTimeMillis()

        val mockDispatcher = object : CloudOutboxDispatcher {
            override suspend fun dispatch(item: DurableOutboxEntity): DurableOutboxManager.CloudDispatchResult {
                return DurableOutboxManager.CloudDispatchResult.Success(
                    ackToken = confirmedToken,
                    serverTimestamp = serverTimestamp
                )
            }
        }

        val syncingOutbox = DurableOutboxManager(
            dao = db.schoolDao(),
            cloudDispatcher = mockDispatcher,
            defaultInstitutionId = tenantId
        )

        val syncingRepo = AttendanceRepositoryImpl(db.schoolDao(), syncingOutbox)

        val s1 = StudentEntity(
            id = "STU-ACK-01",
            institutionId = tenantId,
            name = "Ack Scholar",
            grade = "10",
            section = "A",
            rollNumber = 10,
            admissionNumber = "ACK-10",
            gender = "Male",
            guardianName = "Parent",
            guardianPhone = "1234567890",
            email = "ack@test.com"
        )
        schoolRepository.insertStudent(s1)

        val attendance = AttendanceEntity(
            institutionId = tenantId,
            studentId = s1.id,
            studentName = s1.name,
            classSection = "10-A",
            date = "2026-09-20",
            session = "Morning Assembly",
            status = "PRESENT"
        )

        syncingRepo.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(attendance))

        // Verify initial state: isSyncedToCentral == false
        var records = db.schoolDao().getAllAttendanceRecords().first()
        val studentRecord = records.find { it.personId == s1.id }
        assertNotNull(studentRecord)
        assertFalse("Must be false prior to server ACK", studentRecord!!.isSyncedToCentral)

        // Flush outbox to simulate online network sync
        val flushResult = syncingOutbox.flushQueue(tenantId, forceAll = true)
        assertTrue(flushResult.isSuccess)
        val batchResult = flushResult.getOrThrow()
        assertEquals(1, batchResult.acknowledgedCount)

        // Now verify genuine server ACK updated Room DB record to isSyncedToCentral = true
        records = db.schoolDao().getAllAttendanceRecords().first()
        val updatedRecord = records.find { it.personId == s1.id }
        assertNotNull(updatedRecord)
        assertTrue("Record must now be marked as synced after genuine server ACK", updatedRecord!!.isSyncedToCentral)
    }

    // =========================================================================
    // 7. CROSS-TENANT REJECTION
    // =========================================================================

    @Test
    fun testCrossTenantRejection() = runBlocking {
        val foreignTenant = "INST-FOREIGN-999"
        val foreignAttendance = AttendanceEntity(
            institutionId = foreignTenant,
            studentId = "STU-FOREIGN-01",
            studentName = "Foreign Student",
            classSection = "10-A",
            date = "2026-09-20",
            status = "PRESENT"
        )

        try {
            attendanceRepository.saveDailyAttendanceBatch("10-A", "2026-09-20", listOf(foreignAttendance))
            fail("Expected TenantSecurityException on cross-tenant submission")
        } catch (e: TenantSecurityException) {
            assertTrue(e.message?.contains("Tenant isolation violation") == true)
        }
    }
}
