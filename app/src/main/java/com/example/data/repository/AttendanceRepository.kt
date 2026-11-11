package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.DurableOutboxEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import com.example.data.remote.sync.ConflictResolutionPolicy
import com.example.data.remote.sync.DurableOutboxManager
import com.example.security.TenantSecurityContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface AttendanceRepository {
    val allAttendance: Flow<List<AttendanceEntity>>
    val attendanceState: StateFlow<List<AttendanceEntity>>
    val allAttendanceRecords: Flow<List<AttendanceRecordEntity>>
    val attendanceRecordsState: StateFlow<List<AttendanceRecordEntity>>
    val allRegisterBatches: Flow<List<RegisterScanBatchEntity>>
    val registerBatchesState: StateFlow<List<RegisterScanBatchEntity>>

    fun observeAttendance(): Flow<List<AttendanceEntity>>
    fun observeAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>>
    fun observeAttendanceInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<AttendanceEntity>>
    fun observeAttendanceRecords(): Flow<List<AttendanceRecordEntity>>
    fun observeAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>>
    fun observeAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>>
    fun observeRegisterBatches(): Flow<List<RegisterScanBatchEntity>>

    suspend fun getAttendanceForClassAndDate(classSection: String, date: String): List<AttendanceEntity>
    suspend fun getAttendanceForClassDateAndSession(classSection: String, date: String, session: String): List<AttendanceEntity>
    suspend fun isTeacherAssignedToClass(teacherId: String, classSection: String): Boolean
    suspend fun saveDailyAttendanceBatch(
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        updateStudentPercentages: Boolean = true
    )
    suspend fun recordAttendance(attendance: AttendanceEntity)
    suspend fun recordAttendanceBatch(attendances: List<AttendanceEntity>)
    suspend fun insertAttendanceRecord(record: AttendanceRecordEntity)
    suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>)
    suspend fun commitRegisterScan(batch: RegisterScanBatchEntity, records: List<AttendanceRecordEntity>)
    suspend fun verifyBatchByIncharge(batchId: String)
    suspend fun approveBatchByPrincipal(batchId: String)
    suspend fun inchargeVerifyBatchWithAudit(batchId: String, auditLog: AuditLogEntity)
    suspend fun principalApproveBatchWithAudit(batchId: String, auditLog: AuditLogEntity)
    suspend fun applyBatchAttendanceAndSyncRoster(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>,
        percentageUpdates: Map<String, Float>
    )
}

