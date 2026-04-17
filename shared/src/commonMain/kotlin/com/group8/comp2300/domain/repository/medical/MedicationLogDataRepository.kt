package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.MedicationOccurrenceCandidate
import com.group8.comp2300.domain.model.medical.RoutineDayAgenda
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverrideRequest

interface MedicationLogDataRepository {
    suspend fun getRoutineAgenda(date: String): List<RoutineDayAgenda>

    suspend fun getRoutineAgendaRange(
        startDate: String,
        endDate: String,
    ): Map<String, List<RoutineDayAgenda>>

    suspend fun getManualMedicationLogs(date: String): List<MedicationLog>

    suspend fun getManualMedicationLogsRange(
        startDate: String,
        endDate: String,
    ): Map<String, List<MedicationLog>>

    suspend fun getMedicationOccurrenceCandidates(
        medicationId: String,
        timestampMs: Long,
    ): List<MedicationOccurrenceCandidate>

    suspend fun rescheduleRoutineOccurrence(request: RoutineOccurrenceOverrideRequest): RoutineOccurrenceOverride

    suspend fun logMedication(request: MedicationLogRequest): MedicationLog
}
