package com.group8.comp2300.presentation.screens.medical.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationOccurrenceCandidate
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.CalendarDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.MoodDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val appointments: List<Appointment> = emptyList(),
    val overview: List<CalendarOverviewResponse> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val selectedDate: String = "",
    val routineAgenda: List<RoutineDayAgenda> = emptyList(),
    val manualLogs: List<MedicationLog> = emptyList(),
)

class CalendarViewModel(
    private val syncCoordinator: SyncCoordinator,
    private val calendarRepository: CalendarDataRepository,
    private val appointmentRepository: AppointmentDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val medicationLogRepository: MedicationLogDataRepository,
    private val moodRepository: MoodDataRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData(dateStringOverride: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                syncCoordinator.refreshAuthenticatedData()
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val year = now.year
                val month = now.month.number
                val todayDateString = "${now.year}-${now.month.number.toString().padStart(
                    2,
                    '0',
                )}-${now.day.toString().padStart(2, '0')}"
                val dateString = dateStringOverride ?: _state.value.selectedDate.ifBlank { todayDateString }

                _state.update {
                    it.copy(
                        isLoading = false,
                        overview = calendarRepository.getCalendarOverview(year, month),
                        appointments = appointmentRepository.getAppointments(),
                        medications = medicationRepository.getMedications(),
                        selectedDate = dateString,
                        routineAgenda = medicationLogRepository.getRoutineAgenda(dateString),
                        manualLogs = medicationLogRepository.getManualMedicationLogs(dateString),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load calendar data") }
            }
        }
    }

    fun loadAgendaForDate(dateString: String) {
        viewModelScope.launch {
            try {
                _state.update {
                    it.copy(
                        selectedDate = dateString,
                        routineAgenda = medicationLogRepository.getRoutineAgenda(dateString),
                        manualLogs = medicationLogRepository.getManualMedicationLogs(dateString),
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadOverviewForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            runCatching { calendarRepository.getCalendarOverview(year, month) }
                .onSuccess { _state.update { state -> state.copy(overview = it) } }
                .onFailure { error -> _state.update { state -> state.copy(error = error.message) } }
        }
    }

    fun createMedication(request: MedicationCreateRequest) {
        viewModelScope.launch {
            runCatching { medicationRepository.saveMedication(request) }
                .onSuccess { loadInitialData(_state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to save medication")) }
                }
        }
    }

    fun logMedication(request: MedicationLogRequest) {
        viewModelScope.launch {
            runCatching { medicationLogRepository.logMedication(request) }
                .onSuccess { loadInitialData(_state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to log medication")) }
                }
        }
    }

    fun getMedicationOccurrenceCandidates(
        medicationId: String,
        timestampMs: Long,
        onLoaded: (List<MedicationOccurrenceCandidate>) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                medicationLogRepository.getMedicationOccurrenceCandidates(
                    medicationId = medicationId,
                    timestampMs = timestampMs,
                )
            }.onSuccess(onLoaded)
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to load matching doses")) }
                }
        }
    }

    fun rescheduleRoutineOccurrence(request: RoutineOccurrenceOverrideRequest) {
        viewModelScope.launch {
            runCatching { medicationLogRepository.rescheduleRoutineOccurrence(request) }
                .onSuccess { loadInitialData(_state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to move scheduled dose")) }
                }
        }
    }

    fun scheduleAppointment(doctorName: String, appointmentType: String, appointmentTimeMs: Long) {
        viewModelScope.launch {
            runCatching {
                val request = AppointmentRequest(
                    title = "Appointment with $doctorName",
                    appointmentTime = appointmentTimeMs,
                    appointmentType = mapAppointmentType(appointmentType),
                    doctorName = doctorName,
                )
                appointmentRepository.scheduleAppointment(request)
            }.onSuccess { loadInitialData() }
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to save appointment")) }
                }
        }
    }

    fun logMood(score: Int, tags: List<String>, symptoms: List<String>, notes: String) {
        viewModelScope.launch {
            runCatching {
                moodRepository.logMood(
                    MoodEntryRequest(
                        moodScore = score,
                        tags = tags,
                        symptoms = symptoms,
                        notes = notes,
                    ),
                )
            }.onSuccess { loadInitialData() }
                .onFailure { error ->
                    _state.update { state -> state.copy(error = error.errorMessage("Failed to log mood")) }
                }
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    companion object {
        fun mapAppointmentType(displayType: String): String = when (displayType.lowercase()) {
            "consultation" -> "CONSULTATION"
            "lab work", "labwork" -> "LAB_TEST"
            "follow-up", "follow up", "followup" -> "FOLLOW_UP"
            "checkup", "check-up" -> "CHECKUP"
            "screening" -> "SCREENING"
            "vaccination" -> "VACCINATION"
            "emergency" -> "EMERGENCY"
            else -> "OTHER"
        }
    }
}

private fun Throwable.errorMessage(default: String): String = message ?: default
