package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SchoolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AttendanceStatus(val label: String, val shortCode: String) {
    PRESENT("Present", "P"),
    ABSENT("Absent", "A"),
    LATE("Late", "L"),
    LEAVE("On Leave", "LV")
}

data class StudentAttendanceItem(
    val studentId: String,
    val studentName: String,
    val rollNumber: Int,
    val admissionNumber: String,
    val status: String = "PRESENT",
    val remarks: String = "",
    val historicalPercentage: Float = 95.0f,
    val guardianPhone: String = "",
    val studentPhone: String = guardianPhone
)

data class AttendanceSummary(
    val totalCount: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val leaveCount: Int = 0,
    val attendancePercentage: Float = 0f
)

data class TeacherAttendanceUiState(
    val selectedClass: String = "10-A",
    val availableClasses: List<String> = listOf("10-A", "10-B", "9-A", "9-B", "8-A", "11-A", "12-A"),
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
    val displayDate: String = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date()),
    val selectedSession: String = "Morning Assembly",
    val availableSessions: List<String> = listOf(
        "Morning Assembly",
        "Period 1 - Physics",
        "Period 2 - Mathematics",
        "Period 3 - Science Practical",
        "Afternoon Session",
        "Full Day"
    ),
    val teacherName: String = "Dr. Meenakshi Sundaram",
    val teacherId: String = "FAC-TCH-01",
    val institutionId: String = "INST-SXA-2026",
    val pendingOutboxCount: Int = 0,
    val isOnline: Boolean = true,
    val isSyncingOutbox: Boolean = false,
    val syncStatusMessage: String = "All pending changes are fully synchronized.",
    val isReviewMode: Boolean = false,
    val roster: List<StudentAttendanceItem> = emptyList(),
    val summary: AttendanceSummary = AttendanceSummary(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAlreadyRecorded: Boolean = false,
    val isAuthorizedToMark: Boolean = true,
    val authorizationError: String? = null,
    val validationError: String? = null,
    val duplicateWarning: String? = null,
    val isEmptyRoster: Boolean = false,
    val lastSavedTimestamp: Long? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null,
    val showWhatsAppPushSheet: Boolean = false,
    val selectedStudentForWhatsApp: StudentAttendanceItem? = null,
    val showPostSaveWhatsAppPrompt: Boolean = false,
    val lastWhatsAppPushCount: Int? = null
)

