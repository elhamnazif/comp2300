package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationEntity
import com.group8.comp2300.domain.model.medical.*
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
            dose_amount = medication.doseAmount,
            dose_unit = medication.doseUnit.name,
            custom_dose_unit = medication.customDoseUnit,
            stock_amount = medication.stockAmount,
            stock_unit = medication.stockUnit.name,
            custom_stock_unit = medication.customStockUnit,
            instruction = medication.instruction,
            color_hex = medication.colorHex,
            start_date = "",
            end_date = null,
            has_reminder = 0L,
            status = medication.status.name,
        )
    }

    override fun update(medication: Medication) {
        database.medicationQueries.updateMedById(
            med_name = medication.name,
            dose_amount = medication.doseAmount,
            dose_unit = medication.doseUnit.name,
            custom_dose_unit = medication.customDoseUnit,
            stock_amount = medication.stockAmount,
            stock_unit = medication.stockUnit.name,
            custom_stock_unit = medication.customStockUnit,
            instruction = medication.instruction,
            color_hex = medication.colorHex,
            start_date = "",
            end_date = null,
            has_reminder = 0L,
            status = medication.status.name,
            id = medication.id,
        )
    }

    override fun delete(id: String) {
        database.medicationQueries.deleteMedById(id)
    }
}

private fun MedicationEntity.toDomain(): Medication = Medication(
    id = id,
    userId = user_id,
    name = med_name,
    doseAmount = dose_amount,
    doseUnit = MedicationUnit.valueOf(dose_unit),
    customDoseUnit = custom_dose_unit?.trim()?.takeIf(String::isNotBlank),
    stockAmount = stock_amount,
    stockUnit = MedicationUnit.valueOf(stock_unit),
    customStockUnit = custom_stock_unit?.trim()?.takeIf(String::isNotBlank),
    instruction = instruction,
    colorHex = color_hex,
    status = MedicationStatus.valueOf(status),
)
