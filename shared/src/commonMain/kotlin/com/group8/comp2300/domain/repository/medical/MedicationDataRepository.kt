package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationCreateRequest

interface MedicationDataRepository {
    suspend fun getMedications(): List<Medication>

    suspend fun saveMedication(request: MedicationCreateRequest, id: String? = null): Medication

    suspend fun deleteMedication(id: String)
}
