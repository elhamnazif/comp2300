package com.group8.comp2300.data.offline

import com.group8.comp2300.data.local.*
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.*

class MedicationUpsertMutationHandler(
    private val apiService: ApiService,
    private val medicationLocal: MedicationLocalDataSource,
) : DecodingOfflineMutationHandler<MedicationCreateRequest>(
    MedicalOfflineMutations.medicationUpsert,
) {
    override suspend fun handle(item: OutboxItem, payload: MedicationCreateRequest) {
        medicationLocal.insert(apiService.upsertMedication(item.localId, payload))
    }
}

class MedicationDeleteMutationHandler(private val apiService: ApiService) :
    ItemOnlyOfflineMutationHandler(MedicalOfflineMutations.medicationDelete.type) {
    override suspend fun apply(item: OutboxItem) {
        apiService.deleteMedication(item.localId)
    }
}

class RoutineUpsertMutationHandler(
    private val apiService: ApiService,
    private val routineLocal: RoutineLocalDataSource,
) : DecodingOfflineMutationHandler<RoutineCreateRequest>(
    MedicalOfflineMutations.routineUpsert,
) {
    override suspend fun handle(item: OutboxItem, payload: RoutineCreateRequest) {
        routineLocal.insert(apiService.upsertRoutine(item.localId, payload))
    }
}

class RoutineDeleteMutationHandler(private val apiService: ApiService) :
    ItemOnlyOfflineMutationHandler(MedicalOfflineMutations.routineDelete.type) {
    override suspend fun apply(item: OutboxItem) {
        apiService.deleteRoutine(item.localId)
    }
}

class RoutineOccurrenceOverrideMutationHandler(
    private val apiService: ApiService,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
) : DecodingOfflineMutationHandler<RoutineOccurrenceOverrideRequest>(
    MedicalOfflineMutations.routineOccurrenceOverrideUpsert,
) {
    override suspend fun handle(item: OutboxItem, payload: RoutineOccurrenceOverrideRequest) {
        val serverResult = apiService.upsertRoutineOccurrenceOverride(payload)
        routineOccurrenceOverrideLocal.deleteById(item.localId)
        routineOccurrenceOverrideLocal.insert(serverResult)
    }
}

class MedicationLogMutationHandler(
    private val apiService: ApiService,
    private val medicationLogLocal: MedicationLogLocalDataSource,
) : DecodingOfflineMutationHandler<MedicationLogRequest>(
    MedicalOfflineMutations.medicationLog,
) {
    override suspend fun handle(item: OutboxItem, payload: MedicationLogRequest) {
        val serverResult = apiService.logMedication(payload)
        medicationLogLocal.deleteById(item.localId)
        medicationLogLocal.insert(serverResult)
    }
}

class MoodMutationHandler(private val apiService: ApiService, private val moodLocal: MoodLocalDataSource) :
    DecodingOfflineMutationHandler<MoodEntryRequest>(MedicalOfflineMutations.mood) {
    override suspend fun handle(item: OutboxItem, payload: MoodEntryRequest) {
        val serverResult = apiService.logMood(payload)
        moodLocal.deleteById(item.localId)
        moodLocal.insert(serverResult)
    }
}
