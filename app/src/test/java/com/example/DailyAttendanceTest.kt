package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.AttendanceRepositoryImpl
import com.example.data.repository.SchoolRepository
import com.example.ui.viewmodel.TeacherAttendanceViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        schoolRepository = SchoolRepository(db.schoolDao())
        attendanceRepository = AttendanceRepositoryImpl(db.schoolDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun testAttendanceSavingAndRoomPersistence() = runBlocking {
        // 1. Insert test scholars into Room database
        val s1 = StudentEntity(
            id = "STU-10A-01",
            institutionId = "INST-SXA-2026",
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
            institutionId = "INST-SXA-2026",
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
                institutionId = "INST-SXA-2026",
                studentId = s1.id,
                studentName = s1.name,
                classSection = "10-A",
                date = dateKey,
                dateTimestamp = System.currentTimeMillis(),
                status = "PRESENT",
                session = "Morning Assembly",
                markedByTeacherId = "FAC-TCH-01",
                remarks = "On time",
                isVerifiedByIncharge = true,
                isApprovedByPrincipal = true
            ),
            AttendanceEntity(
                institutionId = "INST-SXA-2026",
                studentId = s2.id,
                studentName = s2.name,
                classSection = "10-A",
                date = dateKey,
                dateTimestamp = System.currentTimeMillis(),
                status = "ABSENT",
                session = "Morning Assembly",
                markedByTeacherId = "FAC-TCH-01",
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

        // 4. Verify mirrored audit records created in Room DB
        val records = attendanceRepository.observeAttendanceForPerson(s1.id).first()
        assertTrue(records.any { it.status == "PRESENT" && it.sourceMethod == "DAILY_ATTENDANCE_UI" })
    }
}
