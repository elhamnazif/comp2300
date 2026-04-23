package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class NotificationSettings(
    val routineRemindersEnabled: Boolean = true,
    val appointmentRemindersEnabled: Boolean = true,
)

class NotificationSettingsDataSource(private val settings: Settings) {
    val state: StateFlow<NotificationSettings>
        field: MutableStateFlow<NotificationSettings> = MutableStateFlow(loadSettings())

    fun setRoutineRemindersEnabled(enabled: Boolean) {
        settings.putBoolean(KeyRoutineRemindersEnabled, enabled)
        state.value = state.value.copy(routineRemindersEnabled = enabled)
    }

    fun setAppointmentRemindersEnabled(enabled: Boolean) {
        settings.putBoolean(KeyAppointmentRemindersEnabled, enabled)
        state.value = state.value.copy(appointmentRemindersEnabled = enabled)
    }

    private fun loadSettings(): NotificationSettings = NotificationSettings(
        routineRemindersEnabled = settings.getBoolean(KeyRoutineRemindersEnabled, true),
        appointmentRemindersEnabled = settings.getBoolean(KeyAppointmentRemindersEnabled, true),
    )

    private companion object {
        const val KeyRoutineRemindersEnabled = "notifications.routine_reminders_enabled"
        const val KeyAppointmentRemindersEnabled = "notifications.appointment_reminders_enabled"
    }
}
