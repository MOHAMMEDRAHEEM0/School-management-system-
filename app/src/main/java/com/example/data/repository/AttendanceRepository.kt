package com.example.data.repository

import com.example.data.local.dao.SchoolDao
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AttendanceRecordEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.RegisterScanBatchEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
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
    private val dao: SchoolDao
) : AttendanceRepository {
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

    override suspend fun getAttendanceForClassAndDate(
        classSection: String,
        date: String
    ): List<AttendanceEntity> = withContext(Dispatchers.IO) {
        dao.getAttendanceForClassAndDate(classSection.trim(), date.trim())
    }

    override suspend fun saveDailyAttendanceBatch(
        classSection: String,
        date: String,
        attendances: List<AttendanceEntity>,
        updateStudentPercentages: Boolean
    ) = withContext(Dispatchers.IO) {
        // Delete previous records for the exact classSection and date to avoid duplicates
        dao.deleteAttendanceForClassAndDate(classSection.trim(), date.trim())
        // Insert new list
        dao.insertAttendanceList(attendances)

        // Also create mirrored AttendanceRecordEntity records for institutional sync & audit
        val recordEntities = attendances.map { att ->
            AttendanceRecordEntity(
                institutionId = att.institutionId,
                personId = att.studentId,
                personName = att.studentName,
                roleType = "STUDENT",
                date = att.date,
                status = att.status,
                timeMarked = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(att.dateTimestamp)),
                remarks = att.remarks,
                markedByRole = "TEACHER",
                markedByName = "Faculty In-Charge",
                classSection = att.classSection,
                sessionType = att.session,
                sourceMethod = "DAILY_ATTENDANCE_UI",
                isSyncedToCentral = true
            )
        }
        dao.insertAttendanceRecords(recordEntities)

        if (updateStudentPercentages) {
            // Update percentage for each student in students table if present
            attendances.forEach { att ->
                val student = dao.getStudentById(att.studentId)
                if (student != null) {
                    val isPresentOrLate = att.status.equals("PRESENT", ignoreCase = true) || att.status.equals("LATE", ignoreCase = true)
                    val currentPct = student.attendancePercentage
                    // Slight realistic adjustment towards 100 or 0
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
        dao.insertAttendance(attendance)
    }

    override suspend fun recordAttendanceBatch(attendances: List<AttendanceEntity>) = withContext(Dispatchers.IO) {
        dao.insertAttendanceList(attendances)
    }

    override suspend fun insertAttendanceRecord(record: AttendanceRecordEntity) = withContext(Dispatchers.IO) {
        dao.insertAttendanceRecord(record)
    }

    override suspend fun insertAttendanceRecords(records: List<AttendanceRecordEntity>) = withContext(Dispatchers.IO) {
        dao.insertAttendanceRecords(records)
    }

    override suspend fun commitRegisterScan(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>
    ) = withContext(Dispatchers.IO) {
        dao.insertRegisterBatch(batch)
        dao.insertAttendanceRecords(records)
    }

    override suspend fun verifyBatchByIncharge(batchId: String) = withContext(Dispatchers.IO) {
        dao.verifyBatchByIncharge(batchId)
    }

    override suspend fun approveBatchByPrincipal(batchId: String) = withContext(Dispatchers.IO) {
        dao.approveBatchByPrincipal(batchId)
    }

    override suspend fun inchargeVerifyBatchWithAudit(batchId: String, auditLog: AuditLogEntity) =
        withContext(Dispatchers.IO) {
            dao.inchargeVerifyBatchWithAudit(batchId, auditLog)
        }

    override suspend fun principalApproveBatchWithAudit(batchId: String, auditLog: AuditLogEntity) =
        withContext(Dispatchers.IO) {
            dao.principalApproveBatchWithAudit(batchId, auditLog)
        }

    override suspend fun applyBatchAttendanceAndSyncRoster(
        batch: RegisterScanBatchEntity,
        records: List<AttendanceRecordEntity>,
        percentageUpdates: Map<String, Float>
    ) = withContext(Dispatchers.IO) {
        dao.applyBatchAttendanceAndSyncRoster(batch, records, percentageUpdates)
    }
}
