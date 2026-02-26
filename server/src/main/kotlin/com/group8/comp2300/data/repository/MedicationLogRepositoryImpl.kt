package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationLogEnt
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.repository.MedicationLogRepository

class MedicationLogRepositoryImpl(private val database: ServerDatabase) : MedicationLogRepository {

    private val queries = database.medicationLogQueries

    override fun insert(log: MedicationLog) {
        queries.insertMedLog(
            id = log.id,
            medication_id = log.medicationId,
            medication_time = log.medicationTime,
            status = log.status.name
        )
    }

    override fun getLogsByMedication(medicationId: String): List<MedicationLog> =
        queries.selectLogsByMedication(medicationId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: String): MedicationLog? =
        queries.selectLogDetails(id)
            .executeAsOneOrNull()?.toDomain()

    override fun getDailyAgenda(userId: String, dateString: String): List<MedicationLog> =
        queries.selectDailyMedicationAgenda(userId, dateString)
            .executeAsList()
            .map { row ->
                MedicationLog(
                    id = row.id,
                    medicationId = row.medication_id,
                    medicationTime = row.medication_time,
                    status = MedicationLogStatus.valueOf(row.status),
                    medicationName = row.med_name // From the JOIN
                )
            }

    override fun updateStatus(id: String, status: MedicationLogStatus) {
        queries.updateMedicationStatus(
            status = status.name,
            id = id
        )
    }

    override fun delete(id: String) {
        queries.deleteMedicationLog(id)
    }

    // Mapper for standard entity
    private fun MedicationLogEnt.toDomain() = MedicationLog(
        id = id,
        medicationId = medication_id,
        medicationTime = medication_time,
        status = MedicationLogStatus.valueOf(status)
    )
}