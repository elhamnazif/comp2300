package com.group8.comp2300.data.notifications

import com.group8.comp2300.data.local.PrivacySettingsDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.russhwolf.settings.Settings
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

interface RoutineNotificationScheduler {
    suspend fun syncRoutine(routine: Routine, previousRoutine: Routine? = null)

    suspend fun removeRoutine(routine: Routine)

    suspend fun syncAllRoutines()
}

class RoutineNotificationBootstrap(private val scheduler: RoutineNotificationScheduler) {
    suspend fun synchronize() {
        scheduler.syncAllRoutines()
    }
}

interface RoutineNotificationService {
    suspend fun schedule(notification: ScheduledRoutineNotification)

    suspend fun cancel(notificationId: String)

    suspend fun notificationsEnabled(): Boolean
}

data class ScheduledRoutineNotification(
    val id: String,
    val routineId: String,
    val fireAtMs: Long,
    val title: String,
    val body: String,
)

class RoutineNotificationSchedulerImpl(
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val registry: RoutineNotificationRegistry,
    private val platform: RoutineNotificationService,
    private val privacySettingsDataSource: PrivacySettingsDataSource,
    private val notificationContentFormatter: NotificationContentFormatter,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : RoutineNotificationScheduler {
    override suspend fun syncRoutine(routine: Routine, previousRoutine: Routine?) {
        cancelStoredNotifications(routine.id)
        if (previousRoutine != null && previousRoutine.id != routine.id) {
            cancelStoredNotifications(previousRoutine.id)
        }

        if (!platform.notificationsEnabled()) {
            registry.remove(routine.id)
            return
        }

        val notifications = planner().planForRoutine(
            routine = routine,
            overrides = routineOccurrenceOverrideLocal.getAll(),
        )
        notifications.forEach { platform.schedule(it) }
        registry.replace(routine.id, notifications.map(ScheduledRoutineNotification::id))
    }

    override suspend fun removeRoutine(routine: Routine) {
        cancelStoredNotifications(routine.id)
        registry.remove(routine.id)
    }

    override suspend fun syncAllRoutines() {
        val trackedRoutineIds = registry.all().keys
        val routines = routineLocal.getAll()
        val routineIds = routines.mapTo(mutableSetOf(), Routine::id)

        if (!platform.notificationsEnabled()) {
            trackedRoutineIds.forEach { cancelStoredNotifications(it) }
            return
        }

        trackedRoutineIds.filterNot(routineIds::contains).forEach { obsoleteId ->
            cancelStoredNotifications(obsoleteId)
        }

        val overrides = routineOccurrenceOverrideLocal.getAll()
        val planner = planner()
        routines.forEach { routine ->
            cancelStoredNotifications(routine.id)
            val notifications = planner.planForRoutine(routine = routine, overrides = overrides)
            notifications.forEach { platform.schedule(it) }
            registry.replace(routine.id, notifications.map(ScheduledRoutineNotification::id))
        }
    }

    private suspend fun cancelStoredNotifications(routineId: String) {
        registry.idsForRoutine(routineId).forEach { notificationId ->
            platform.cancel(notificationId)
        }
        registry.remove(routineId)
    }

    private fun planner(): RoutineNotificationPlanner = RoutineNotificationPlanner(
        nowMs = clock.now().toEpochMilliseconds(),
        timeZone = timeZone,
        routineReminderContent = notificationContentFormatter.routineReminder(privacySettingsDataSource.state.value),
    )
}

class RoutineNotificationRegistry(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun idsForRoutine(routineId: String): List<String> = all()[routineId].orEmpty()

    fun replace(routineId: String, notificationIds: List<String>) {
        val state = all().toMutableMap()
        if (notificationIds.isEmpty()) {
            state.remove(routineId)
        } else {
            state[routineId] = notificationIds.distinct()
        }
        write(state)
    }

    fun remove(routineId: String) {
        val state = all().toMutableMap()
        if (state.remove(routineId) != null) {
            write(state)
        }
    }

    fun all(): Map<String, List<String>> = runCatching {
        settings.getStringOrNull(KEY)
            ?.let { stored -> json.decodeFromString<RoutineNotificationRegistryState>(stored) }
            ?.routineNotificationIds
            ?: emptyMap()
    }.getOrDefault(emptyMap())

    private fun write(state: Map<String, List<String>>) {
        if (state.isEmpty()) {
            settings.remove(KEY)
            return
        }
        settings.putString(
            KEY,
            json.encodeToString(RoutineNotificationRegistryState(routineNotificationIds = state)),
        )
    }

    private companion object {
        const val KEY = "routine_notification_registry"
    }
}

private class RoutineNotificationPlanner(
    private val nowMs: Long,
    private val timeZone: TimeZone,
    private val routineReminderContent: NotificationContent,
    private val horizonDays: Int = 30,
) {
    fun planForRoutine(
        routine: Routine,
        overrides: List<RoutineOccurrenceOverride>,
    ): List<ScheduledRoutineNotification> {
        if (!routine.shouldScheduleNotifications(nowMs = nowMs, timeZone = timeZone)) return emptyList()

        val maxOffsetMins = routine.reminderOffsetsMins.maxOrNull() ?: 0
        val planningStart = Instant.fromEpochMilliseconds(nowMs - (maxOffsetMins * 60_000L))
            .toLocalDateTime(timeZone)
            .date
        val planningEnd = Instant.fromEpochMilliseconds(nowMs)
            .toLocalDateTime(timeZone)
            .date
            .plus(horizonDays, DateTimeUnit.DAY)
        val routineOverrides = overrides.filter { it.routineId == routine.id }
        val overridesByOriginalOccurrence = routineOverrides.associateBy(
            RoutineOccurrenceOverride::originalOccurrenceTimeMs,
        )

        val baseNotifications = datesInRange(
            start = maxOf(planningStart, LocalDate.parse(routine.startDate)),
            endInclusive = planningEnd,
        ).flatMap { date ->
            if (!routine.isDueOn(date)) return@flatMap emptyList()

            routine.timesOfDayMs.sorted().distinct().flatMap { timeOfDayMs ->
                val occurrenceTimeMs = date.atOccurrenceTime(timeOfDayMs, timeZone)
                if (overridesByOriginalOccurrence.containsKey(occurrenceTimeMs)) {
                    emptyList()
                } else {
                    notificationsForOccurrence(routine = routine, occurrenceTimeMs = occurrenceTimeMs)
                }
            }
        }

        val overrideNotifications = routineOverrides.flatMap { override ->
            val originalDate = Instant.fromEpochMilliseconds(override.originalOccurrenceTimeMs)
                .toLocalDateTime(timeZone)
                .date
            val rescheduledDate = Instant.fromEpochMilliseconds(override.rescheduledOccurrenceTimeMs)
                .toLocalDateTime(timeZone)
                .date
            if (!routine.isDueOn(originalDate)) return@flatMap emptyList()
            if (rescheduledDate < planningStart || rescheduledDate > planningEnd) return@flatMap emptyList()
            notificationsForOccurrence(routine = routine, occurrenceTimeMs = override.rescheduledOccurrenceTimeMs)
        }

        return (baseNotifications + overrideNotifications)
            .distinctBy(ScheduledRoutineNotification::id)
            .sortedBy(ScheduledRoutineNotification::fireAtMs)
    }

    private fun notificationsForOccurrence(
        routine: Routine,
        occurrenceTimeMs: Long,
    ): List<ScheduledRoutineNotification> = routine.reminderOffsetsMins
        .sorted()
        .distinct()
        .mapNotNull { offsetMins ->
            val fireAtMs = occurrenceTimeMs - (offsetMins * 60_000L)
            if (fireAtMs <= nowMs) return@mapNotNull null
            ScheduledRoutineNotification(
                id = "${routine.id}:$occurrenceTimeMs:$offsetMins",
                routineId = routine.id,
                fireAtMs = fireAtMs,
                title = routineReminderContent.title,
                body = routineReminderContent.body,
            )
        }
}

@Serializable
private data class RoutineNotificationRegistryState(val routineNotificationIds: Map<String, List<String>> = emptyMap())

private fun Routine.shouldScheduleNotifications(nowMs: Long, timeZone: TimeZone): Boolean =
    status == RoutineStatus.ACTIVE &&
        hasReminder &&
        reminderOffsetsMins.isNotEmpty() &&
        medicationIds.isNotEmpty() &&
        timesOfDayMs.isNotEmpty() &&
        (
            endDate
                ?.takeIf(String::isNotBlank)
                ?.let(LocalDate::parse)
                ?.let { end -> end >= Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(timeZone).date }
                ?: true
            )

private fun Routine.isDueOn(date: LocalDate): Boolean {
    val start = LocalDate.parse(startDate)
    val end = endDate?.takeIf(String::isNotBlank)?.let(LocalDate::parse)
    if (date < start || (end != null && date > end)) return false
    return when (repeatType) {
        RoutineRepeatType.DAILY -> true

        RoutineRepeatType.WEEKLY -> {
            val selected = daysOfWeek.toSet()
            selected.isNotEmpty() && date.dayOfWeek.toStorageDayOfWeek() in selected
        }
    }
}

private fun LocalDate.atOccurrenceTime(offsetMs: Long, timeZone: TimeZone): Long {
    val totalMinutes = (offsetMs / 60_000).toInt()
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    return LocalDateTime(this, LocalTime(hour, minute)).toInstant(timeZone).toEpochMilliseconds()
}

private fun DayOfWeek.toStorageDayOfWeek(): Int = when (this) {
    DayOfWeek.SUNDAY -> 0
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
}

private fun datesInRange(start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
    if (start > endInclusive) return emptyList()

    val dates = mutableListOf<LocalDate>()
    var cursor = start
    while (cursor <= endInclusive) {
        dates += cursor
        cursor = cursor.plus(1, DateTimeUnit.DAY)
    }
    return dates
}
