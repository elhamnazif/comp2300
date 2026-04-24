package com.group8.comp2300.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Clock

internal data class CalendarUiState(
    val isLoading: Boolean = false,
    val screenError: String? = null,
    val snackbarMessage: String? = null,
    val isSheetMutationInFlight: Boolean = false,
    val completedSheetMutationCount: Int = 0,
    val appointments: List<Appointment> = emptyList(),
    val overview: List<CalendarOverviewResponse> = emptyList(),
    val medications: List<Medication> = emptyList(),
    val selectedDate: String = "",
    val routineAgenda: List<RoutineDayAgenda> = emptyList(),
    val manualLogs: List<MedicationLog> = emptyList(),
    val agendaDays: List<CalendarAgendaDay> = emptyList(),
    val dayMoodEntries: List<Mood> = emptyList(),
    val monthMoodSummary: Map<MoodType, Int> = emptyMap(),
)

class CalendarViewModel(
    private val syncCoordinator: OfflineSyncCoordinator,
    private val calendarRepository: CalendarDataRepository,
    private val appointmentRepository: AppointmentDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val medicationLogRepository: MedicationLogDataRepository,
    private val moodRepository: MoodDataRepository,
) : ViewModel() {

    internal val state: StateFlow<CalendarUiState>
        field: MutableStateFlow<CalendarUiState> = MutableStateFlow(CalendarUiState())

    init {
        loadInitialData()
    }

    private fun loadInitialData(dateStringOverride: String? = null) {
        viewModelScope.launch {
            state.update { it.copy(isLoading = true, screenError = null) }
            try {
                syncCoordinator.refreshCaches()
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val todayDateString = "${now.year}-${now.month.number.toString().padStart(
                    2,
                    '0',
                )}-${now.day.toString().padStart(2, '0')}"
                val dateString = dateStringOverride ?: state.value.selectedDate.ifBlank { todayDateString }
                val selectedDate = LocalDate.parse(dateString)
                val agendaDays = loadAgendaDays(dateString)
                val dayMoodEntries = moodRepository.getMoodsForDate(dateString)
                val monthMoodSummary = moodRepository.getMoodsForMonth(selectedDate.year, selectedDate.month.number)
                    .groupingBy { it.moodType }
                    .eachCount()

                state.update {
                    it.copy(
                        isLoading = false,
                        overview = calendarRepository.getCalendarOverview(selectedDate.year, selectedDate.month.number),
                        appointments = appointmentRepository.getAppointments(),
                        medications = medicationRepository.getMedications(),
                        selectedDate = dateString,
                        routineAgenda = medicationLogRepository.getRoutineAgenda(dateString),
                        manualLogs = medicationLogRepository.getManualMedicationLogs(dateString),
                        agendaDays = agendaDays,
                        dayMoodEntries = dayMoodEntries,
                        monthMoodSummary = monthMoodSummary,
                    )
                }
            } catch (e: Exception) {
                state.update {
                    it.copy(
                        isLoading = false,
                        screenError = e.errorMessage("Failed to load calendar data"),
                    )
                }
            }
        }
    }

    fun loadAgendaForDate(dateString: String) {
        viewModelScope.launch {
            try {
                val selectedDate = LocalDate.parse(dateString)
                val moods = moodRepository.getMoodsForDate(dateString)
                val monthMoodSummary = moodRepository.getMoodsForMonth(selectedDate.year, selectedDate.month.number)
                    .groupingBy { it.moodType }
                    .eachCount()
                val agendaDays = loadAgendaDays(dateString)
                val appointments = appointmentRepository.getAppointments()
                state.update {
                    it.copy(
                        appointments = appointments,
                        selectedDate = dateString,
                        routineAgenda = medicationLogRepository.getRoutineAgenda(dateString),
                        manualLogs = medicationLogRepository.getManualMedicationLogs(dateString),
                        agendaDays = agendaDays,
                        dayMoodEntries = moods,
                        monthMoodSummary = monthMoodSummary,
                    )
                }
            } catch (e: Exception) {
                state.update { it.copy(snackbarMessage = e.errorMessage("Failed to load schedule")) }
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
                .onFailure { error ->
                    state.update { state ->
                        state.copy(snackbarMessage = error.errorMessage("Failed to load calendar month"))
                    }
                }
        }
    }

    fun logMedication(request: MedicationLogRequest, fromSheet: Boolean = false) {
        if (fromSheet && !beginSheetMutation()) return
        viewModelScope.launch {
            runCatching { medicationLogRepository.logMedication(request) }
                .onSuccess {
                    if (fromSheet) {
                        finishSheetMutationSuccess()
                    }
                    loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank))
                }
                .onFailure { error ->
                    val message = error.errorMessage("Failed to log medication")
                    if (fromSheet) {
                        finishSheetMutationFailure(message)
                    } else {
                        state.update { state -> state.copy(snackbarMessage = message) }
                    }
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
                    state.update { currentState ->
                        currentState.copy(snackbarMessage = error.errorMessage("Failed to load matching doses"))
                    }
                }
        }
    }

    fun rescheduleRoutineOccurrence(request: RoutineOccurrenceOverrideRequest, fromSheet: Boolean = false) {
        if (fromSheet && !beginSheetMutation()) return
        viewModelScope.launch {
            runCatching { medicationLogRepository.rescheduleRoutineOccurrence(request) }
                .onSuccess {
                    if (fromSheet) {
                        finishSheetMutationSuccess()
                    }
                    loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank))
                }
                .onFailure { error ->
                    val message = error.errorMessage("Failed to move scheduled dose")
                    if (fromSheet) {
                        finishSheetMutationFailure(message)
                    } else {
                        state.update { state -> state.copy(snackbarMessage = message) }
                    }
                }
        }
    }

    fun logMood(
        score: Int,
        tags: List<String>,
        symptoms: List<String>,
        notes: String,
        timestampMs: Long? = null,
        fromSheet: Boolean = false,
    ) {
        if (fromSheet && !beginSheetMutation()) return
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
                if (fromSheet) {
                    finishSheetMutationSuccess()
                }
                val currentDate = state.value.selectedDate
                if (currentDate.isNotBlank()) {
                    loadAgendaForDate(currentDate)
                } else {
                    loadInitialData()
                }
            }.onFailure { error ->
                val message = error.errorMessage("Failed to log mood")
                if (fromSheet) {
                    finishSheetMutationFailure(message)
                } else {
                    state.update { state -> state.copy(snackbarMessage = message) }
                }
            }
        }
    }

    fun retryCurrentSelection() {
        loadInitialData(state.value.selectedDate.takeIf(String::isNotBlank))
    }

    fun dismissSnackbarMessage() {
        state.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun loadAgendaDays(startDateString: String): List<CalendarAgendaDay> {
        val startDate = LocalDate.parse(startDateString)
        val endDate = startDate.plus(6, DateTimeUnit.DAY)
        val routinesByDate = medicationLogRepository.getRoutineAgendaRange(startDate.toString(), endDate.toString())
        val manualLogsByDate = medicationLogRepository.getManualMedicationLogsRange(
            startDate.toString(),
            endDate.toString(),
        )

        return buildList {
            var current = startDate
            while (current <= endDate) {
                val dateKey = current.toString()
                add(
                    CalendarAgendaDay(
                        date = current,
                        routineAgenda = routinesByDate[dateKey].orEmpty(),
                        manualLogs = manualLogsByDate[dateKey].orEmpty(),
                    ),
                )
                current = current.plus(1, DateTimeUnit.DAY)
            }
        }
    }

    private fun beginSheetMutation(): Boolean {
        if (state.value.isSheetMutationInFlight) return false
        state.update { it.copy(isSheetMutationInFlight = true) }
        return true
    }

    private fun finishSheetMutationSuccess() {
        state.update {
            it.copy(
                isSheetMutationInFlight = false,
                completedSheetMutationCount = it.completedSheetMutationCount + 1,
            )
        }
    }

    private fun finishSheetMutationFailure(message: String) {
        state.update {
            it.copy(
                isSheetMutationInFlight = false,
                snackbarMessage = message,
            )
        }
    }
}

private fun Throwable.errorMessage(default: String): String = message ?: default
