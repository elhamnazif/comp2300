package com.group8.comp2300.domain.model.medical

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.time.Instant

@Serializable
data class RoutineDayAgenda(
    val routineId: String,
    val routineName: String,
    val occurrenceTimeMs: Long,
    val originalOccurrenceTimeMs: Long = occurrenceTimeMs,
    val isRescheduled: Boolean = false,
    val hasReminder: Boolean,
    val reminderOffsetsMins: List<Int> = emptyList(),
    val medications: List<RoutineMedicationAgenda>,
)

@Serializable
data class RoutineMedicationAgenda(
    val medicationId: String,
    val medicationName: String,
    val dosage: String,
    val colorHex: String? = null,
    val status: MedicationLogStatus,
    val logId: String? = null,
    val loggedTimeMs: Long? = null,
)

fun buildMedicationOccurrenceCandidates(
    routines: List<Routine>,
    medications: List<Medication>,
    logs: List<MedicationLog>,
    overrides: List<RoutineOccurrenceOverride> = emptyList(),
    medicationId: String,
    timestampMs: Long,
    nowMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    windowHours: Int = 24,
): List<MedicationOccurrenceCandidate> {
    val activeMedication =
        medications.firstOrNull { it.id == medicationId && it.status == MedicationStatus.ACTIVE } ?: return emptyList()
    val windowMs = windowHours * 60L * 60L * 1000L
    val startDate = Instant.fromEpochMilliseconds(timestampMs - windowMs).toLocalDateTime(timeZone).date
    val endDate = Instant.fromEpochMilliseconds(timestampMs + windowMs).toLocalDateTime(timeZone).date

    return datesInRange(startDate, endDate)
        .asSequence()
        .flatMap { date ->
            buildRoutineDayAgenda(
                routines = routines,
                medications = medications,
                logs = logs,
                overrides = overrides,
                date = date,
                nowMs = nowMs,
                timeZone = timeZone,
            )
        }
        .flatMap { agenda ->
            agenda.medications
                .filter { medication ->
                    medication.medicationId == activeMedication.id &&
                        (
                            medication.status == MedicationLogStatus.PENDING ||
                                medication.status == MedicationLogStatus.MISSED
                            )
                }
                .map {
                    MedicationOccurrenceCandidate(
                        medicationId = it.medicationId,
                        routineId = agenda.routineId,
                        routineName = agenda.routineName,
                        occurrenceTimeMs = agenda.occurrenceTimeMs,
                        status = it.status,
                    )
                }
        }
        .filter { candidate -> abs(candidate.occurrenceTimeMs - timestampMs) <= windowMs }
        .distinctBy { candidate -> "${candidate.routineId}:${candidate.medicationId}:${candidate.occurrenceTimeMs}" }
        .sortedWith(
            compareBy<MedicationOccurrenceCandidate> { abs(it.occurrenceTimeMs - timestampMs) }
                .thenBy(MedicationOccurrenceCandidate::occurrenceTimeMs),
        )
        .toList()
}

fun buildRoutineDayAgenda(
    routines: List<Routine>,
    medications: List<Medication>,
    logs: List<MedicationLog>,
    overrides: List<RoutineOccurrenceOverride> = emptyList(),
    date: LocalDate,
    nowMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): List<RoutineDayAgenda> {
    val activeMedications = medications.filter { it.status == MedicationStatus.ACTIVE }.associateBy(Medication::id)
    val logBuckets =
        logs.filter { it.routineId != null && it.occurrenceTimeMs != null }
            .associateBy { AgendaLogKey(it.routineId!!, it.medicationId, it.occurrenceTimeMs!!) }
    val overrideBuckets =
        overrides.groupBy { OverrideKey(it.routineId, it.originalOccurrenceTimeMs) }
            .mapValues { (_, values) -> values.maxBy { it.rescheduledOccurrenceTimeMs } }
    val routineById = routines.associateBy(Routine::id)

    val baseAgenda = routines
        .filter { it.status == RoutineStatus.ACTIVE && it.isDueOn(date) }
        .flatMap { routine ->
            routine.timesOfDayMs.sorted().distinct().mapNotNull { timeOfDayMs ->
                val occurrenceTimeMs = date.atOccurrenceTime(timeOfDayMs, timeZone)
                if (overrideBuckets.containsKey(OverrideKey(routine.id, occurrenceTimeMs))) {
                    return@mapNotNull null
                }
                toRoutineDayAgenda(
                    routine = routine,
                    activeMedications = activeMedications,
                    logBuckets = logBuckets,
                    effectiveOccurrenceTimeMs = occurrenceTimeMs,
                    originalOccurrenceTimeMs = occurrenceTimeMs,
                    isRescheduled = false,
                    nowMs = nowMs,
                )
            }
        }
    val overrideAgenda = overrideBuckets.values
        .mapNotNull { override ->
            val routine = routineById[override.routineId] ?: return@mapNotNull null
            if (routine.status != RoutineStatus.ACTIVE) {
                return@mapNotNull null
            }
            val originalDate = Instant.fromEpochMilliseconds(
                override.originalOccurrenceTimeMs,
            ).toLocalDateTime(timeZone).date
            val rescheduledDate = Instant.fromEpochMilliseconds(
                override.rescheduledOccurrenceTimeMs,
            ).toLocalDateTime(timeZone).date
            if (rescheduledDate != date || !routine.isDueOn(originalDate)) {
                return@mapNotNull null
            }
            toRoutineDayAgenda(
                routine = routine,
                activeMedications = activeMedications,
                logBuckets = logBuckets,
                effectiveOccurrenceTimeMs = override.rescheduledOccurrenceTimeMs,
                originalOccurrenceTimeMs = override.originalOccurrenceTimeMs,
                isRescheduled = override.rescheduledOccurrenceTimeMs != override.originalOccurrenceTimeMs,
                nowMs = nowMs,
            )
        }

    return (baseAgenda + overrideAgenda)
        .distinctBy { "${it.routineId}:${it.originalOccurrenceTimeMs}:${it.occurrenceTimeMs}" }
        .sortedBy(RoutineDayAgenda::occurrenceTimeMs)
}

