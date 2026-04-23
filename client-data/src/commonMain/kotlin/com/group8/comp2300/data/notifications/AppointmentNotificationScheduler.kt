package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.NotificationSettingsDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.domain.model.medical.Appointment
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

interface AppointmentNotificationScheduler {
    suspend fun syncAppointment(appointment: Appointment)

    suspend fun removeAppointment(appointment: Appointment)

    suspend fun syncAllAppointments()
}

class AppointmentNotificationBootstrap(private val scheduler: AppointmentNotificationScheduler) {
    suspend fun synchronize() {
        scheduler.syncAllAppointments()
    }
}

class AppointmentNotificationSchedulerImpl(
    private val appointmentLocal: AppointmentLocalDataSource,
    private val registry: AppointmentNotificationRegistry,
    private val platform: LocalNotificationService,
    private val notificationSettingsDataSource: NotificationSettingsDataSource,
    private val privacySettingsDataSource: PrivacySettingsDataSource,
    private val notificationContentFormatter: NotificationContentFormatter,
    private val clock: Clock = Clock.System,
) : AppointmentNotificationScheduler {
    override suspend fun syncAppointment(appointment: Appointment) {
        cancelStoredNotification(appointment.id)

        if (!platform.notificationsEnabled() || !notificationSettingsDataSource.state.value.appointmentRemindersEnabled) {
            registry.remove(appointment.id)
            return
        }

        val notification = planner().notificationForAppointment(
            appointment = appointment,
            content = notificationContentFormatter.appointmentReminder(privacySettingsDataSource.state.value),
        ) ?: run {
            registry.remove(appointment.id)
            return
        }

        platform.schedule(notification)
        registry.replace(appointment.id, notification.id)
    }

    override suspend fun removeAppointment(appointment: Appointment) {
        cancelStoredNotification(appointment.id)
        registry.remove(appointment.id)
    }

    override suspend fun syncAllAppointments() {
        val trackedAppointmentIds = registry.all().keys
        val appointments = appointmentLocal.getAll()
        val appointmentIds = appointments.mapTo(mutableSetOf(), Appointment::id)

        if (!platform.notificationsEnabled() || !notificationSettingsDataSource.state.value.appointmentRemindersEnabled) {
            trackedAppointmentIds.forEach { cancelStoredNotification(it) }
            return
        }

        trackedAppointmentIds.filterNot(appointmentIds::contains).forEach { obsoleteId ->
            cancelStoredNotification(obsoleteId)
        }

        val planner = planner()
        val content = notificationContentFormatter.appointmentReminder(privacySettingsDataSource.state.value)
        appointments.forEach { appointment ->
            cancelStoredNotification(appointment.id)
            val notification = planner.notificationForAppointment(appointment = appointment, content = content) ?: return@forEach
            platform.schedule(notification)
            registry.replace(appointment.id, notification.id)
        }
    }

    private suspend fun cancelStoredNotification(appointmentId: String) {
        registry.idForAppointment(appointmentId)?.let { notificationId ->
            platform.cancel(notificationId)
        }
        registry.remove(appointmentId)
    }

    private fun planner(): AppointmentNotificationPlanner = AppointmentNotificationPlanner(
        nowMs = clock.now().toEpochMilliseconds(),
    )
}

class AppointmentNotificationRegistry(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun idForAppointment(appointmentId: String): String? = all()[appointmentId]

    fun replace(appointmentId: String, notificationId: String) {
        val state = all().toMutableMap()
        state[appointmentId] = notificationId
        write(state)
    }

    fun remove(appointmentId: String) {
        val state = all().toMutableMap()
        if (state.remove(appointmentId) != null) {
            write(state)
        }
    }

    fun all(): Map<String, String> = runCatching {
        settings.getStringOrNull(KEY)
            ?.let { stored -> json.decodeFromString<AppointmentNotificationRegistryState>(stored) }
            ?.appointmentNotificationIds
            ?: emptyMap()
    }.getOrDefault(emptyMap())

    private fun write(state: Map<String, String>) {
        if (state.isEmpty()) {
            settings.remove(KEY)
            return
        }
        settings.putString(
            KEY,
            json.encodeToString(AppointmentNotificationRegistryState(appointmentNotificationIds = state)),
        )
    }

    private companion object {
        const val KEY = "appointment_notification_registry"
    }
}

private class AppointmentNotificationPlanner(
    private val nowMs: Long,
    private val leadTimeMs: Long = 24L * 60L * 60L * 1000L,
) {
    fun notificationForAppointment(
        appointment: Appointment,
        content: NotificationContent,
    ): ScheduledLocalNotification? {
        if (appointment.status != "CONFIRMED" || !appointment.hasReminder) return null

        val fireAtMs = appointment.appointmentTime - leadTimeMs
        if (fireAtMs <= nowMs) return null

        return ScheduledLocalNotification(
            id = "appointment:${appointment.id}:24h",
            fireAtMs = fireAtMs,
            title = content.title,
            body = content.body,
        )
    }
}

@Serializable
private data class AppointmentNotificationRegistryState(
    val appointmentNotificationIds: Map<String, String> = emptyMap(),
)
