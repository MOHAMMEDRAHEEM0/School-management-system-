package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AuditLogEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.SchoolRepository
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
    val guardianPhone: String = ""
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
    val selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    val displayDate: String = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
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
    val roster: List<StudentAttendanceItem> = emptyList(),
    val summary: AttendanceSummary = AttendanceSummary(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isAlreadyRecorded: Boolean = false,
    val lastSavedTimestamp: Long? = null,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class TeacherAttendanceViewModel(
    private val attendanceRepository: AttendanceRepository,
    private val schoolRepository: SchoolRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherAttendanceUiState())
    val uiState: StateFlow<TeacherAttendanceUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    init {
        // Observe students from repository
        viewModelScope.launch {
            schoolRepository.allStudents.collect { studentsList ->
                loadRosterForCurrentSelection(studentsList)
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
        _uiState.update { it.copy(selectedSession = session) }
    }

    fun setStudentStatus(studentId: String, status: String) {
        _uiState.update { state ->
            val updatedRoster = state.roster.map { item ->
                if (item.studentId == studentId) {
                    item.copy(status = status)
                } else item
            }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary)
        }
    }

    fun setStudentRemarks(studentId: String, remarks: String) {
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
        _uiState.update { state ->
            val updatedRoster = state.roster.map { it.copy(status = "PRESENT") }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary)
        }
    }

    fun markAllAbsent() {
        _uiState.update { state ->
            val updatedRoster = state.roster.map { it.copy(status = "ABSENT") }
            val summary = calculateSummary(updatedRoster)
            state.copy(roster = updatedRoster, summary = summary)
        }
    }

    fun toggleSingleStudent(studentId: String) {
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
            state.copy(roster = updatedRoster, summary = summary)
        }
    }

    fun saveDailyAttendance(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        val currentState = _uiState.value
        if (currentState.roster.isEmpty()) {
            val msg = "No students found in class ${currentState.selectedClass} to record."
            _uiState.update { it.copy(errorMessage = msg) }
            onComplete(false, msg)
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val attendanceEntities = currentState.roster.map { student ->
                    AttendanceEntity(
                        institutionId = currentState.institutionId,
                        studentId = student.studentId,
                        studentName = student.studentName,
                        classSection = currentState.selectedClass,
                        date = currentState.selectedDate,
                        dateTimestamp = timestamp,
                        status = student.status,
                        session = currentState.selectedSession,
                        markedByTeacherId = currentState.teacherId,
                        remarks = student.remarks,
                        isVerifiedByIncharge = true,
                        isApprovedByPrincipal = true
                    )
                }

                // Save directly to Room Database
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
                        formattedTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date(timestamp)),
                        action = "RECORD_DAILY_ATTENDANCE",
                        entityType = "ATTENDANCE_REGISTER",
                        entityId = "${currentState.selectedClass}_${currentState.selectedDate}",
                        actorUserId = currentState.teacherId,
                        actorName = currentState.teacherName,
                        actorRole = "TEACHER",
                        previousState = if (currentState.isAlreadyRecorded) "PREVIOUSLY_RECORDED" else "PENDING",
                        newState = "COMMITTED_ROOM",
                        remarks = "Attendance marked for ${currentState.selectedClass} on ${currentState.selectedDate}: ${currentState.summary.presentCount} P, ${currentState.summary.absentCount} A, ${currentState.summary.lateCount} L (${currentState.summary.attendancePercentage}%)"
                    )
                )

                val successMsg = "Attendance for ${currentState.selectedClass} on ${currentState.displayDate} successfully saved to local Room database."
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isAlreadyRecorded = true,
                        lastSavedTimestamp = timestamp,
                        feedbackMessage = successMsg
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

        // Filter students matching the grade and section
        // Format of selectedClass: e.g. "10-A"
        val parts = currentClass.split("-")
        val gradePart = parts.getOrNull(0) ?: "10"
        val sectionPart = parts.getOrNull(1) ?: "A"

        val matchingStudents = allStudents.filter { s ->
            val matchGrade = s.grade.equals(gradePart, ignoreCase = true) || s.grade.contains(gradePart, ignoreCase = true)
            val matchSection = s.section.equals(sectionPart, ignoreCase = true)
            matchGrade && matchSection
        }.ifEmpty {
            // Fallback if grade field has "Grade 10" or "10"
            allStudents.filter { s -> s.section.equals(sectionPart, ignoreCase = true) }
        }.sortedBy { it.rollNumber }

        // Check if records exist in Room database for this date and class
        val existingRecords = attendanceRepository.getAttendanceForClassAndDate(currentClass, currentDate)
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
                guardianPhone = student.guardianPhone
            )
        }

        val summary = calculateSummary(rosterItems)
        val lastSaved = existingRecords.maxOfOrNull { it.dateTimestamp }

        _uiState.update {
            it.copy(
                roster = rosterItems,
                summary = summary,
                isAlreadyRecorded = isAlreadyRecorded,
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
