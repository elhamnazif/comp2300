package com.group8.comp2300.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.data.notifications.RoutineNotificationService
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class HomeViewModel(
    private val syncCoordinator: SyncCoordinator,
    private val appointmentRepository: AppointmentDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val medicationLogRepository: MedicationLogDataRepository,
    private val notificationService: RoutineNotificationService,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    internal val state: StateFlow<HomeUiState>
        field: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state.update { current -> current.copy(isLoading = true, errorMessage = null) }
            try {
                syncCoordinator.refreshAuthenticatedData()
                val now = clock.now().toEpochMilliseconds()
                val today = clock.now().toLocalDateTime(timeZone).date.toString()
                val appointments = appointmentRepository.getAppointments()
                val activeMedicationCount = medicationRepository.getMedications().count { it.status == MedicationStatus.ACTIVE }
                val agenda = medicationLogRepository.getRoutineAgenda(today)
                val notificationsEnabled = runCatching { notificationService.notificationsEnabled() }.getOrDefault(true)

                state.value =
                    HomeUiState(
                        isLoading = false,
                        greetingPeriod = buildGreetingPeriod(now, timeZone),
                        activeMedicationCount = activeMedicationCount,
                        todaySummary = buildTodaySummary(appointments = appointments, agenda = agenda, nowMs = now),
                        inboxItems = buildHomeInboxItems(
                            appointments = appointments,
                            agenda = agenda,
                            notificationsEnabled = notificationsEnabled,
                            nowMs = now,
                            timeZone = timeZone,
                        ),
                    )
            } catch (error: Exception) {
                state.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Failed to load home",
                    )
                }
            }
        }
    }
}
