package com.example

import com.example.data.local.entities.ExamRecordEntity
import com.example.data.local.entities.FeeRecordEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.AiAcademicAssistant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiAcademicToolsTest {

    private lateinit var aiAssistant: AiAcademicAssistant

    @Before
    fun setup() {
        aiAssistant = AiAcademicAssistant()
    }

    @Test
    fun testGenerateStudentPerformanceRemarks() = runBlocking {
        val student = StudentEntity(
            id = "STU-10A-01",
            admissionNumber = "SXA-2026-1001",
            name = "Aarav Sharma",
            grade = "10",
            section = "A",
            rollNumber = 1,
            attendancePercentage = 94.5f
        )

        val exams = listOf(
            ExamRecordEntity(
                id = "EXAM-1",
                studentId = "STU-10A-01",
                subject = "Physics",
                marksObtained = 92.0,
                maxMarks = 100.0,
                grade = "A+",
                term = "Term 1"
            ),
            ExamRecordEntity(
                id = "EXAM-2",
                studentId = "STU-10A-01",
                subject = "Mathematics",
                marksObtained = 95.0,
                maxMarks = 100.0,
                grade = "A+",
                term = "Term 1"
            )
        )

        val remark = aiAssistant.generateStudentPerformanceRemarks(
            student = student,
            exams = exams,
            attendancePercentage = 94.5f
        )

        assertNotNull(remark)
        assertTrue(remark.isNotBlank())
        assertTrue(remark.contains("Aarav Sharma") || remark.contains("scholar") || remark.contains("attendance"))
    }

    @Test
    fun testDraftSmartCircular() = runBlocking {
        val circular = aiAssistant.draftSmartCircular(
            title = "Annual Science Exhibition & Project Fair",
            circularType = "PARENT_CIRCULAR",
            targetAudience = "Parents & Guardians",
            keyPoints = "Exhibition will commence at 09:00 AM on 25th September. All projects to be installed by previous evening.",
            effectiveDate = "2026-09-22"
        )

        assertNotNull(circular)
        assertTrue(circular.isNotBlank())
        assertTrue(circular.contains("Science Exhibition") || circular.contains("CIRCULAR") || circular.contains("St. Xavier"))
    }

    @Test
    fun testGenerateTestPaperAndRubric() = runBlocking {
        val quiz = aiAssistant.generateTestPaperAndRubric(
            grade = "10",
            subject = "Science",
            topic = "Electromagnetism and Fleming's Rules",
            totalMarks = 25,
            difficultyLevel = "Standard",
            questionTypes = "MCQs + Short Answer + HOTS"
        )

        assertNotNull(quiz)
        assertTrue(quiz.isNotBlank())
        assertTrue(quiz.contains("Electromagnetism") || quiz.contains("SECTION") || quiz.contains("Marks") || quiz.contains("Question"))
    }

    @Test
    fun testAnalyzeAttendanceAndFeeRisks() = runBlocking {
        val students = listOf(
            StudentEntity(
                id = "STU-1",
                name = "Kunal Verma",
                grade = "10",
                section = "A",
                rollNumber = 12,
                attendancePercentage = 64.0f,
                guardianName = "Suresh Verma",
                guardianPhone = "9876543210"
            ),
            StudentEntity(
                id = "STU-2",
                name = "Priya Rao",
                grade = "10",
                section = "B",
                rollNumber = 5,
                attendancePercentage = 96.0f,
                guardianName = "Manoj Rao",
                guardianPhone = "9876543211"
            )
        )

        val feeRecords = listOf(
            FeeRecordEntity(
                id = "FEE-1",
                studentId = "STU-1",
                studentName = "Kunal Verma",
                gradeSection = "10-A",
                totalRupees = 45000.0,
                paidRupees = 20000.0,
                dueRupees = 25000.0,
                status = "OVERDUE",
                term = "Term 1"
            )
        )

        val analysis = aiAssistant.analyzeAttendanceAndFeeRisks(
            students = students,
            feeRecords = feeRecords
        )

        assertNotNull(analysis)
        assertTrue(analysis.isNotBlank())
        assertTrue(analysis.contains("ATTENDANCE") || analysis.contains("Kunal") || analysis.contains("75%") || analysis.contains("Fee"))
    }
}
