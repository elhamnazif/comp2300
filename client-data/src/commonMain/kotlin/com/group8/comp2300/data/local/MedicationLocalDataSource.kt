package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.database.MedicationEntity
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus

class MedicationLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<Medication> = database.appDatabaseQueries.selectAllMedications()
        .executeAsList()
        .map(::toDomain)

    fun getById(id: String): Medication? = database.appDatabaseQueries.selectMedicationById(id)
        .executeAsOneOrNull()
        ?.let(::toDomain)

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
            start_date = "",
            end_date = null,
            has_reminder = 0L,
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

    private fun toDomain(entity: MedicationEntity): Medication = Medication(
        id = entity.id,
        userId = entity.userId,
        name = entity.med_name,
        dosage = entity.dosage,
        quantity = entity.quantity,
        frequency = MedicationFrequency.valueOf(entity.frequency),
        instruction = entity.instruction,
        colorHex = entity.color_hex,
        status = MedicationStatus.valueOf(entity.status),
    )
}
