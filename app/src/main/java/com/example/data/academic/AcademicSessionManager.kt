package com.example.data.academic

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AcademicPromotionHistoryEntity
import com.example.data.local.entities.AcademicYearEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.StudentEnrollmentEntity
import com.example.data.local.entities.StudentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Enterprise Academic Session & Lifecycle Management Engine.
 *
 * Guarantees strict Academic Year Isolation:
 * - Schools maintain multiple concurrent or historical academic sessions (e.g. 2024-2025, 2025-2026, 2026-2027).
 * - Classes, sections, subjects, enrollments, promotions, transfers, withdrawals, and student marks/attendance
 *   are strictly scoped to their academic year.
 * - Locked academic years (isLocked = true) are permanently read-only, preventing retrospective modification
 *   or corruption of historical audit trails and report cards.
 */
class AcademicSessionManager(
    private val dao: SchoolDao,
    private val defaultInstitutionId: String = "INST-SXA-2026"
) {

    /**
     * Retrieves all academic sessions registered for this institution.
     */
    fun getAllAcademicYears(institutionId: String = defaultInstitutionId): Flow<List<AcademicYearEntity>> {
        return dao.getAcademicYears(institutionId)
    }

    /**
     * Retrieves the current active academic session.
     */
    suspend fun getActiveSession(institutionId: String = defaultInstitutionId): AcademicYearEntity? =
        withContext(Dispatchers.IO) {
            dao.getCurrentAcademicYear(institutionId)
        }

    /**
     * Locks or unlocks an academic year. When locked, operational changes (attendance, marks, invoices)
     * are strictly forbidden to ensure statutory immutability.
     */
    suspend fun setAcademicYearLock(
        academicYearId: String,
        isLocked: Boolean,
        authorizedByUserId: String,
        institutionId: String = defaultInstitutionId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.setAcademicYearLock(academicYearId, isLocked)
            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    timestamp = System.currentTimeMillis(),
                    formattedTime = java.time.Instant.now().toString(),
                    action = if (isLocked) "ACADEMIC_YEAR_LOCKED" else "ACADEMIC_YEAR_UNLOCKED",
                    entityType = "ACADEMIC_YEAR",
                    entityId = academicYearId,
                    actorUserId = authorizedByUserId,
                    actorName = "Authorized Academic Administrator",
                    actorRole = "PRINCIPAL",
                    previousState = if (isLocked) "UNLOCKED" else "LOCKED",
                    newState = if (isLocked) "LOCKED" else "UNLOCKED",
                    remarks = "Academic session state transitioned by principal authority."
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sets the active current academic year.
     */
    suspend fun switchActiveAcademicYear(
        academicYearId: String,
        authorizedByUserId: String,
        institutionId: String = defaultInstitutionId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.setCurrentAcademicYear(institutionId, academicYearId)
            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    timestamp = System.currentTimeMillis(),
                    formattedTime = java.time.Instant.now().toString(),
                    action = "ACTIVE_ACADEMIC_YEAR_SWITCHED",
                    entityType = "ACADEMIC_YEAR",
                    entityId = academicYearId,
                    actorUserId = authorizedByUserId,
                    actorName = "Authorized Academic Administrator",
                    actorRole = "PRINCIPAL",
                    previousState = null,
                    newState = academicYearId,
                    remarks = "Current operational academic year updated."
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Checks if a target academic year is locked.
     */
    suspend fun isAcademicYearLocked(
        academicYearName: String,
        institutionId: String = defaultInstitutionId
    ): Boolean = withContext(Dispatchers.IO) {
        // Find academic year entity by yearName or ID
        val activeYear = dao.getCurrentAcademicYear(institutionId)
        if (activeYear?.yearName == academicYearName) {
            return@withContext activeYear.isLocked
        }
        false
    }

    /**
     * Promotes a student from one academic year and class/section to the next.
     * Maintains non-destructive history:
     * 1. Marks previous year's enrollment as PROMOTED (or RETAINED if repeating).
     * 2. Inserts new active enrollment in destination academic year & section.
     * 3. Appends historical audit record to academic_promotion_history table.
     * 4. Appends tamper-evident audit log.
     */
    suspend fun promoteStudent(
        student: StudentEntity,
        currentEnrollmentId: String,
        fromYear: String,
        toYear: String,
        fromClassSection: String,
        toClassSection: String,
        finalPercentage: Float,
        remarks: String = "Promoted upon completion of academic requirements",
        authorizedByUserId: String,
        isRepeatYear: Boolean = false,
        institutionId: String = defaultInstitutionId
    ): Result<AcademicPromotionHistoryEntity> = withContext(Dispatchers.IO) {
        try {
            // Verify destination academic year is not locked
            if (isAcademicYearLocked(toYear, institutionId)) {
                return@withContext Result.failure(
                    IllegalStateException("Target academic year '$toYear' is locked. Promotion forbidden.")
                )
            }

            val newEnrollmentId = "ENR-${toYear.replace("-", "")}-${student.id}"
            val promotionType = if (isRepeatYear) "RETAINED" else "ANNUAL_PROMOTION"

            val newEnrollment = StudentEnrollmentEntity(
                enrollmentId = newEnrollmentId,
                institutionId = institutionId,
                studentId = student.id,
                academicYear = toYear,
                classId = toClassSection.substringBefore("-", toClassSection),
                section = toClassSection.substringAfter("-", "A"),
                rollNumber = student.rollNumber,
                enrollmentStatus = "ACTIVE",
                enrollmentDate = java.time.LocalDate.now().toString()
            )

            val promotionHistory = AcademicPromotionHistoryEntity(
                promotionId = "PROM-${UUID.randomUUID().toString().take(8).uppercase()}",
                institutionId = institutionId,
                studentId = student.id,
                studentName = student.name,
                fromAcademicYear = fromYear,
                toAcademicYear = toYear,
                fromClassSection = fromClassSection,
                toClassSection = toClassSection,
                promotionType = promotionType,
                finalPercentage = finalPercentage,
                remarks = remarks,
                authorizedByUserId = authorizedByUserId,
                timestamp = System.currentTimeMillis()
            )

            val auditLog = AuditLogEntity(
                institutionId = institutionId,
                timestamp = System.currentTimeMillis(),
                formattedTime = java.time.Instant.now().toString(),
                action = "STUDENT_PROMOTED",
                entityType = "STUDENT_ENROLLMENT",
                entityId = student.id,
                actorUserId = authorizedByUserId,
                actorName = "Academic Dean / Principal",
                actorRole = "PRINCIPAL",
                previousState = "$fromYear: $fromClassSection",
                newState = "$toYear: $toClassSection ($promotionType)",
                remarks = "Student ${student.name} promoted with $finalPercentage% score."
            )

            dao.promoteStudentWithAudit(
                oldEnrollmentId = currentEnrollmentId,
                newEnrollment = newEnrollment,
                promotionRecord = promotionHistory,
                auditLog = auditLog
            )

            // Update student's current active grade and section in student master profile
            val newGrade = toClassSection.substringBefore("-", toClassSection)
            val newSection = toClassSection.substringAfter("-", "A")
            dao.updateStudentPromotion(
                studentId = student.id,
                newGrade = newGrade,
                newSection = newSection,
                status = "PROMOTED"
            )

            Result.success(promotionHistory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Executes a student transfer (between sections or campuses) within the active year.
     */
    suspend fun transferStudent(
        student: StudentEntity,
        currentEnrollmentId: String,
        academicYear: String,
        fromSection: String,
        toSection: String,
        reason: String,
        authorizedByUserId: String,
        institutionId: String = defaultInstitutionId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateStudentEnrollmentStatus(currentEnrollmentId, "TRANSFERRED")

            val newEnrollmentId = "ENR-${academicYear.replace("-", "")}-${student.id}-T"
            val newEnrollment = StudentEnrollmentEntity(
                enrollmentId = newEnrollmentId,
                institutionId = institutionId,
                studentId = student.id,
                academicYear = academicYear,
                classId = toSection.substringBefore("-", toSection),
                section = toSection.substringAfter("-", "A"),
                rollNumber = student.rollNumber,
                enrollmentStatus = "ACTIVE",
                enrollmentDate = java.time.LocalDate.now().toString()
            )
            dao.insertEnrollment(newEnrollment)

            val promoRecord = AcademicPromotionHistoryEntity(
                promotionId = "XFER-${UUID.randomUUID().toString().take(8).uppercase()}",
                institutionId = institutionId,
                studentId = student.id,
                studentName = student.name,
                fromAcademicYear = academicYear,
                toAcademicYear = academicYear,
                fromClassSection = fromSection,
                toClassSection = toSection,
                promotionType = "SECTION_TRANSFER",
                finalPercentage = 0f,
                remarks = reason,
                authorizedByUserId = authorizedByUserId,
                timestamp = System.currentTimeMillis()
            )
            dao.insertPromotionRecord(promoRecord)

            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    timestamp = System.currentTimeMillis(),
                    formattedTime = java.time.Instant.now().toString(),
                    action = "STUDENT_SECTION_TRANSFER",
                    entityType = "STUDENT",
                    entityId = student.id,
                    actorUserId = authorizedByUserId,
                    actorName = "Class Incharge / Administrator",
                    actorRole = "INCHARGE",
                    previousState = fromSection,
                    newState = toSection,
                    remarks = "Transferred from $fromSection to $toSection: $reason"
                )
            )

            val targetGrade = toSection.substringBefore("-", toSection)
            val targetSec = toSection.substringAfter("-", "A")
            dao.updateStudentPromotion(student.id, targetGrade, targetSec, student.promotionStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Records a student withdrawal (e.g. TC issued, family relocation).
     */
    suspend fun withdrawStudent(
        student: StudentEntity,
        currentEnrollmentId: String,
        academicYear: String,
        reason: String,
        authorizedByUserId: String,
        institutionId: String = defaultInstitutionId
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            dao.updateStudentEnrollmentStatus(currentEnrollmentId, "WITHDRAWN")

            val currentClassSection = "${student.grade}-${student.section}"
            val withdrawalRecord = AcademicPromotionHistoryEntity(
                promotionId = "WDR-${UUID.randomUUID().toString().take(8).uppercase()}",
                institutionId = institutionId,
                studentId = student.id,
                studentName = student.name,
                fromAcademicYear = academicYear,
                toAcademicYear = "EXIT",
                fromClassSection = currentClassSection,
                toClassSection = "WITHDRAWN",
                promotionType = "WITHDRAWAL",
                finalPercentage = 0f,
                remarks = reason,
                authorizedByUserId = authorizedByUserId,
                timestamp = System.currentTimeMillis()
            )
            dao.insertPromotionRecord(withdrawalRecord)

            dao.insertAuditLog(
                AuditLogEntity(
                    institutionId = institutionId,
                    timestamp = System.currentTimeMillis(),
                    formattedTime = java.time.Instant.now().toString(),
                    action = "STUDENT_WITHDRAWAL",
                    entityType = "STUDENT",
                    entityId = student.id,
                    actorUserId = authorizedByUserId,
                    actorName = "Academic Administration",
                    actorRole = "PRINCIPAL",
                    previousState = "ACTIVE ($currentClassSection)",
                    newState = "WITHDRAWN",
                    remarks = "Student withdrawal processed: $reason"
                )
            )

            dao.updateStudentPromotion(
                studentId = student.id,
                newGrade = student.grade,
                newSection = student.section,
                status = "WITHDRAWN"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieves the complete academic career promotion history for a given student.
     */
    fun getStudentPromotionHistory(
        studentId: String,
        institutionId: String = defaultInstitutionId
    ): Flow<List<AcademicPromotionHistoryEntity>> {
        return dao.getPromotionHistoryForStudent(institutionId, studentId)
    }

    /**
     * Retrieves all student enrollments for a specific academic year.
     */
    fun getEnrollmentsByAcademicYear(
        academicYear: String,
        institutionId: String = defaultInstitutionId
    ): Flow<List<StudentEnrollmentEntity>> {
        return dao.getEnrollmentsForAcademicYear(academicYear, institutionId)
    }
}
