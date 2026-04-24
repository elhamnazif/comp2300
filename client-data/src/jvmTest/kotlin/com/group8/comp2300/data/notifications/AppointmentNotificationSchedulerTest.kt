package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.NotificationPrivacyMode
import com.group8.comp2300.data.local.NotificationSettingsDataSource
import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.data.repository.newDatabase
import com.group8.comp2300.domain.model.medical.Appointment
import com.russhwolf.settings.Settings
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class AppointmentNotificationSchedulerTest {
    @Test
    fun confirmedAppointmentWithReminderSchedulesOneNotification24HoursEarly() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 18, 9, 0))

        fixture.scheduler.syncAppointment(appointment)

        assertEquals(1, fixture.platform.scheduled.size)
        val scheduled = fixture.platform.scheduled.single()
        assertEquals("appointment:${appointment.id}:24h", scheduled.id)
        assertEquals(utcMs(2026, Month.MARCH, 17, 9, 0), scheduled.fireAtMs)
    }

    @Test
    fun appointmentSettingOffCancelsStoredNotificationsAndSkipsRescheduling() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 18, 9, 0))

        fixture.scheduler.syncAppointment(appointment)
        fixture.notificationSettingsDataSource.setAppointmentRemindersEnabled(false)
        fixture.scheduler.syncAllAppointments()

        assertTrue(fixture.platform.canceled.contains("appointment:${appointment.id}:24h"))
        assertEquals(emptyMap(), fixture.registry.all())
    }

    @Test
    fun appointmentsWithin24HoursAreSkipped() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 17, 20, 0))

        fixture.scheduler.syncAppointment(appointment)

        assertEquals(1, fixture.platform.scheduled.size)
        val scheduled = fixture.platform.scheduled.single()
        assertEquals("appointment:${appointment.id}:2h", scheduled.id)
        assertEquals(utcMs(2026, Month.MARCH, 17, 18, 0), scheduled.fireAtMs)
    }

    @Test
    fun appointmentsWithinTwoHoursUseThirtyMinuteReminder() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 17, 9, 30))

        fixture.scheduler.syncAppointment(appointment)

        assertEquals(1, fixture.platform.scheduled.size)
        val scheduled = fixture.platform.scheduled.single()
        assertEquals("appointment:${appointment.id}:30m", scheduled.id)
        assertEquals(utcMs(2026, Month.MARCH, 17, 9, 0), scheduled.fireAtMs)
    }

    @Test
    fun cancelledAppointmentRemovesStoredNotification() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 18, 9, 0))

        fixture.scheduler.syncAppointment(appointment)
        fixture.scheduler.syncAppointment(appointment.copy(status = "CANCELLED"))

        assertTrue(fixture.platform.canceled.contains("appointment:${appointment.id}:24h"))
        assertEquals(emptyMap(), fixture.registry.all())
    }

    @Test
    fun reschedulingAppointmentReplacesOldNotification() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val original = fixture.appointment(id = "appointment-1", appointmentTime = utcMs(2026, Month.MARCH, 18, 9, 0))
        val updated = original.copy(appointmentTime = utcMs(2026, Month.MARCH, 19, 15, 0))

        fixture.scheduler.syncAppointment(original)
        fixture.scheduler.syncAppointment(updated)

        assertTrue(fixture.platform.canceled.contains("appointment:${original.id}:24h"))
        assertEquals("appointment:${updated.id}:24h", fixture.platform.scheduled.last().id)
        assertEquals("appointment:${updated.id}:24h", fixture.registry.idForAppointment(updated.id))
    }

    @Test
    fun aliasPrivacyModeUsesAliasCopy() = runTest {
        val fixture = schedulerFixture(
            nowMs = utcMs(2026, Month.MARCH, 17, 8, 0),
            notificationPrivacyMode = NotificationPrivacyMode.ALIAS_BASED,
            notificationAlias = "Care Buddy",
        )
        val appointment = fixture.appointment(appointmentTime = utcMs(2026, Month.MARCH, 18, 9, 0))

        fixture.scheduler.syncAppointment(appointment)

        assertEquals("Reminder for Care Buddy", fixture.platform.scheduled.single().title)
    }
}

private class AppointmentSchedulerFixture(
    val scheduler: AppointmentNotificationScheduler,
    val platform: RecordingLocalNotificationService,
    val registry: AppointmentNotificationRegistry,
    val notificationSettingsDataSource: NotificationSettingsDataSource,
) {
    fun appointment(
        id: String = "appointment-1",
        appointmentTime: Long,
        status: String = "CONFIRMED",
        hasReminder: Boolean = true,
    ): Appointment = Appointment(
        id = id,
        userId = "user-1",
        title = "Appointment at Clinic",
        appointmentTime = appointmentTime,
        appointmentType = "CONSULTATION",
        clinicId = "clinic-1",
        bookingId = "slot-1",
        status = status,
        notes = "Bring results",
        hasReminder = hasReminder,
        paymentStatus = "PENDING",
    )
}

private fun schedulerFixture(
    nowMs: Long,
    notificationPrivacyMode: NotificationPrivacyMode = NotificationPrivacyMode.NEUTRAL,
    notificationAlias: String = "",
): AppointmentSchedulerFixture {
    val db = newDatabase()
    val settings = newNotificationSettings()
    val appointmentLocal = AppointmentLocalDataSource(db)
    val privacySettingsDataSource = PrivacySettingsDataSource(settings)
    val notificationSettingsDataSource = NotificationSettingsDataSource(settings)
    privacySettingsDataSource.setNotificationPrivacyMode(notificationPrivacyMode)
    privacySettingsDataSource.setNotificationAlias(notificationAlias)
    val registry = AppointmentNotificationRegistry(settings)
    val platform = RecordingLocalNotificationService()
    return AppointmentSchedulerFixture(
        scheduler = AppointmentNotificationSchedulerImpl(
            appointmentLocal = appointmentLocal,
            registry = registry,
            platform = platform,
            notificationSettingsDataSource = notificationSettingsDataSource,
            privacySettingsDataSource = privacySettingsDataSource,
            notificationContentFormatter = NotificationContentFormatter(),
            clock = AppointmentFixedClock(nowMs),
        ),
        platform = platform,
        registry = registry,
        notificationSettingsDataSource = notificationSettingsDataSource,
    )
}

private fun newNotificationSettings(): Settings = Settings().also {
    it.remove("notifications.routine_reminders_enabled")
    it.remove("notifications.appointment_reminders_enabled")
}

private class RecordingLocalNotificationService : LocalNotificationService {
    val scheduled = mutableListOf<ScheduledLocalNotification>()
    val canceled = mutableListOf<String>()

    override suspend fun schedule(notification: ScheduledLocalNotification) {
        scheduled += notification
    }

    override suspend fun cancel(notificationId: String) {
        canceled += notificationId
    }

    override suspend fun notificationsEnabled(): Boolean = true
}

private class AppointmentFixedClock(private val nowMs: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
}

private fun utcMs(year: Int, month: Month, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime(year, month, day, hour, minute)
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()
