package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.offline.MedicalOfflineMutations
import com.group8.comp2300.data.offline.QueuedOfflineStore
import com.group8.comp2300.data.offline.QueuedWriteDispatcher
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.session.userOrNull
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.medical.MedicationDataRepository

class MedicationDataRepositoryImpl(
    private val authRepository: AuthRepository,
    private val medicationLocal: MedicationLocalDataSource,
    private val queuedWriteDispatcher: QueuedWriteDispatcher,
) : MedicationDataRepository {
    private val medicationWrites = QueuedOfflineStore(
        mutation = MedicalOfflineMutations.medicationUpsert,
        queuedWriteDispatcher = queuedWriteDispatcher,
        buildLocal = { medicationId, request ->
            Medication(
                id = medicationId,
                userId = authRepository.session.value.userOrNull?.id.orEmpty(),
                name = request.name,
                doseAmount = request.doseAmount,
                doseUnit = MedicationUnit.valueOf(request.doseUnit),
                customDoseUnit = request.customDoseUnit,
                stockAmount = request.stockAmount,
                stockUnit = MedicationUnit.valueOf(request.stockUnit),
                customStockUnit = request.customStockUnit,
                instruction = request.instruction,
                colorHex = request.colorHex,
                status = MedicationStatus.valueOf(request.status),
            )
        },
        saveLocal = medicationLocal::insert,
        readLocal = medicationLocal::getById,
    )

    override suspend fun getMedications(): List<Medication> = medicationLocal.getAll()

    override suspend fun saveMedication(request: MedicationCreateRequest, id: String?): Medication {
        val normalizedRequest =
            request.copy(
                name = request.name.trim(),
                doseAmount = request.doseAmount.trim(),
                customDoseUnit = request.customDoseUnit?.trim()?.takeIf(String::isNotEmpty),
                stockAmount = request.stockAmount.trim(),
                customStockUnit = request.customStockUnit?.trim()?.takeIf(String::isNotEmpty),
                instruction = request.instruction?.trim()?.takeIf(String::isNotEmpty),
            )
        return medicationWrites.write(normalizedRequest, id)
    }

    override suspend fun deleteMedication(id: String) {
        queuedWriteDispatcher.deletePending(MedicalOfflineMutations.medicationUpsert, id)
        medicationLocal.deleteById(id)
        if (authRepository.session.value.userOrNull != null) {
            queuedWriteDispatcher.replacePending(MedicalOfflineMutations.medicationDelete, id, Unit)
        }
    }
}
