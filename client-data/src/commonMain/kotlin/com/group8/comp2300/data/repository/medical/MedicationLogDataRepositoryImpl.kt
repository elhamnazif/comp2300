package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class MedicationLogDataRepositoryImpl(
    private val medicationLocal: MedicationLocalDataSource,
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MedicationLogDataRepository {
    override suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda> {
        val localDate = LocalDate.parse(date)
        return buildRoutineDayAgenda(
            routines = routineLocal.getAll(),
            medications = medicationLocal.getAll(),
            logs = medicationLogLocal.getAll(),
            overrides = routineOccurrenceOverrideLocal.getAll(),
            date = localDate,
            nowMs = Clock.System.now().toEpochMilliseconds(),
            timeZone = TimeZone.currentSystemDefault(),
        )
    }

    override suspend fun getManualMedicationLogs(date: String): List<MedicationLog> {
        val localDate = LocalDate.parse(date)
        return medicationLogLocal.getAll()
            .filter { it.routineId == null }
            .filter {
                Instant.fromEpochMilliseconds(it.medicationTime)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date == localDate
            }
            .sortedByDescending(MedicationLog::medicationTime)
    }

    override suspend fun getMedicationOccurrenceCandidates(
        medicationId: String,
        timestampMs: Long,
    ): List<MedicationOccurrenceCandidate> =
        buildMedicationOccurrenceCandidates(
            routines = routineLocal.getAll(),
            medications = medicationLocal.getAll(),
            logs = medicationLogLocal.getAll(),
            overrides = routineOccurrenceOverrideLocal.getAll(),
            medicationId = medicationId,
            timestampMs = timestampMs,
            nowMs = Clock.System.now().toEpochMilliseconds(),
            timeZone = TimeZone.currentSystemDefault(),
        )

    override suspend fun rescheduleRoutineOccurrence(request: RoutineOccurrenceOverrideRequest): RoutineOccurrenceOverride {
        val routine = routineLocal.getById(request.routineId)
        val override = RoutineOccurrenceOverride(
            id = "${request.routineId}:${request.originalOccurrenceTimeMs}",
            routineId = request.routineId,
            originalOccurrenceTimeMs = request.originalOccurrenceTimeMs,
            rescheduledOccurrenceTimeMs = request.rescheduledOccurrenceTimeMs,
        )
        routineOccurrenceOverrideLocal.insert(override)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.ROUTINE_OCCURRENCE_OVERRIDE_UPSERT,
            localId = override.id,
            payload = Json.encodeToString(request),
        )
        return routineOccurrenceOverrideLocal.getAll().firstOrNull { it.id == override.id }
            ?: override.copy(routineId = routine?.id ?: request.routineId)
    }

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog {
        val medication = medicationLocal.getById(request.medicationId)
        val timestamp = request.timestampMs ?: Clock.System.now().toEpochMilliseconds()
        val linkMode = request.linkMode ?: if (request.routineId != null && request.occurrenceTimeMs != null) {
            MedicationLogLinkMode.ATTACH_TO_OCCURRENCE
        } else {
            MedicationLogLinkMode.EXTRA_DOSE
        }
        val logId =
            if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && request.routineId != null && request.occurrenceTimeMs != null) {
                "${request.routineId}:${request.medicationId}:${request.occurrenceTimeMs}"
            } else {
                Uuid.random().toString()
            }
        val log = MedicationLog(
            id = logId,
            medicationId = request.medicationId,
            medicationTime = timestamp,
            status = MedicationLogStatus.valueOf(request.status),
            routineId = request.routineId,
            occurrenceTimeMs = request.occurrenceTimeMs,
            medicationName = medication?.name,
            routineName = request.routineId?.let { routineLocal.getById(it)?.name },
        )

        medicationLogLocal.insert(log)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.MEDICATION_LOG,
            localId = log.id,
            payload = Json.encodeToString(request.copy(timestampMs = timestamp, linkMode = linkMode)),
        )
        return medicationLogLocal.getAll().firstOrNull { it.id == log.id } ?: log
    }
}
