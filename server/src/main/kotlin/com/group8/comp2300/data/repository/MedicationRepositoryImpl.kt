package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationEntity
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.repository.MedicationRepository

class MedicationRepositoryImpl(private val database: ServerDatabase) : MedicationRepository {

    override fun getAllByUserId(userId: String): List<Medication> =
        database.medicationQueries.selectActiveArchivedMedsByUserId(userId).executeAsList().map { it.toDomain() }

    override fun getActiveByUserId(userId: String): List<Medication> =
        database.medicationQueries.selectActiveMedsByUserId(userId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getArchivedByUserId(userId: String): List<Medication> =
        database.medicationQueries.selectArchivedMedsByUserId(userId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getById(id: String): Medication? =
        database.medicationQueries.selectMedById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(medication: Medication) {
        database.medicationQueries.insertMedication(
            id = medication.id,
            user_id = medication.userId,
            med_name = medication.name,
            dosage = medication.dosage,
            quantity = medication.quantity,
            frequency = medication.frequency.name,
            instruction = medication.instruction,
            color_hex = medication.colorHex,
            start_date = medication.startDate,
            end_date = medication.endDate,
            has_reminder = if (medication.hasReminder) 1L else 0L,
            status = medication.status.name,
        )
    }

    override fun update(medication: Medication) {
        database.medicationQueries.updateMedById(
            med_name = medication.name,
            dosage = medication.dosage,
            quantity = medication.quantity,
            frequency = medication.frequency.name,
            instruction = medication.instruction,
            color_hex = medication.colorHex,
            start_date = medication.startDate,
            end_date = medication.endDate,
            has_reminder = if (medication.hasReminder) 1L else 0L,
            status = medication.status.name,
            id = medication.id,
        )
    }

    override fun delete(id: String) {
        database.medicationQueries.deleteMedById(id)
    }
}

private fun MedicationEntity.toDomain() = Medication(
    id = id,
    userId = user_id,
    name = med_name,
    dosage = dosage ?: "",
    quantity = quantity ?: "",
    frequency = MedicationFrequency.valueOf(frequency),
    instruction = instruction,
    colorHex = color_hex,
    startDate = start_date,
    endDate = end_date,
    hasReminder = has_reminder == 1L,
    status = MedicationStatus.valueOf(status),
)
