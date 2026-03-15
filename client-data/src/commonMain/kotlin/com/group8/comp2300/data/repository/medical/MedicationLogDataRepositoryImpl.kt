package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.repository.medical.MedicationLogDataRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

class MedicationLogDataRepositoryImpl(
    private val medicationLocal: MedicationLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MedicationLogDataRepository {
    override suspend fun getMedicationAgenda(date: String): List<MedicationLog> {
        val localDate = LocalDate.parse(date)
        val timezone = TimeZone.currentSystemDefault()
        val start = LocalDateTime(localDate, LocalTime(0, 0)).toInstant(timezone).toEpochMilliseconds()
        val end = LocalDateTime(localDate.plus(1, DateTimeUnit.DAY), LocalTime(0, 0)).toInstant(timezone).toEpochMilliseconds()
        return medicationLogLocal.getByDateRange(start, end)
    }

    override suspend fun logMedication(request: MedicationLogRequest): MedicationLog {
        val medication = medicationLocal.getById(request.medicationId)
        val timestamp = request.timestampMs ?: Clock.System.now().toEpochMilliseconds()
        val log = MedicationLog(
            id = Uuid.random().toString(),
            medicationId = request.medicationId,
            medicationTime = timestamp,
            status = MedicationLogStatus.valueOf(request.status),
            medicationName = medication?.name,
        )

        medicationLogLocal.insert(log)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.MEDICATION_LOG,
            localId = log.id,
            payload = Json.encodeToString(request.copy(timestampMs = timestamp)),
        )
        return medicationLogLocal.getAll().firstOrNull { it.id == log.id } ?: log
    }
}
