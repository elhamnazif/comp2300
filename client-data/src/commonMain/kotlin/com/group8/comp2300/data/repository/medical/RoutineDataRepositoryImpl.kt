package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class RoutineDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val routineLocal: RoutineLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : RoutineDataRepository {
    override suspend fun getRoutines(): List<Routine> = routineLocal.getAll()

    override suspend fun saveRoutine(request: RoutineCreateRequest, id: String?): Routine {
        val routineId = id ?: Uuid.random().toString()
        val normalizedRequest =
            request.copy(
                name = request.name.trim(),
                endDate = request.endDate?.takeIf(String::isNotBlank),
                timesOfDayMs = request.timesOfDayMs.sorted().distinct(),
                reminderOffsetsMins =
                if (request.hasReminder) {
                    request.reminderOffsetsMins.sorted().distinct()
                } else {
                    emptyList()
                },
                medicationIds = request.medicationIds.distinct(),
            )
        val routine = Routine(
            id = routineId,
            userId = authRepository.session.value.userOrNull?.id.orEmpty(),
            name = normalizedRequest.name,
            timesOfDayMs = normalizedRequest.timesOfDayMs,
            repeatType = RoutineRepeatType.valueOf(normalizedRequest.repeatType),
            daysOfWeek = normalizedRequest.daysOfWeek.sorted().distinct(),
            startDate = normalizedRequest.startDate,
            endDate = normalizedRequest.endDate,
            hasReminder = normalizedRequest.hasReminder,
            reminderOffsetsMins = normalizedRequest.reminderOffsetsMins,
            status = RoutineStatus.valueOf(normalizedRequest.status),
            medicationIds = normalizedRequest.medicationIds,
        )
        routineLocal.insert(routine)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.ROUTINE_UPSERT,
            localId = routineId,
            payload = Json.encodeToString(normalizedRequest),
        )
        return routineLocal.getById(routineId) ?: routine
    }

    override suspend fun deleteRoutine(id: String) {
        queuedWriteDispatcher.deletePending(OutboxEntityType.ROUTINE_UPSERT, id)
        routineLocal.deleteById(id)
        if (authRepository.session.value.userOrNull != null) {
            queuedWriteDispatcher.replacePending(
                entityType = OutboxEntityType.ROUTINE_DELETE,
                localId = id,
                payload = "",
            )
        }
    }
}
