package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationLogEnt
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.repository.MedicationLogRepository

class MedicationLogRepositoryImpl(private val database: ServerDatabase) : MedicationLogRepository {

    override fun insert(log: MedicationLog) {
        database.medicationLogQueries.insertMedLog(
            id = log.id,
            medication_id = log.medicationId,
            medication_time = log.medicationTime,
            status = log.status.name,
        )
    }

    override fun getLogsByMedication(medicationId: String): List<MedicationLog> =
        database.medicationLogQueries.selectLogsByMedication(medicationId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: String): MedicationLog? = database.medicationLogQueries.selectLogDetails(id)
        .executeAsOneOrNull()?.toDomain()

    override fun getDailyAgenda(userId: String, dateString: String): List<MedicationLog> =
        database.medicationLogQueries.selectDailyMedicationAgenda(userId, dateString)
            .executeAsList()
            .map { row ->
                MedicationLog(
                    id = row.id,
                    medicationId = row.medication_id,
                    medicationTime = row.medication_time,
                    status = MedicationLogStatus.valueOf(row.status),
                    medicationName = row.med_name, // From the JOIN
                )
            }

    override fun updateStatus(id: String, status: MedicationLogStatus) {
        database.medicationLogQueries.updateMedicationStatus(
            status = status.name,
            id = id,
        )
    }

    override fun delete(id: String) {
        database.medicationLogQueries.deleteMedicationLog(id)
    }
}

private fun MedicationLogEnt.toDomain() = MedicationLog(
    id = id,
    medicationId = medication_id,
    medicationTime = medication_time,
    status = MedicationLogStatus.valueOf(status),
)
