package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.repository.newDatabase
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.russhwolf.settings.Settings
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class RoutineNotificationSchedulerTest {
    @Test
    fun dailyRoutineSchedulesOneNotificationPerFutureOccurrenceAndOffset() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val routine = fixture.dailyRoutine(reminderOffsetsMins = listOf(0, 15))

        fixture.scheduler.syncRoutine(routine)

        assertEquals(62, fixture.platform.scheduled.size)
        assertTrue(fixture.platform.scheduled.any { it.id == "${routine.id}:${utcMs(2026, Month.MARCH, 17, 9, 0)}:0" })
        assertTrue(fixture.platform.scheduled.any { it.id == "${routine.id}:${utcMs(2026, Month.MARCH, 17, 9, 0)}:15" })
    }

    @Test
    fun weeklyRoutineOnlySchedulesSelectedWeekdays() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val routine = fixture.dailyRoutine(
            repeatType = RoutineRepeatType.WEEKLY,
            daysOfWeek = listOf(1, 3),
        )

        fixture.scheduler.syncRoutine(routine)

        assertEquals(9, fixture.platform.scheduled.size)
        val selectedOccurrence = utcMs(2026, Month.MARCH, 18, 9, 0)
        assertTrue(fixture.platform.scheduled.any { it.id == "${routine.id}:$selectedOccurrence:0" })
        val thursdayOccurrence = utcMs(2026, Month.MARCH, 19, 9, 0)
        assertFalse(fixture.platform.scheduled.any { it.id == "${routine.id}:$thursdayOccurrence:0" })
    }

    @Test
    fun remindersDisabledSchedulesNothing() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val routine = fixture.dailyRoutine(hasReminder = false, reminderOffsetsMins = emptyList())

        fixture.scheduler.syncRoutine(routine)

        assertTrue(fixture.platform.scheduled.isEmpty())
        assertTrue(fixture.registry.idsForRoutine(routine.id).isEmpty())
    }

    @Test
    fun archivedOrEndedRoutinesScheduleNothing() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val archived = fixture.dailyRoutine(id = "archived", status = RoutineStatus.ARCHIVED)
        val ended = fixture.dailyRoutine(id = "ended", endDate = "2026-03-16")

        fixture.scheduler.syncRoutine(archived)
        fixture.scheduler.syncRoutine(ended)

        assertTrue(fixture.platform.scheduled.isEmpty())
    }

    @Test
    fun updatingRoutineCancelsOldNotificationIdsAndReplacesThem() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val original = fixture.dailyRoutine(timesOfDayMs = listOf(9 * 60 * 60 * 1000L))
        val updated = original.copy(timesOfDayMs = listOf(10 * 60 * 60 * 1000L))
        val originalId = "${original.id}:${utcMs(2026, Month.MARCH, 17, 9, 0)}:0"
        val updatedId = "${updated.id}:${utcMs(2026, Month.MARCH, 17, 10, 0)}:0"

        fixture.scheduler.syncRoutine(original)
        fixture.scheduler.syncRoutine(updated, previousRoutine = original)

        assertTrue(fixture.platform.canceled.any { it == originalId })
        assertFalse(originalId in fixture.registry.idsForRoutine(updated.id))
        assertTrue(updatedId in fixture.registry.idsForRoutine(updated.id))
    }

    @Test
    fun deletingRoutineCancelsStoredNotifications() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val routine = fixture.dailyRoutine()

        fixture.scheduler.syncRoutine(routine)
        fixture.scheduler.removeRoutine(routine)

        assertTrue(fixture.platform.canceled.isNotEmpty())
        assertTrue(fixture.registry.idsForRoutine(routine.id).isEmpty())
    }

    @Test
    fun occurrenceOverrideReplacesOriginalOccurrenceNotification() = runTest {
        val fixture = schedulerFixture(nowMs = utcMs(2026, Month.MARCH, 17, 8, 0))
        val routine = fixture.dailyRoutine()
        fixture.overrideLocal.insert(
            RoutineOccurrenceOverride(
                id = "${routine.id}:${utcMs(2026, Month.MARCH, 17, 9, 0)}",
                routineId = routine.id,
                originalOccurrenceTimeMs = utcMs(2026, Month.MARCH, 17, 9, 0),
                rescheduledOccurrenceTimeMs = utcMs(2026, Month.MARCH, 18, 11, 30),
            ),
        )

        fixture.scheduler.syncRoutine(routine)

        assertFalse(fixture.platform.scheduled.any { it.id == "${routine.id}:${utcMs(2026, Month.MARCH, 17, 9, 0)}:0" })
        assertTrue(
            fixture.platform.scheduled.any {
                it.id == "${routine.id}:${utcMs(2026, Month.MARCH, 18, 11, 30)}:0"
            },
        )
    }
}

private class SchedulerFixture(
    val scheduler: RoutineNotificationScheduler,
    val platform: RecordingPlatform,
    val registry: RoutineNotificationRegistry,
    val overrideLocal: RoutineOccurrenceOverrideLocalDataSource,
) {
    fun dailyRoutine(
        id: String = "routine-1",
        timesOfDayMs: List<Long> = listOf(9 * 60 * 60 * 1000L),
        reminderOffsetsMins: List<Int> = listOf(0),
        repeatType: RoutineRepeatType = RoutineRepeatType.DAILY,
        daysOfWeek: List<Int> = emptyList(),
        hasReminder: Boolean = true,
        status: RoutineStatus = RoutineStatus.ACTIVE,
        endDate: String? = null,
    ): Routine = Routine(
        id = id,
        userId = "user-1",
        name = "Morning meds",
        timesOfDayMs = timesOfDayMs,
        repeatType = repeatType,
        daysOfWeek = daysOfWeek,
        startDate = "2026-03-17",
        endDate = endDate,
        hasReminder = hasReminder,
        reminderOffsetsMins = reminderOffsetsMins,
        status = status,
        medicationIds = listOf("med-1"),
    )
}

private fun schedulerFixture(nowMs: Long): SchedulerFixture {
    val db = newDatabase()
    val routineLocal = RoutineLocalDataSource(db)
    val overrideLocal = RoutineOccurrenceOverrideLocalDataSource(db)
    val registry = RoutineNotificationRegistry(Settings())
    val platform = RecordingPlatform()
    return SchedulerFixture(
        scheduler = RoutineNotificationSchedulerImpl(
            routineLocal = routineLocal,
            routineOccurrenceOverrideLocal = overrideLocal,
            registry = registry,
            platform = platform,
            clock = FixedClock(nowMs),
            timeZone = TimeZone.UTC,
        ),
        platform = platform,
        registry = registry,
        overrideLocal = overrideLocal,
    )
}

private class RecordingPlatform : RoutineNotificationPlatform {
    val scheduled = mutableListOf<ScheduledRoutineNotification>()
    val canceled = mutableListOf<String>()

    override suspend fun schedule(notification: ScheduledRoutineNotification) {
        scheduled += notification
    }

    override suspend fun cancel(notificationId: String) {
        canceled += notificationId
    }

    override suspend fun notificationsEnabled(): Boolean = true
}

private class FixedClock(private val nowMs: Long) : Clock {
    override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
}

private fun utcMs(year: Int, month: Month, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime(year, month, day, hour, minute)
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()
