package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest

interface MedicationLogDataRepository {
    suspend fun getMedicationAgenda(date: String): List<MedicationLog>

    suspend fun logMedication(request: MedicationLogRequest): MedicationLog
}
