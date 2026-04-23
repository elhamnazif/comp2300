package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.database.MedicationEntity
import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationStatus
import com.group8.comp2300.domain.model.medical.MedicationUnit
import com.group8.comp2300.domain.model.medical.formatMedicationAmount
import com.group8.comp2300.domain.model.medical.parseLegacyMedicationAmount

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
            dose_amount = medication.doseAmount,
            dose_unit = medication.doseUnit.name,
            custom_dose_unit = medication.customDoseUnit,
            quantity = formatMedicationAmount(
                amount = medication.stockAmount,
                unit = medication.stockUnit,
                customUnit = medication.customStockUnit,
            ),
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

    private fun toDomain(entity: MedicationEntity): Medication {
        val legacyDose = parseLegacyMedicationAmount(entity.dosage)
        val parsedDoseUnit = entity.dose_unit?.let { runCatching { MedicationUnit.valueOf(it) }.getOrNull() }
        val doseUnitValue = parsedDoseUnit ?: legacyDose.unit
        val legacyStock = parseLegacyMedicationAmount(entity.quantity)
        val parsedStockUnit = entity.stock_unit?.let { runCatching { MedicationUnit.valueOf(it) }.getOrNull() }
        val stockUnitValue = parsedStockUnit ?: legacyStock.unit

        return Medication(
            id = entity.id,
            userId = entity.userId,
            name = entity.med_name,
            doseAmount = entity.dose_amount?.trim().takeUnless { it.isNullOrBlank() } ?: legacyDose.amount,
            doseUnit = doseUnitValue,
            customDoseUnit = when (doseUnitValue) {
                MedicationUnit.OTHER -> entity.custom_dose_unit?.trim().takeUnless { it.isNullOrBlank() }
                    ?: legacyDose.customUnit

                else -> null
            },
            stockAmount = entity.stock_amount?.trim().takeUnless { it.isNullOrBlank() } ?: legacyStock.amount,
            stockUnit = stockUnitValue,
            customStockUnit = when (stockUnitValue) {
                MedicationUnit.OTHER -> entity.custom_stock_unit?.trim().takeUnless { it.isNullOrBlank() }
                    ?: legacyStock.customUnit

                else -> null
            },
            instruction = entity.instruction,
            colorHex = entity.color_hex,
            status = MedicationStatus.valueOf(entity.status),
        )
    }
}
