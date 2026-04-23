package com.group8.comp2300.data.local

import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsDataSourceTest {
    @Test
    fun notificationCategoriesDefaultToEnabledAndPersistChanges() {
        val settings = Settings()
        settings.remove("notifications.routine_reminders_enabled")
        settings.remove("notifications.appointment_reminders_enabled")
        val dataSource = NotificationSettingsDataSource(settings)

        assertTrue(dataSource.state.value.routineRemindersEnabled)
        assertTrue(dataSource.state.value.appointmentRemindersEnabled)

        dataSource.setRoutineRemindersEnabled(false)
        dataSource.setAppointmentRemindersEnabled(false)

        val restored = NotificationSettingsDataSource(settings).state.value
        assertFalse(restored.routineRemindersEnabled)
        assertFalse(restored.appointmentRemindersEnabled)
    }
}
