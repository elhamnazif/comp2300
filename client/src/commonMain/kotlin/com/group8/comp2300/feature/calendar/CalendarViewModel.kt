package com.group8.comp2300.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val dayMoodEntries: List<Mood> = emptyList(),
    val monthMoodSummary: Map<MoodType, Int> = emptyMap(),
)

class CalendarViewModel(
    private val syncCoordinator: SyncCoordinator,
    private val calendarRepository: CalendarDataRepository,
    private val appointmentRepository: AppointmentDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val medicationLogRepository: MedicationLogDataRepository,
    private val moodRepository: MoodDataRepository,
) : ViewModel() {

    val state: StateFlow<CalendarUiState>
        field: MutableStateFlow<CalendarUiState> = MutableStateFlow(CalendarUiState())

    init {
        loadInitialData()
    }

    private fun loadInitialData(dateStringOverride: String? = null) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, error = null) }
            try {
                syncCoordinator.refreshAuthenticatedData()
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val year = now.year
                val month = now.month.number
                val todayDateString = "${now.year}-${now.month.number.toString().padStart(
                    2,
                    '0',
                )}-${now.day.toString().padStart(2, '0')}"
                val dateString = dateStringOverride ?: state.value.selectedDate.ifBlank { todayDateString }

                state.update {
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
                state.update { it.copy(isLoading = false, error = e.message ?: "Failed to load calendar data") }
            }
        }
    }

    fun loadAgendaForDate(dateString: String) {
        viewModelScope.launch {
            try {
                val moods = moodRepository.getMoodsForDate(dateString)
                state.update {
                    it.copy(
                        selectedDate = dateString,
                        routineAgenda = medicationLogRepository.getRoutineAgenda(dateString),
                        manualLogs = medicationLogRepository.getManualMedicationLogs(dateString),
                        dayMoodEntries = moods,
                    )
                }
            } catch (e: Exception) {
                state.update { it.copy(error = e.message) }
            }
        }
    }

    fun loadOverviewForMonth(year: Int, month: Int) {
        viewModelScope.launch {
            runCatching { calendarRepository.getCalendarOverview(year, month) }
                .onSuccess { overview ->
                    val moodCounts = moodRepository.getMoodsForMonth(year, month)
                        .groupingBy { it.moodType }
                        .eachCount()
                    state.update { state -> state.copy(overview = overview, monthMoodSummary = moodCounts) }
                }
                .onFailure { error -> state.update { state -> state.copy(error = error.message) } }
        }
    }

    fun createMedication(request: MedicationCreateRequest) {
        viewModelScope.launch {
            runCatching { medicationRepository.saveMedication(request) }
                .onSuccess { loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    state.update { state -> state.copy(error = error.errorMessage("Failed to save medication")) }
                }
        }
    }

    fun logMedication(request: MedicationLogRequest) {
        viewModelScope.launch {
            runCatching { medicationLogRepository.logMedication(request) }
                .onSuccess { loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    state.update { state -> state.copy(error = error.errorMessage("Failed to log medication")) }
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
                    state.update { state -> state.copy(error = error.errorMessage("Failed to load matching doses")) }
                }
        }
    }

    fun rescheduleRoutineOccurrence(request: RoutineOccurrenceOverrideRequest) {
        viewModelScope.launch {
            runCatching { medicationLogRepository.rescheduleRoutineOccurrence(request) }
                .onSuccess { loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank)) }
                .onFailure { error ->
                    state.update { state -> state.copy(error = error.errorMessage("Failed to move scheduled dose")) }
                }
        }
    }

    fun logMood(score: Int, tags: List<String>, symptoms: List<String>, notes: String, timestampMs: Long? = null) {
        viewModelScope.launch {
            runCatching {
                moodRepository.logMood(
                    MoodEntryRequest(
                        moodScore = score,
                        tags = tags,
                        symptoms = symptoms,
                        notes = notes,
                        timestampMs = timestampMs,
                    ),
                )
            }.onSuccess {
                val currentDate = state.value.selectedDate
                if (currentDate.isNotBlank()) {
                    loadAgendaForDate(currentDate)
                } else {
                    loadInitialData()
                }
            }.onFailure { error ->
                state.update { state -> state.copy(error = error.errorMessage("Failed to log mood")) }
            }
        }
    }

    fun dismissError() {
        state.update { it.copy(error = null) }
    }
}

private fun Throwable.errorMessage(default: String): String = message ?: default