private fun toRoutineDayAgenda(
    routine: Routine,
    activeMedications: Map<String, Medication>,
    logBuckets: Map<AgendaLogKey, MedicationLog>,
    effectiveOccurrenceTimeMs: Long,
    originalOccurrenceTimeMs: Long,
    isRescheduled: Boolean,
    nowMs: Long,
): RoutineDayAgenda? {
    val agendaMeds =
        routine.medicationIds.mapNotNull { medicationId ->
            val medication = activeMedications[medicationId] ?: return@mapNotNull null
            val matchedLog = logBuckets[AgendaLogKey(routine.id, medicationId, effectiveOccurrenceTimeMs)]
            RoutineMedicationAgenda(
                medicationId = medication.id,
                medicationName = medication.name,
                dosage = medication.dosage,
                colorHex = medication.colorHex,
                status = matchedLog?.status ?: derivedAgendaStatus(effectiveOccurrenceTimeMs, nowMs),
                logId = matchedLog?.id,
                loggedTimeMs = matchedLog?.medicationTime,
            )
        }
    if (agendaMeds.isEmpty()) {
        return null
    }
    return RoutineDayAgenda(
        routineId = routine.id,
        routineName = routine.name,
        occurrenceTimeMs = effectiveOccurrenceTimeMs,
        originalOccurrenceTimeMs = originalOccurrenceTimeMs,
        isRescheduled = isRescheduled,
        hasReminder = routine.hasReminder,
        reminderOffsetsMins = routine.reminderOffsetsMins.sorted().distinct(),
        medications = agendaMeds.sortedBy(RoutineMedicationAgenda::medicationName),
    )
}

fun buildCalendarOverviewStatus(
    routines: List<Routine>,
    medications: List<Medication>,
    logs: List<MedicationLog>,
    overrides: List<RoutineOccurrenceOverride> = emptyList(),
    date: LocalDate,
    nowMs: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val agenda = buildRoutineDayAgenda(routines, medications, logs, overrides, date, nowMs, timeZone)
    val routineStatuses = agenda.flatMap(RoutineDayAgenda::medications)
    val hasManualTaken =
        logs.any {
            it.routineId == null &&
                it.status == MedicationLogStatus.TAKEN &&
                it.medicationTime >= date.atTime(0, 0).toInstant(timeZone).toEpochMilliseconds() &&
                it.medicationTime < date.plusDays(1).atTime(0, 0).toInstant(timeZone).toEpochMilliseconds()
        }
    return when {
        routineStatuses.any { it.status == MedicationLogStatus.TAKEN } || hasManualTaken -> "TAKEN"

        routineStatuses.any {
            it.status == MedicationLogStatus.SKIPPED || it.status == MedicationLogStatus.MISSED
        } -> "MISSED"

        else -> "NONE"
    }
}

private data class AgendaLogKey(val routineId: String, val medicationId: String, val occurrenceTimeMs: Long)

private data class OverrideKey(val routineId: String, val originalOccurrenceTimeMs: Long)

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

private fun derivedAgendaStatus(occurrenceTimeMs: Long, nowMs: Long): MedicationLogStatus =
    if (occurrenceTimeMs <= nowMs) MedicationLogStatus.MISSED else MedicationLogStatus.PENDING

private fun LocalDate.atOccurrenceTime(offsetMs: Long, timeZone: TimeZone): Long {
    val totalMinutes = (offsetMs / 60_000).toInt()
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    return LocalDateTime(this, LocalTime(hour, minute)).toInstant(timeZone).toEpochMilliseconds()
}

private fun LocalDate.plusDays(days: Int): LocalDate = plus(days, DateTimeUnit.DAY)

private fun datesInRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = startDate
    while (current <= endDate) {
        dates += current
        current = current.plusDays(1)
    }
    return dates
}

fun DayOfWeek.toStorageDayOfWeek(): Int = isoDayNumber % 7