@HiltViewModel
class TeacherAttendanceViewModel @Inject constructor(
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository,
    private val outboxManager: com.example.data.remote.sync.DurableOutboxManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAttendanceUiState())
    val uiState: StateFlow<TeacherAttendanceUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.US)

    private val effectiveOutboxManager = outboxManager ?: schoolRepository.outboxManager
    private var activeActorRole: String = "TEACHER"

    init {
        // Resolve active tenant and authenticated teacher from Security Claims
        val activeClaims = com.example.security.TenantSecurityContext.getCurrentClaims()
            ?: com.example.security.SecureSessionManager.getActiveSession()?.claims
        val activeTenant = com.example.security.TenantSecurityContext.getActiveTenantIdOrNull()
            ?: com.example.security.SecureSessionManager.getActiveSession()?.institutionId
            ?: "INST-SXA-2026"

        val tName = activeClaims?.displayName ?: "Faculty Member"
        val tId = activeClaims?.userId ?: "FAC-TCH-01"
        val role = activeClaims?.role
        activeActorRole = role?.name ?: "TEACHER"

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

        val canMark = isSystemAdmin || isTeacherOrIncharge || (activeClaims?.hasPermission("MARK_ATTENDANCE_SECTION") == true)

        _uiState.update {
            it.copy(
                institutionId = activeTenant,
                teacherName = tName,
                teacherId = tId,
                isAuthorizedToMark = canMark,
                authorizationError = if (!canMark) "Unauthorized: Role '${role?.name ?: "USER"}' does not have permission to mark attendance." else null
            )
        }

        // Dynamically load assigned classes strictly within active tenant
        viewModelScope.launch {
            val allClasses = schoolRepository.getAllAcademicClassesList()
                .filter { it.institutionId == activeTenant }
            val available = if (isSystemAdmin) {
                val mapped = allClasses.map { "${it.className}-${it.section}".replace("Grade ", "").replace("Class ", "") }.distinct()
                mapped.ifEmpty { listOf("10-A", "10-B", "9-A", "9-B", "8-A") }
            } else if (isTeacherOrIncharge) {
                val assignedSection = activeClaims?.assignedClassSection
                val list = mutableListOf<String>()
                if (!assignedSection.isNullOrBlank()) {
                    list.add(assignedSection)
                }
                allClasses.forEach { c ->
                    val cleanSec = "${c.className}-${c.section}".replace("Grade ", "").replace("Class ", "")
                    if (c.classTeacherId == tId && !list.contains(cleanSec)) {
                        list.add(cleanSec)
                    }
                }
                if (list.isEmpty() && !assignedSection.isNullOrBlank()) {
                    list.add(assignedSection)
                }
                list.ifEmpty { listOf("10-A") }
            } else {
                emptyList()
            }

            val defaultClass = available.firstOrNull() ?: "10-A"
            _uiState.update {
                it.copy(
                    availableClasses = available,
                    selectedClass = defaultClass,
                    isAuthorizedToMark = canMark && (isSystemAdmin || available.isNotEmpty()),
                    authorizationError = if (!canMark) {
                        "Unauthorized: Role '${role?.name ?: "USER"}' does not have permission to mark daily attendance."
                    } else if (!isSystemAdmin && available.isEmpty()) {
                        "No classes currently assigned to faculty member '$tName'."
                    } else null
                )
            }
            refreshRosterAndExistingRecords()
        }

        // Observe students from repository
        viewModelScope.launch {
            schoolRepository.allStudents.collect { studentsList ->
                loadRosterForCurrentSelection(studentsList)
            }
        }

        // Observe pending outbox count for attendance/system mutations
        viewModelScope.launch {
            effectiveOutboxManager.getPendingOutboxCount(activeTenant).collect { count ->
                _uiState.update { it.copy(pendingOutboxCount = count) }
            }
        }

        // Observe outbox syncing state
        viewModelScope.launch {
            effectiveOutboxManager.isSyncing.collect { syncing ->
                _uiState.update { it.copy(isSyncingOutbox = syncing) }
            }
        }

        // Observe sync status messages
        viewModelScope.launch {
            effectiveOutboxManager.syncStatusMessage.collect { statusMsg ->
                _uiState.update { it.copy(syncStatusMessage = statusMsg) }
            }
        }
    }

    fun setReviewMode(enabled: Boolean) {
        _uiState.update { it.copy(isReviewMode = enabled) }
    }

    fun triggerCloudSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        val activeTenant = _uiState.value.institutionId
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncingOutbox = true) }
            val result = effectiveOutboxManager.flushQueue(activeTenant, forceAll = true)
            _uiState.update { it.copy(isSyncingOutbox = false) }
            result.onSuccess { batch ->
                val msg = "Sync completed: ${batch.acknowledgedCount} acknowledged by server, ${batch.failedCount} pending/retrying."
                _uiState.update { it.copy(feedbackMessage = msg) }
                onComplete?.invoke(true, msg)
            }.onFailure { err ->
                val errMsg = "Sync deferred: ${err.localizedMessage ?: "Network offline"}"
                _uiState.update { it.copy(errorMessage = errMsg) }
                onComplete?.invoke(false, errMsg)
            }
        }
    }

    fun selectClass(classSection: String) {
        if (_uiState.value.selectedClass == classSection) return
        _uiState.update { it.copy(selectedClass = classSection, isLoading = true) }
        refreshRosterAndExistingRecords()
    }

    fun selectDate(dateString: String) {
        try {
            val parsed = dateFormat.parse(dateString) ?: Date()
            val formattedDisplay = displayFormat.format(parsed)
            _uiState.update {
                it.copy(
                    selectedDate = dateString,
                    displayDate = formattedDisplay,
                    isLoading = true
                )
            }
            refreshRosterAndExistingRecords()
        } catch (e: Exception) {
            _uiState.update { it.copy(selectedDate = dateString, displayDate = dateString) }
        }
    }

    fun nextDay() {
        shiftDate(1)
    }

    fun previousDay() {
        shiftDate(-1)
    }

    fun setToday() {
        val today = Date()
        _uiState.update {
            it.copy(
                selectedDate = dateFormat.format(today),
                displayDate = displayFormat.format(today),
                isLoading = true
            )
        }
        refreshRosterAndExistingRecords()
    }

    private fun shiftDate(days: Int) {
        try {
            val cal = Calendar.getInstance()
            val current = dateFormat.parse(_uiState.value.selectedDate) ?: Date()
            cal.time = current
            cal.add(Calendar.DAY_OF_YEAR, days)
            val newDate = cal.time
            _uiState.update {
                it.copy(
                    selectedDate = dateFormat.format(newDate),
                    displayDate = displayFormat.format(newDate),
                    isLoading = true
                )
            }
            refreshRosterAndExistingRecords()
        } catch (e: Exception) {
            // Ignore shift error
        }
    }

    fun selectSession(session: String) {
        _uiState.update { it.copy(selectedSession = session, isLoading = true) }
        refreshRosterAndExistingRecords()
    }

    fun setStudentStatus(studentId: String, status: String) {
        if (!_uiState.value.isAuthorizedToMark) return
        _uiState.update { state ->
            val updatedRoster = state.roster.map { item ->
                if (item.studentId == studentId) {
                    item.copy(status = status)
                } else item
            }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary, validationError = null)
        }
    }

    fun setStudentRemarks(studentId: String, remarks: String) {
        if (!_uiState.value.isAuthorizedToMark) return
        _uiState.update { state ->
            val updatedRoster = state.roster.map { item ->
                if (item.studentId == studentId) {
                    item.copy(remarks = remarks)
                } else item
            }
            state.copy(roster = updatedRoster)
        }
    }

    fun markAllPresent() {
        if (!_uiState.value.isAuthorizedToMark) return
        _uiState.update { state ->
            val updatedRoster = state.roster.map { it.copy(status = "PRESENT") }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary, validationError = null)
        }
    }

    fun markAllAbsent() {
        if (!_uiState.value.isAuthorizedToMark) return
        _uiState.update { state ->
            val updatedRoster = state.roster.map { it.copy(status = "ABSENT") }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary, validationError = null)
        }
    }

    fun toggleSingleStudent(studentId: String) {
        if (!_uiState.value.isAuthorizedToMark) return
        _uiState.update { state ->
            val updatedRoster = state.roster.map { item ->
                if (item.studentId == studentId) {
                    val nextStatus = when (item.status) {
                        "PRESENT" -> "ABSENT"
                        "ABSENT" -> "LATE"
                        "LATE" -> "LEAVE"
                        else -> "PRESENT"
                    }
                    item.copy(status = nextStatus)
                } else item
            }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary, validationError = null)
        }
    }

    fun saveDailyAttendance(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val currentState = _uiState.value

        // 1. RBAC Permission & Authorization Check
        if (!currentState.isAuthorizedToMark) {
            val errorMsg = currentState.authorizationError
                ?: "Access Denied: You do not have permission to mark attendance for class ${currentState.selectedClass}."
            _uiState.update { it.copy(errorMessage = errorMsg) }
            onComplete(false, errorMsg)
            return
        }

        // 2. Class Membership & Roster Validation
        if (currentState.roster.isEmpty()) {
            val msg = "No students enrolled in class ${currentState.selectedClass} to record attendance."
            _uiState.update { it.copy(errorMessage = msg, validationError = msg) }
            onComplete(false, msg)
            return
        }

        // 3. Status Validation (Each student must have PRESENT, ABSENT, or LATE)
        val validStatuses = setOf("PRESENT", "ABSENT", "LATE", "LEAVE")
        val invalidEntries = currentState.roster.filter { it.status.uppercase() !in validStatuses }
        if (invalidEntries.isNotEmpty()) {
            val msg = "Invalid status found for student ${invalidEntries.first().studentName}. Must be Present, Absent, or Late."
            _uiState.update { it.copy(errorMessage = msg, validationError = msg) }
            onComplete(false, msg)
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null, validationError = null) }

        viewModelScope.launch {
            try {
                // 4. Duplicate Check: check if attendance for this class and date exists
                val existing = attendanceRepository.getAttendanceForClassDateAndSession(
                    currentState.selectedClass,
                    currentState.selectedDate,
                    currentState.selectedSession
                ).ifEmpty {
                    attendanceRepository.getAttendanceForClassAndDate(
                        currentState.selectedClass,
                        currentState.selectedDate
                    )
                }

                val timestamp = System.currentTimeMillis()
                val attendanceEntities = currentState.roster.map { student ->
                    AttendanceEntity(
                        institutionId = currentState.institutionId,
                        studentId = student.studentId,
                        studentName = student.studentName,
                        classSection = currentState.selectedClass,
                        date = currentState.selectedDate,
                        dateTimestamp = timestamp,
                        status = student.status.uppercase(),
                        session = currentState.selectedSession,
                        markedByTeacherId = currentState.teacherId,
                        remarks = student.remarks,
                        isVerifiedByIncharge = true,
                        isApprovedByPrincipal = true
                    )
                }

                // Save directly and atomically to Room Database (with durable outbox event)
                attendanceRepository.saveDailyAttendanceBatch(
                    classSection = currentState.selectedClass,
                    date = currentState.selectedDate,
                    attendances = attendanceEntities,
                    updateStudentPercentages = true
                )

                // Audit log entry in Room
                schoolRepository.insertAuditLog(
                    AuditLogEntity(
                        institutionId = currentState.institutionId,
                        timestamp = timestamp,
                        formattedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(timestamp)),
                        action = "RECORD_DAILY_ATTENDANCE",
                        entityType = "ATTENDANCE_REGISTER",
                        entityId = "${currentState.selectedClass}_${currentState.selectedDate}",
                        actorUserId = currentState.teacherId,
                        actorName = currentState.teacherName,
                        actorRole = activeActorRole,
                        previousState = if (existing.isNotEmpty()) "PREVIOUSLY_RECORDED" else "PENDING",
                        newState = "COMMITTED_ROOM",
                        remarks = "Attendance marked for ${currentState.selectedClass} on ${currentState.selectedDate}: ${currentState.summary.presentCount} P, ${currentState.summary.absentCount} A, ${currentState.summary.lateCount} L (${currentState.summary.attendancePercentage}%)"
                    )
                )

                val successMsg = "Attendance for ${currentState.selectedClass} on ${currentState.displayDate} successfully saved to local Room database."
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isAlreadyRecorded = true,
                        isReviewMode = false,
                        lastSavedTimestamp = timestamp,
                        feedbackMessage = successMsg,
                        showPostSaveWhatsAppPrompt = true
                    )
                }
                onComplete(true, successMsg)
            } catch (e: Exception) {
                val errorMsg = "Failed to save attendance: ${e.localizedMessage ?: "Database error"}"
                _uiState.update { it.copy(isSaving = false, errorMessage = errorMsg) }
                onComplete(false, errorMsg)
            }
        }
    }

    fun openWhatsAppPushSheet(selectedStudent: StudentAttendanceItem? = null) {
        _uiState.update {
            it.copy(
                showWhatsAppPushSheet = true,
                selectedStudentForWhatsApp = selectedStudent,
                showPostSaveWhatsAppPrompt = false
            )
        }
    }

    fun closeWhatsAppPushSheet() {
        _uiState.update {
            it.copy(
                showWhatsAppPushSheet = false,
                selectedStudentForWhatsApp = null
            )
        }
    }

    fun dismissPostSaveWhatsAppPrompt() {
        _uiState.update { it.copy(showPostSaveWhatsAppPrompt = false) }
    }

    fun recordWhatsAppBatchAudit(dispatchedCount: Int) {
        val currentState = _uiState.value
        val timestamp = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                schoolRepository.insertAuditLog(
                    AuditLogEntity(
                        institutionId = currentState.institutionId,
                        timestamp = timestamp,
                        formattedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date(timestamp)),
                        action = "WHATSAPP_ATTENDANCE_PUSH_DISPATCH",
                        entityType = "WHATSAPP_PUSH_DISPATCHER",
                        entityId = "${currentState.selectedClass}_${currentState.selectedDate}",
                        actorUserId = currentState.teacherId,
                        actorName = currentState.teacherName,
                        actorRole = activeActorRole,
                        previousState = "COMPOSED",
                        newState = "DISPATCHED_TO_LINKED_NUMBERS",
                        remarks = "Computer-generated WhatsApp attendance notices pushed to $dispatchedCount students on linked mobile numbers for ${currentState.selectedClass} on ${currentState.selectedDate}."
                    )
                )
                _uiState.update {
                    it.copy(
                        lastWhatsAppPushCount = dispatchedCount,
                        feedbackMessage = "WhatsApp attendance notice successfully pushed to $dispatchedCount linked mobile numbers."
                    )
                }
            } catch (e: Exception) {
                // Log failed audit non-critically
            }
        }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null, errorMessage = null) }
    }

    private fun refreshRosterAndExistingRecords() {
        viewModelScope.launch {
            val allStudents = schoolRepository.getAllStudentsList()
            loadRosterForCurrentSelection(allStudents)
        }
    }

    private suspend fun loadRosterForCurrentSelection(allStudents: List<StudentEntity>) {
        val currentClass = _uiState.value.selectedClass
        val currentDate = _uiState.value.selectedDate
        val currentSession = _uiState.value.selectedSession

        // Filter students matching the grade and section
        // Format of selectedClass: e.g. "10-A"
        val parts = currentClass.split("-")
        val gradePart = parts.getOrNull(0) ?: "10"
        val sectionPart = parts.getOrNull(1) ?: "A"

        val activeTenant = _uiState.value.institutionId
        val matchingStudents = allStudents.filter { s ->
            val matchTenant = s.institutionId == activeTenant
            val matchGrade = s.grade.equals(gradePart, ignoreCase = true) || s.grade.contains(gradePart, ignoreCase = true)
            val matchSection = s.section.equals(sectionPart, ignoreCase = true)
            matchTenant && matchGrade && matchSection
        }.sortedBy { it.rollNumber }

        val isEmpty = matchingStudents.isEmpty()

        // Check if records exist in Room database for this date, class, and session
        val existingRecords = attendanceRepository.getAttendanceForClassDateAndSession(currentClass, currentDate, currentSession)
            .ifEmpty { attendanceRepository.getAttendanceForClassAndDate(currentClass, currentDate) }
        val isAlreadyRecorded = existingRecords.isNotEmpty()
        val recordMap = existingRecords.associateBy { it.studentId }

        val rosterItems = matchingStudents.map { student ->
            val recorded = recordMap[student.id]
            StudentAttendanceItem(
                studentId = student.id,
                studentName = student.name,
                rollNumber = student.rollNumber,
                admissionNumber = student.admissionNumber,
                status = recorded?.status ?: "PRESENT",
                remarks = recorded?.remarks ?: "",
                historicalPercentage = student.attendancePercentage,
                guardianPhone = student.guardianPhone,
                studentPhone = student.studentPhone.ifBlank { student.guardianPhone }
            )
        }

        val summary = calculateSummary(rosterItems)
        val lastSaved = existingRecords.maxOfOrNull { it.dateTimestamp }
        val dupWarning = if (isAlreadyRecorded && lastSaved != null) {
            "Attendance for $currentClass on $currentDate ($currentSession) was previously recorded on ${displayFormat.format(Date(lastSaved))}."
        } else null

        _uiState.update {
            it.copy(
                roster = rosterItems,
                summary = summary,
                isAlreadyRecorded = isAlreadyRecorded,
                duplicateWarning = dupWarning,
                isEmptyRoster = isEmpty,
                lastSavedTimestamp = lastSaved,
                isLoading = false
            )
        }
    }

    private fun calculateSummary(roster: List<StudentAttendanceItem>): AttendanceSummary {
        val total = roster.size
        if (total == 0) return AttendanceSummary()
        var present = 0
        var absent = 0
        var late = 0
        var leave = 0

        roster.forEach { item ->
            when (item.status.uppercase()) {
                "PRESENT", "P" -> present++
                "ABSENT", "A" -> absent++
                "LATE", "L" -> late++
                "LEAVE", "LV" -> leave++
                else -> present++
            }
        }

        val pct = if (total > 0) ((present + late * 0.5f) / total * 100f) else 0f
        val roundedPct = Math.round(pct * 10f) / 10f

        return AttendanceSummary(
            totalCount = total,
            presentCount = present,
            absentCount = absent,
            lateCount = late,
            leaveCount = leave,
            attendancePercentage = roundedPct.toFloat()
        )
    }
}

class TeacherAttendanceViewModelFactory(
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TeacherAttendanceViewModel::class.java)) {
            return TeacherAttendanceViewModel(attendanceRepository, schoolRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
