package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.database.MedicationEntity
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus

class MedicationLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<Medication> =
        database.appDatabaseQueries.selectAllMedications()
            .executeAsList()
            .map(MedicationEntity::toDomain)

    fun getById(id: String): Medication? =
        database.appDatabaseQueries.selectMedicationById(id)
            .executeAsOneOrNull()
            ?.toDomain()

    fun insert(medication: Medication) {
        database.appDatabaseQueries.insertMedication(
            id = medication.id,
            userId = medication.userId,
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

    fun replaceAll(medications: List<Medication>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllMedications()
            medications.forEach(::insert)
        }
    }

    fun deleteById(id: String) {
        database.appDatabaseQueries.deleteMedicationById(id)
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllMedications()
    }
}

private fun MedicationEntity.toDomain() = Medication(
    id = id,
    userId = userId,
    name = med_name,
    dosage = dosage,
    quantity = quantity,
    frequency = MedicationFrequency.valueOf(frequency),
    instruction = instruction,
    colorHex = color_hex,
    startDate = start_date,
    endDate = end_date,
    hasReminder = has_reminder == 1L,
    status = MedicationStatus.valueOf(status),
)