@Singleton
class AttendanceRepositoryImpl @Inject constructor(
    private val dao: SchoolDao,
    private val outboxManager: DurableOutboxManager
) : AttendanceRepository {

    constructor(dao: SchoolDao) : this(
        dao = dao,
        outboxManager = DurableOutboxManager(dao)
    )

    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val allAttendance: Flow<List<AttendanceEntity>> = dao.getAllAttendance()
    override val attendanceState: StateFlow<List<AttendanceEntity>> = allAttendance
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val allAttendanceRecords: Flow<List<AttendanceRecordEntity>> = dao.getAllAttendanceRecords()
    override val attendanceRecordsState: StateFlow<List<AttendanceRecordEntity>> = allAttendanceRecords
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override val allRegisterBatches: Flow<List<RegisterScanBatchEntity>> = dao.getAllRegisterBatches()
    override val registerBatchesState: StateFlow<List<RegisterScanBatchEntity>> = allRegisterBatches
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    override fun observeAttendance(): Flow<List<AttendanceEntity>> = dao.getAllAttendance()
    override fun observeAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>> =
        dao.getAttendanceForStudent(studentId)
    override fun observeAttendanceInRange(startEpochMs: Long, endEpochMs: Long): Flow<List<AttendanceEntity>> =
        dao.getAttendanceInRange(startEpochMs, endEpochMs)
    override fun observeAttendanceRecords(): Flow<List<AttendanceRecordEntity>> =
        dao.getAllAttendanceRecords()
    override fun observeAttendanceForClass(classSection: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForClass(classSection)
    override fun observeAttendanceForPerson(personId: String): Flow<List<AttendanceRecordEntity>> =
        dao.getAttendanceForPerson(personId)
    override fun observeRegisterBatches(): Flow<List<RegisterScanBatchEntity>> =
        dao.getAllRegisterBatches()

    private fun resolveAuthenticatedTenant(preferredTenantId: String? = null): String {
        val sessionTenant = TenantSecurityContext.getActiveTenantIdOrNull()
            ?: com.example.security.SecureSessionManager.getActiveSession()?.institutionId

        if (sessionTenant != null) {
            // If the entity explicitly specified a foreign tenant (other than template default fallback "INST-SXA-2026"),
            // prevent cross-tenant mutation. If it has the template default or matches, resolve to authenticated session.
            if (preferredTenantId != null && preferredTenantId.isNotBlank() &&
                preferredTenantId != "INST-SXA-2026" && preferredTenantId != sessionTenant) {
                throw com.example.security.TenantSecurityException(
                    "Tenant isolation violation: Active authenticated session belongs to '$sessionTenant', cannot perform mutation for tenant '$preferredTenantId'."
                )
            }
            return sessionTenant
        }

        val fallback = preferredTenantId?.takeIf { it.isNotBlank() }
        return fallback ?: throw com.example.security.TenantSecurityException(
            "Unauthorized operation: Verified authenticated session required. No tenant context established."
        )
    }

    override suspend fun getAttendanceForClassAndDate(
        classSection: String,
        date: String
    ): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        dao.getAttendanceForClassAndDate(classSection.trim(), date.trim())
    }

    override suspend fun getAttendanceForClassDateAndSession(
        classSection: String,
        date: String,
        session: String
    ): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        dao.getAttendanceForClassDateAndSession(classSection.trim(), date.trim(), session.trim())
    }

    override suspend fun isTeacherAssignedToClass(
        teacherId: String,
        classSection: String
    ): Boolean = withContext(Dispatchers.IO) {
        val clean = classSection.trim()
        val parts = clean.split("-")
        val gradePart = parts.getOrNull(0) ?: clean
        val sectionPart = parts.getOrNull(1) ?: "A"

        // 1. Check in academic_classes
        val classes = dao.getAllClassesList()
        val isClassTeacher = classes.any { c ->
            val matchSection = c.section.equals(sectionPart, ignoreCase = true)
            val matchGrade = c.className.contains(gradePart, ignoreCase = true) ||
                    c.classId.contains(clean.replace("-", ""), ignoreCase = true)
            matchGrade && matchSection && c.classTeacherId == teacherId
        }
        if (isClassTeacher) return@withContext true

        // 2. Check in staff_members
        val staff = dao.getStaffById(teacherId)
        if (staff != null && staff.isClassIncharge && !staff.inchargeClass.isNullOrBlank()) {
            if (staff.inchargeClass.equals(clean, ignoreCase = true)) return@withContext true
        }

        // 3. Check in teachers
        val teacher = dao.getTeacherById(teacherId)
        if (teacher != null && teacher.isClassIncharge && !teacher.inchargeClassSection.isNullOrBlank()) {
            if (teacher.inchargeClassSection.equals(clean, ignoreCase = true)) return@withContext true
        }

        false
    }

    override suspend fun saveDailyAttendanceBatch(
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        updateStudentPercentages: Boolean
    ) = withContext(Dispatchers.IO) {
        val cleanClass = classSection.trim()
        val cleanDate = date.trim()

        // 1. Validate non-empty roster
        if (attendances.isEmpty()) {
            throw IllegalArgumentException("Validation error: Attendance roster cannot be empty.")
        }

        // 2. Validate attendance statuses (PRESENT, ABSENT, LATE, or LEAVE)
        val validStatuses = setOf("PRESENT", "ABSENT", "LATE", "LEAVE")
        for (att in attendances) {
            val statusUpper = att.status.trim().uppercase()
            if (statusUpper !in validStatuses) {
                throw IllegalArgumentException(
                    "Validation error: Invalid status '${att.status}' for student '${att.studentName}'. Status must be Present, Absent, or Late."
                )
            }
        }

        // 3. Enforce Tenant Isolation
        val resolvedTenant = resolveAuthenticatedTenant(attendances.firstOrNull()?.institutionId)

        // 4. Enforce Role Permissions and Class Assignment Authorization
        val activeClaims = TenantSecurityContext.getCurrentClaims()
            ?: com.example.security.SecureSessionManager.getActiveSession()?.claims

        if (activeClaims != null) {
            val role = activeClaims.role
            val isSystemAdmin = role in setOf(
                com.example.data.model.UserRole.ADMIN,
                com.example.data.model.UserRole.PRINCIPAL,
                com.example.data.model.UserRole.VICE_PRINCIPAL
            )
            val isTeacherOrIncharge = role in setOf(
                com.example.data.model.UserRole.TEACHER,
                com.example.data.model.UserRole.TEACHING,
                com.example.data.model.UserRole.INCHARGE
            )

            if (!isSystemAdmin && !isTeacherOrIncharge && !activeClaims.hasPermission(com.example.data.model.SchoolPermissions.MARK_ATTENDANCE_SECTION)) {
                throw SecurityException("Unauthorized: User role '${role.name}' does not have permission to mark attendance.")
            }

            if (isTeacherOrIncharge && !isSystemAdmin) {
                val assignedSection = activeClaims.assignedClassSection
                val isAssigned = (assignedSection != null && assignedSection.equals(cleanClass, ignoreCase = true)) ||
                        isTeacherAssignedToClass(activeClaims.userId, cleanClass)

                if (!isAssigned) {
                    throw SecurityException(
                        "Access Denied: Faculty member '${activeClaims.displayName}' is not assigned to class '$cleanClass'. Only assigned teachers or administrators can record attendance."
                    )
                }
            }
        }

        // 5. Validate Student Membership
        val classParts = cleanClass.split("-")
        val expectedGrade = classParts.getOrNull(0)?.trim() ?: cleanClass
        val expectedSection = classParts.getOrNull(1)?.trim() ?: "A"

        for (att in attendances) {
            val student = dao.getStudentById(att.studentId)
            if (student != null) {
                val gradeMatches = student.grade.equals(expectedGrade, ignoreCase = true) ||
                        student.grade.contains(expectedGrade, ignoreCase = true)
                val sectionMatches = student.section.equals(expectedSection, ignoreCase = true)
                if (!gradeMatches || !sectionMatches) {
                    throw IllegalArgumentException(
                        "Student membership validation error: Student '${student.name}' (${student.id}) is enrolled in ${student.grade}-${student.section}, not in class '$cleanClass'."
                    )
                }
            }
        }

        val tenantAttendances = attendances.map { att ->
            att.copy(
                institutionId = resolvedTenant,
                classSection = cleanClass,
                date = cleanDate,
                status = att.status.trim().uppercase()
            )
        }

        // Prepare mirrored records with isSyncedToCentral = false (pending cloud sync)
        val recordEntities = tenantAttendances.map { att ->
            AttendanceRecordEntity(
                institutionId = resolvedTenant,
                personId = att.studentId,
                personName = att.studentName,
                roleType = "STUDENT",
                date = att.date,
                status = att.status,
                timeMarked = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(att.dateTimestamp)),
                remarks = att.remarks,
                markedByRole = activeClaims?.role?.name ?: "TEACHER",
                markedByName = activeClaims?.displayName ?: "Faculty In-Charge",
                classSection = att.classSection,
                sessionType = att.session,
                sourceMethod = "DAILY_ATTENDANCE_UI",
                isSyncedToCentral = false
            )
        }

        // Prepare durable outbox mutation items for each attendance entry
        val outboxItems = tenantAttendances.map { att ->
            val payload = JSONObject().apply {
                put("studentId", att.studentId)
                put("studentName", att.studentName)
                put("classSection", att.classSection)
                put("date", att.date)
                put("status", att.status)
                put("session", att.session)
                put("markedByTeacherId", att.markedByTeacherId)
                put("remarks", att.remarks)
                put("dateTimestamp", att.dateTimestamp)
                put("institutionId", resolvedTenant)
            }.toString()

            val entityId = "${att.classSection.trim()}_${att.date.trim()}_${att.studentId.trim()}"
            val idempotencyKey = "ATT-${resolvedTenant}-${entityId}"

            outboxManager.createOutboxEntity(
                entityType = "ATTENDANCE_RECORD",
                entityId = entityId,
                operation = "CREATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = idempotencyKey
            )
        }

        // Commit atomically to Room: deletion, batch insert, records, and outbox queue in one transaction
        dao.saveDailyAttendanceBatchWithOutbox(
            classSection = cleanClass,
            date = cleanDate,
            attendances = tenantAttendances,
            records = recordEntities,
            outboxItems = outboxItems
        )

        if (updateStudentPercentages) {
            tenantAttendances.forEach { att ->
                val student = dao.getStudentById(att.studentId)
                if (student != null) {
                    val isPresentOrLate = att.status.equals("PRESENT", ignoreCase = true) || att.status.equals("LATE", ignoreCase = true)
                    val currentPct = student.attendancePercentage
                    val updatedPct = if (isPresentOrLate) {
                        (currentPct * 0.95f + 100f * 0.05f).coerceIn(50f, 100f)
                    } else {
                        (currentPct * 0.95f + 0f * 0.05f).coerceIn(40f, 100f)
                    }
                    dao.updateStudentAttendancePercentage(student.id, (Math.round(updatedPct * 10f) / 10f).toFloat())
                }
            }
        }
    }

    override suspend fun recordAttendance(attendance: AttendanceEntity) = withContext(Dispatchers.IO) {
        saveDailyAttendanceBatch(attendance.classSection, attendance.date, listOf(attendance), false)
    }

    override suspend fun recordAttendanceBatch(attendances: List<AttendanceEntity>) = withContext(Dispatchers.IO) {
        val first = attendances.firstOrNull() ?: return@withContext
        saveDailyAttendanceBatch(first.classSection, first.date, attendances, true)
    }

    override suspend fun insertAttendanceRecord(record: AttendanceRecordEntity) = withContext(Dispatchers.IO) {
        val resolvedTenant = resolveAuthenticatedTenant(record.institutionId)
        val tenantRecord = record.copy(institutionId = resolvedTenant, isSyncedToCentral = false)

        val payload = JSONObject().apply {
            put("personId", tenantRecord.personId)
            put("personName", tenantRecord.personName)
            put("roleType", tenantRecord.roleType)
            put("date", tenantRecord.date)
            put("status", tenantRecord.status)
            put("timeMarked", tenantRecord.timeMarked)
            put("remarks", tenantRecord.remarks)
            put("classSection", tenantRecord.classSection)
            put("institutionId", resolvedTenant)
        }.toString()

        val entityId = "${tenantRecord.classSection.trim()}_${tenantRecord.date.trim()}_${tenantRecord.personId.trim()}"
        val idempotencyKey = "REC-${resolvedTenant}-${entityId}"

        val outboxItem = outboxManager.createOutboxEntity(
            entityType = "ATTENDANCE_RECORD",
            entityId = entityId,
            operation = "CREATE",
            payloadJson = payload,
            isHighIntegrity = true,
            policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
            institutionId = resolvedTenant,
            customIdempotencyKey = idempotencyKey
        )

        dao.insertAttendanceRecordWithOutbox(tenantRecord, outboxItem)
    }

    override suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>) = withContext(Dispatchers.IO) {
        if (records.isEmpty()) return@withContext
        val resolvedTenant = resolveAuthenticatedTenant(records.firstOrNull()?.institutionId)
        val tenantRecords = records.map { it.copy(institutionId = resolvedTenant, isSyncedToCentral = false) }

        val outboxItems = tenantRecords.map { rec ->
            val payload = JSONObject().apply {
                put("personId", rec.personId)
                put("personName", rec.personName)
                put("roleType", rec.roleType)
                put("date", rec.date)
                put("status", rec.status)
                put("timeMarked", rec.timeMarked)
                put("remarks", rec.remarks)
                put("classSection", rec.classSection)
                put("institutionId", resolvedTenant)
            }.toString()

            val entityId = "${rec.classSection.trim()}_${rec.date.trim()}_${rec.personId.trim()}"
            val idempotencyKey = "REC-${resolvedTenant}-${entityId}"

            outboxManager.createOutboxEntity(
                entityType = "ATTENDANCE_RECORD",
                entityId = entityId,
                operation = "CREATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = idempotencyKey
            )
        }

        dao.insertAttendanceRecordsWithOutbox(tenantRecords, outboxItems)
    }

    override suspend fun commitRegisterScan(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>
    ) = withContext(Dispatchers.IO) {
        val resolvedTenant = resolveAuthenticatedTenant(batch.institutionId)

        val tenantBatch = batch.copy(
            institutionId = resolvedTenant,
            inchargeVerified = false,
            principalApproved = false,
            status = "PENDING_VERIFICATION"
        )
        val tenantRecords = records.map { it.copy(institutionId = resolvedTenant, isSyncedToCentral = false) }

        // 1. Outbox entry for the batch itself
        val batchPayload = JSONObject().apply {
            put("batchId", tenantBatch.batchId)
            put("classSection", tenantBatch.classSection)
            put("session", tenantBatch.session)
            put("date", tenantBatch.date)
            put("teacherName", tenantBatch.teacherName)
            put("totalStudents", tenantBatch.totalStudents)
            put("presentCount", tenantBatch.presentCount)
            put("absentCount", tenantBatch.absentCount)
            put("absentRolls", tenantBatch.absentRolls)
            put("institutionId", resolvedTenant)
            put("timestamp", tenantBatch.timestamp)
        }.toString()

        val batchOutboxItem = outboxManager.createOutboxEntity(
            entityType = "REGISTER_SCAN_BATCH",
            entityId = tenantBatch.batchId,
            operation = "CREATE",
            payloadJson = batchPayload,
            isHighIntegrity = true,
            policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
            institutionId = resolvedTenant,
            customIdempotencyKey = "BATCH-${resolvedTenant}-${tenantBatch.batchId}"
        )

        // 2. Outbox entries for each individual scanned record
        val recordOutboxItems = tenantRecords.map { rec ->
            val payload = JSONObject().apply {
                put("batchId", tenantBatch.batchId)
                put("classSection", tenantBatch.classSection)
                put("date", tenantBatch.date)
                put("personId", rec.personId)
                put("personName", rec.personName)
                put("status", rec.status)
                put("timeMarked", rec.timeMarked)
                put("institutionId", resolvedTenant)
            }.toString()

            val entityId = "${tenantBatch.batchId}_${rec.personId}"
            val idempotencyKey = "SCAN-${resolvedTenant}-${entityId}"

            outboxManager.createOutboxEntity(
                entityType = "ATTENDANCE_RECORD",
                entityId = entityId,
                operation = "CREATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = idempotencyKey
            )
        }

        val allOutboxItems = listOf(batchOutboxItem) + recordOutboxItems
        dao.commitRegisterScanWithOutbox(tenantBatch, tenantRecords, allOutboxItems)
    }

    override suspend fun verifyBatchByIncharge(batchId: String) = withContext(Dispatchers.IO) {
        val resolvedTenant = resolveAuthenticatedTenant()
        val payload = JSONObject().apply {
            put("batchId", batchId)
            put("action", "VERIFIED_BY_INCHARGE")
            put("institutionId", resolvedTenant)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val outboxItem = outboxManager.createOutboxEntity(
            entityType = "ATTENDANCE_BATCH_VERIFICATION",
            entityId = batchId,
            operation = "UPDATE",
            payloadJson = payload,
            isHighIntegrity = true,
            policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
            institutionId = resolvedTenant,
            customIdempotencyKey = "VERIFY-BATCH-${resolvedTenant}-${batchId}"
        )

        dao.inchargeVerifyBatchWithOutbox(batchId, null, outboxItem)
    }

    override suspend fun approveBatchByPrincipal(batchId: String) = withContext(Dispatchers.IO) {
        val resolvedTenant = resolveAuthenticatedTenant()
        val payload = JSONObject().apply {
            put("batchId", batchId)
            put("action", "APPROVED_BY_PRINCIPAL")
            put("institutionId", resolvedTenant)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        val outboxItem = outboxManager.createOutboxEntity(
            entityType = "ATTENDANCE_BATCH_APPROVAL",
            entityId = batchId,
            operation = "UPDATE",
            payloadJson = payload,
            isHighIntegrity = true,
            policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
            institutionId = resolvedTenant,
            customIdempotencyKey = "APPROVE-BATCH-${resolvedTenant}-${batchId}"
        )

        dao.principalApproveBatchWithOutbox(batchId, null, outboxItem)
    }

    override suspend fun inchargeVerifyBatchWithAudit(batchId: String, auditLog: AuditLogEntity) =
        withContext(Dispatchers.IO) {
            val resolvedTenant = resolveAuthenticatedTenant(auditLog.institutionId)
            val tenantAuditLog = auditLog.copy(institutionId = resolvedTenant)

            val payload = JSONObject().apply {
                put("batchId", batchId)
                put("action", "VERIFIED_BY_INCHARGE")
                put("actorUserId", tenantAuditLog.actorUserId)
                put("actorName", tenantAuditLog.actorName)
                put("institutionId", resolvedTenant)
                put("timestamp", tenantAuditLog.timestamp)
            }.toString()

            val outboxItem = outboxManager.createOutboxEntity(
                entityType = "ATTENDANCE_BATCH_VERIFICATION",
                entityId = batchId,
                operation = "UPDATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = "VERIFY-BATCH-${resolvedTenant}-${batchId}"
            )

            dao.inchargeVerifyBatchWithOutbox(batchId, tenantAuditLog, outboxItem)
        }

    override suspend fun principalApproveBatchWithAudit(batchId: String, auditLog: AuditLogEntity) =
        withContext(Dispatchers.IO) {
            val resolvedTenant = resolveAuthenticatedTenant(auditLog.institutionId)
            val tenantAuditLog = auditLog.copy(institutionId = resolvedTenant)

            val payload = JSONObject().apply {
                put("batchId", batchId)
                put("action", "APPROVED_BY_PRINCIPAL")
                put("actorUserId", tenantAuditLog.actorUserId)
                put("actorName", tenantAuditLog.actorName)
                put("institutionId", resolvedTenant)
                put("timestamp", tenantAuditLog.timestamp)
            }.toString()

            val outboxItem = outboxManager.createOutboxEntity(
                entityType = "ATTENDANCE_BATCH_APPROVAL",
                entityId = batchId,
                operation = "UPDATE",
                payloadJson = payload,
                isHighIntegrity = true,
                policy = ConflictResolutionPolicy.HIGH_INTEGRITY_MANUAL,
                institutionId = resolvedTenant,
                customIdempotencyKey = "APPROVE-BATCH-${resolvedTenant}-${batchId}"
            )

            dao.principalApproveBatchWithOutbox(batchId, tenantAuditLog, outboxItem)
        }

    override suspend fun applyBatchAttendanceAndSyncRoster(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>,
        percentageUpdates: Map<String, Float>
    ) = withContext(Dispatchers.IO) {
        commitRegisterScan(batch, records)
        percentageUpdates.forEach { (studentId, pct) ->
            dao.updateStudentAttendancePercentage(studentId, pct)
        }
    }
}
