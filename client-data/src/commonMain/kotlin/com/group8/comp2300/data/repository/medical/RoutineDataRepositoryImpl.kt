package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.notifications.RoutineNotificationScheduler
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.QueuedOfflineStore
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.RoutineDataRepository

class RoutineDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val routineLocal: RoutineLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
    private val routineNotificationScheduler: RoutineNotificationScheduler,
) : RoutineDataRepository {
    private val routineWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.routineUpsert,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { routineId, request ->
            Routine(
                id = routineId,
                userId = authRepository.session.value.userOrNull?.id.orEmpty(),
                name = request.name,
                timesOfDayMs = request.timesOfDayMs,
                repeatType = RoutineRepeatType.valueOf(request.repeatType),
                daysOfWeek = request.daysOfWeek.sorted().distinct(),
                startDate = request.startDate,
                endDate = request.endDate,
                hasReminder = request.hasReminder,
                reminderOffsetsMins = request.reminderOffsetsMins,
                status = RoutineStatus.valueOf(request.status),
                medicationIds = request.medicationIds,
            )
        },
        saveLocal = routineLocal::insert,
        readLocal = routineLocal::getById,
    )

    override suspend fun getRoutines(): List<Routine> = routineLocal.getAll()

    override suspend fun saveRoutine(request: RoutineCreateRequest, id: String?): Routine {
        val previousRoutine = id?.let(routineLocal::getById)
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
        return routineWrites.write(normalizedRequest, id).also { savedRoutine ->
            routineNotificationScheduler.syncRoutine(routine = savedRoutine, previousRoutine = previousRoutine)
        }
    }

    override suspend fun deleteRoutine(id: String) {
        val existingRoutine = routineLocal.getById(id)
        queuedWriteDispatcher.deletePending(MedicalOfflineMutations.routineUpsert, id)
        routineLocal.deleteById(id)
        existingRoutine?.let(routineNotificationScheduler::removeRoutine)
        if (authRepository.session.value.userOrNull != null) {
            queuedWriteDispatcher.replacePending(MedicalOfflineMutations.routineDelete, id, Unit)
        }
    }
}
