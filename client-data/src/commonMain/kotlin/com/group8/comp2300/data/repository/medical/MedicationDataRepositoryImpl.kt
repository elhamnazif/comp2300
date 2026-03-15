package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.offline.OutboxEntityType
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class MedicationDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val medicationLocal: MedicationLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MedicationDataRepository {
    override suspend fun getMedications(): List<Medication> = medicationLocal.getAll()

    override suspend fun saveMedication(request: MedicationCreateRequest, id: String?): Medication {
        val medicationId = id ?: Uuid.random().toString()
        val medication = Medication(
            id = medicationId,
            userId = authRepository.session.value.userOrNull?.id.orEmpty(),
            name = request.name.trim(),
            dosage = request.dosage.trim(),
            quantity = request.quantity.trim(),
            frequency = MedicationFrequency.valueOf(request.frequency),
            instruction = request.instruction?.trim()?.takeIf(String::isNotEmpty),
            colorHex = request.colorHex,
            startDate = request.startDate,
            endDate = request.endDate,
            hasReminder = request.hasReminder,
            status = MedicationStatus.valueOf(request.status),
        )

        medicationLocal.insert(medication)
        queuedWriteDispatcher.replacePending(
            entityType = OutboxEntityType.MEDICATION_UPSERT,
            localId = medicationId,
            payload = Json.encodeToString(request),
        )
        return medicationLocal.getById(medicationId) ?: medication
    }

    override suspend fun deleteMedication(id: String) {
        queuedWriteDispatcher.deletePending(OutboxEntityType.MEDICATION_UPSERT, id)
        medicationLocal.deleteById(id)
        if (authRepository.session.value.userOrNull != null) {
            queuedWriteDispatcher.replacePending(
                entityType = OutboxEntityType.MEDICATION_DELETE,
                localId = id,
                payload = "",
            )
        }
    }
}
