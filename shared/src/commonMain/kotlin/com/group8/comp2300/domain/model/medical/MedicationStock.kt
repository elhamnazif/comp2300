package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationUnit(val displayName: String) {
    TABLET("tablets"),
    CAPSULE("capsules"),
    PILL("pills"),
    PUFF("puffs"),
    PATCH("patches"),
    ML("mL"),
    MG("mg"),
    OTHER("Other"),
    ;
}

typealias MedicationStockUnit = MedicationUnit

fun formatMedicationAmount(amount: String, unit: MedicationUnit, customUnit: String? = null): String {
    val normalizedAmount = amount.trim().ifBlank { "1" }
    val unitLabel = when (unit) {
        MedicationUnit.OTHER -> customUnit?.trim()?.takeIf(String::isNotBlank) ?: "unit"
        else -> unit.displayName
    }
    return "$normalizedAmount $unitLabel"
}

fun Medication.stockLabel(): String = formatMedicationAmount(stockAmount, stockUnit, customStockUnit)

fun Medication.doseLabel(): String = formatMedicationAmount(doseAmount, doseUnit, customDoseUnit)
