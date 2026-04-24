package com.group8.comp2300.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.data.local.NotificationSettings
import com.group8.comp2300.data.local.NotificationSettingsDataSource
import com.group8.comp2300.data.notifications.LocalNotificationService
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.repository.medical.AppointmentDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import com.group8.comp2300.domain.repository.medical.OfflineSyncCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class HomeViewModel(
    private val syncCoordinator: OfflineSyncCoordinator,
    private val appointmentRepository: AppointmentDataRepository,
    private val medicationRepository: MedicationDataRepository,
    private val medicationLogRepository: MedicationLogDataRepository,
    private val notificationService: LocalNotificationService,
    private val notificationSettingsDataSource: NotificationSettingsDataSource,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {
    internal val state: StateFlow<HomeUiState>
        field: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState())
    private var loadedSnapshot: LoadedHomeSnapshot? = null

    init {
        viewModelScope.launch {
            notificationSettingsDataSource.state.collectLatest(::applyNotificationSettings)
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            state.update { current -> current.copy(isLoading = true, errorMessage = null) }
            try {
                syncCoordinator.refreshCaches()
                val now = clock.now().toEpochMilliseconds()
                val today = clock.now().toLocalDateTime(timeZone).date.toString()
                val appointments = appointmentRepository.getAppointments()
                val activeMedicationCount =
                    medicationRepository.getMedications().count { it.status == MedicationStatus.ACTIVE }
                val agenda = medicationLogRepository.getRoutineAgenda(today)
                val snapshot = LoadedHomeSnapshot(
                    appointments = appointments,
                    agenda = agenda,
                    activeMedicationCount = activeMedicationCount,
                )
                loadedSnapshot = snapshot

                state.value =
                    buildLoadedState(snapshot = snapshot, settings = notificationSettingsDataSource.state.value)
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

    private suspend fun applyNotificationSettings(settings: NotificationSettings) {
        val snapshot = loadedSnapshot ?: return
        val currentErrorMessage = state.value.errorMessage
        state.value =
            buildLoadedState(snapshot = snapshot, settings = settings).copy(errorMessage = currentErrorMessage)
    }

    private suspend fun buildLoadedState(snapshot: LoadedHomeSnapshot, settings: NotificationSettings): HomeUiState {
        val now = clock.now().toEpochMilliseconds()
        val routineNotificationsEnabled =
            runCatching { notificationService.notificationsEnabled() }.getOrDefault(true) &&
                settings.routineRemindersEnabled

        return HomeUiState(
            isLoading = false,
            greetingPeriod = buildGreetingPeriod(now, timeZone),
            activeMedicationCount = snapshot.activeMedicationCount,
            todaySummary = buildTodaySummary(
                appointments = snapshot.appointments,
                agenda = snapshot.agenda,
                nowMs = now,
            ),
            inboxItems = buildHomeInboxItems(
                appointments = snapshot.appointments,
                agenda = snapshot.agenda,
                notificationsEnabled = routineNotificationsEnabled,
                nowMs = now,
                timeZone = timeZone,
            ),
        )
    }
}

private data class LoadedHomeSnapshot(
    val appointments: List<Appointment>,
    val agenda: List<RoutineDayAgenda>,
    val activeMedicationCount: Int,
)
