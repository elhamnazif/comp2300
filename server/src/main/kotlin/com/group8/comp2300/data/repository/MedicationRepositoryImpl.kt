package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MedicationEntity
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.formatMedicationStock
import com.group8.comp2300.domain.model.medical.parseLegacyMedicationAmount
import com.group8.comp2300.domain.model.medical.parseLegacyMedicationStock
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
            dose_amount = medication.doseAmount,
            dose_unit = medication.doseUnit.name,
            custom_dose_unit = medication.customDoseUnit,
            quantity = formatMedicationStock(medication.stockAmount, medication.stockUnit, medication.customStockUnit),
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
            dosage = medication.dosage,
            dose_amount = medication.doseAmount,
            dose_unit = medication.doseUnit.name,
            custom_dose_unit = medication.customDoseUnit,
            quantity = formatMedicationStock(medication.stockAmount, medication.stockUnit, medication.customStockUnit),
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

private fun MedicationEntity.toDomain(): Medication {
    val legacyDose = parseLegacyMedicationAmount(dosage)
    val parsedDoseUnit = dose_unit?.let { runCatching { MedicationUnit.valueOf(it) }.getOrNull() }
    val doseUnitValue = parsedDoseUnit ?: legacyDose.unit
    val legacyStock = parseLegacyMedicationStock(quantity)
    val parsedStockUnit = stock_unit?.let { runCatching { MedicationUnit.valueOf(it) }.getOrNull() }
    val stockUnitValue = parsedStockUnit ?: legacyStock.unit

    return Medication(
        id = id,
        userId = user_id,
        name = med_name,
        doseAmount = dose_amount?.trim().takeUnless { it.isNullOrBlank() } ?: legacyDose.amount,
        doseUnit = doseUnitValue,
        customDoseUnit = when (doseUnitValue) {
            MedicationUnit.OTHER -> custom_dose_unit?.trim().takeUnless { it.isNullOrBlank() } ?: legacyDose.customUnit
            else -> null
        },
        stockAmount = stock_amount?.trim().takeUnless { it.isNullOrBlank() } ?: legacyStock.amount,
        stockUnit = stockUnitValue,
        customStockUnit = when (stockUnitValue) {
            MedicationUnit.OTHER -> custom_stock_unit?.trim().takeUnless { it.isNullOrBlank() } ?: legacyStock.customUnit
            else -> null
        },
        instruction = instruction,
        colorHex = color_hex,
        status = MedicationStatus.valueOf(status),
    )
}
