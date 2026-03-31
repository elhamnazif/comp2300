package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.QueuedOfflineStore
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.*
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class MedicationLogDataRepositoryImpl(
    private val medicationLocal: MedicationLocalDataSource,
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MedicationLogDataRepository {
    private val occurrenceOverrideWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.routineOccurrenceOverrideUpsert,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { _, request ->
            RoutineOccurrenceOverride(
                id = "${request.routineId}:${request.originalOccurrenceTimeMs}",
                routineId = request.routineId,
                originalOccurrenceTimeMs = request.originalOccurrenceTimeMs,
                rescheduledOccurrenceTimeMs = request.rescheduledOccurrenceTimeMs,
            )
        },
        saveLocal = routineOccurrenceOverrideLocal::insert,
        readLocal = { overrideId -> routineOccurrenceOverrideLocal.getAll().firstOrNull { it.id == overrideId } },
    )
    private val medicationLogWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.medicationLog,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { localId, request ->
            val medication = medicationLocal.getById(request.medicationId)
            MedicationLog(
                id = localId,
                medicationId = request.medicationId,
                medicationTime = request.timestampMs ?: Clock.System.now().toEpochMilliseconds(),
                status = MedicationLogStatus.valueOf(request.status),
                routineId = request.routineId,
                occurrenceTimeMs = request.occurrenceTimeMs,
                medicationName = medication?.name,
                routineName = request.routineId?.let { routineLocal.getById(it)?.name },
            )
        },
        saveLocal = medicationLogLocal::insert,
        readLocal = { logId -> medicationLogLocal.getAll().firstOrNull { it.id == logId } },
    )

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
    ): List<MedicationOccurrenceCandidate> = buildMedicationOccurrenceCandidates(
        routines = routineLocal.getAll(),
        medications = medicationLocal.getAll(),
        logs = medicationLogLocal.getAll(),
        overrides = routineOccurrenceOverrideLocal.getAll(),
        medicationId = medicationId,
        timestampMs = timestampMs,
        nowMs = Clock.System.now().toEpochMilliseconds(),
        timeZone = TimeZone.currentSystemDefault(),
    )

    override suspend fun rescheduleRoutineOccurrence(
        request: RoutineOccurrenceOverrideRequest,
    ): RoutineOccurrenceOverride {
        val routine = routineLocal.getById(request.routineId)
        val optimisticOverride = occurrenceOverrideWrites.write(
            request = request,
            id = "${request.routineId}:${request.originalOccurrenceTimeMs}",
        )
        return optimisticOverride.copy(routineId = routine?.id ?: request.routineId)
    }

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog {
        val timestamp = request.timestampMs ?: Clock.System.now().toEpochMilliseconds()
        val linkMode = request.linkMode ?: if (request.routineId != null && request.occurrenceTimeMs != null) {
            MedicationLogLinkMode.ATTACH_TO_OCCURRENCE
        } else {
            MedicationLogLinkMode.EXTRA_DOSE
        }
        val logId =
            if (linkMode == MedicationLogLinkMode.ATTACH_TO_OCCURRENCE && request.routineId != null &&
                request.occurrenceTimeMs != null
            ) {
                "${request.routineId}:${request.medicationId}:${request.occurrenceTimeMs}"
            } else {
                null
            }
        return medicationLogWrites.write(
            request = request.copy(timestampMs = timestamp, linkMode = linkMode),
            id = logId,
        )
    }
}
