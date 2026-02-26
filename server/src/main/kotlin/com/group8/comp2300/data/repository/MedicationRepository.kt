package com.group8.comp2300.data.repository

import com.group8.comp2300.database.MedicationEntity
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus



class MedicationRepository(private val database: ServerDatabase) {

    fun getAllByUserId(userId: String): List<Medication> =
        database.serverDatabaseQueries.selectAllMedsByUserId(userId).executeAsList().map { it.toDomain() }

    fun getById(id: String): Medication? =
        database.serverDatabaseQueries.selectMedById(id).executeAsOneOrNull()?.toDomain()

    fun insert(medication: Medication) {
        database.serverDatabaseQueries.insertMedication(
            id = medication.id,
            user_id = medication.userId,
            med_name = medication.name,
            dosage = medication.dosage.toLong(),
            frequency = medication.frequency.name,
            instruction = medication.instruction,
            colour_id = medication.colourId,
            start_date = medication.startDate,
            end_date = medication.endDate,
            reminders_enabled = if (medication.remindersEnabled) 1L else 0L,
            status = medication.status.name
        )
    }

    fun update(medication: Medication) {
        database.serverDatabaseQueries.updateMedById(
            id = medication.id,
            med_name = medication.name,
            dosage = medication.dosage.toLong(),
            frequency = medication.frequency.name,
            instruction = medication.instruction,
            colour_id = medication.colourId,
            start_date = medication.startDate,
            end_date = medication.endDate,
            reminders_enabled = if (medication.remindersEnabled) 1L else 0L,
            status = medication.status.name
        )
    }
}

private fun MedicationEntity.toDomain() = Medication(
    //Right column matches with database
    id = id,
    userId = user_id,
    name = med_name,
    dosage = dosage.toInt(),
    frequency = MedicationFrequency.valueOf(frequency),
    instruction = instruction,
    colourId = colour_id,
    startDate = start_date,
    endDate = end_date,
    remindersEnabled = reminders_enabled == 1L,
    status = MedicationStatus.valueOf(status)
)





