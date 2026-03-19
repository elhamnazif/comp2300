package com.group8.comp2300.data.offline

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.auth.TokenManager
import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.MoodLocalDataSource
import com.group8.comp2300.data.local.OutboxDataSource
import com.group8.comp2300.data.local.OutboxItem
import com.group8.comp2300.data.local.OutboxState
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.RoutineCreateRequest
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest
import com.group8.comp2300.domain.repository.medical.SyncCoordinator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class SyncCoordinatorImpl(
    private val tokenManager: TokenManager,
    private val outbox: OutboxDataSource,
    private val apiService: ApiService,
    private val appointmentLocal: AppointmentLocalDataSource,
    private val medicationLocal: MedicationLocalDataSource,
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val moodLocal: MoodLocalDataSource,
) : SyncCoordinator {
    private val logger = Logger.withTag("SyncCoordinator")
    private val mutex = Mutex()

    override suspend fun flushOutbox() {
        if (tokenManager.getUserId() == null || tokenManager.isTokenExpired()) return

        mutex.withLock {
            outbox.getPending().forEach { item ->
                if (item.retryCount >= MAX_RETRIES) {
                    outbox.updateState(item.id, OutboxState.FAILED, "Retry limit reached")
                    return@forEach
                }

                try {
                    processItem(item)
                    outbox.delete(item.id)
                } catch (e: Exception) {
                    if (e.isRetryable()) {
                        outbox.incrementRetry(item.id)
                        logger.w(e) { "Retryable outbox failure for ${item.entityType}" }
                    } else {
                        outbox.updateState(item.id, OutboxState.FAILED, e.message)
                        logger.e(e) { "Non-retryable outbox failure for ${item.entityType}" }
                    }
                }
            }
        }
    }

    override suspend fun refreshAuthenticatedData() {
        if (tokenManager.getUserId() == null || tokenManager.isTokenExpired()) return

        val remoteMedications = apiService.getUserMedications()
        val remoteRoutines = apiService.getUserRoutines()
        val remoteOverrides = apiService.getRoutineOccurrenceOverrides()
        val remoteLogs = apiService.getMedicationLogHistory()
        val remoteMoods = apiService.getMoodHistory()
        val remoteAppointments = apiService.getAppointments()

        medicationLocal.replaceAll(remoteMedications)
        routineLocal.replaceAll(remoteRoutines)
        routineOccurrenceOverrideLocal.replaceAll(remoteOverrides)
        medicationLogLocal.replaceAll(remoteLogs)
        moodLocal.replaceAll(remoteMoods)
        appointmentLocal.replaceAll(remoteAppointments)
    }

    private suspend fun processItem(item: OutboxItem) {
        when (item.entityType) {
            OutboxEntityType.APPOINTMENT -> {
                val request = Json.decodeFromString<AppointmentRequest>(item.payload)
                val serverResult = apiService.scheduleAppointment(request)
                appointmentLocal.deleteById(item.localId)
                appointmentLocal.insert(serverResult)
            }

            OutboxEntityType.MEDICATION_UPSERT -> {
                val request = Json.decodeFromString<MedicationCreateRequest>(item.payload)
                val serverResult = apiService.upsertMedication(item.localId, request)
                medicationLocal.insert(serverResult)
            }

            OutboxEntityType.MEDICATION_DELETE -> {
                apiService.deleteMedication(item.localId)
            }

            OutboxEntityType.ROUTINE_UPSERT -> {
                val request = Json.decodeFromString<RoutineCreateRequest>(item.payload)
                val serverResult = apiService.upsertRoutine(item.localId, request)
                routineLocal.insert(serverResult)
            }

            OutboxEntityType.ROUTINE_DELETE -> {
                apiService.deleteRoutine(item.localId)
            }

            OutboxEntityType.ROUTINE_OCCURRENCE_OVERRIDE_UPSERT -> {
                val request = Json.decodeFromString<RoutineOccurrenceOverrideRequest>(item.payload)
                val serverResult = apiService.upsertRoutineOccurrenceOverride(request)
                routineOccurrenceOverrideLocal.deleteById(item.localId)
                routineOccurrenceOverrideLocal.insert(serverResult)
            }

            OutboxEntityType.MEDICATION_LOG -> {
                val request = Json.decodeFromString<MedicationLogRequest>(item.payload)
                val serverResult = apiService.logMedication(request)
                medicationLogLocal.deleteById(item.localId)
                medicationLogLocal.insert(serverResult)
            }

            OutboxEntityType.MOOD -> {
                val request = Json.decodeFromString<MoodEntryRequest>(item.payload)
                val serverResult = apiService.logMood(request)
                moodLocal.deleteById(item.localId)
                moodLocal.insert(serverResult)
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 5
    }
}
