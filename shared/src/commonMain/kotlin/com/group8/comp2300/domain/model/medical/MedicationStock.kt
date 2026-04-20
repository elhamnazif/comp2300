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

    companion object {
        fun fromLegacyLabel(label: String): MedicationUnit? {
            return when (label.trim().lowercase()) {
                "tablet", "tablets", "tab", "tabs" -> TABLET
                "capsule", "capsules", "cap", "caps" -> CAPSULE
                "pill", "pills" -> PILL
                "puff", "puffs" -> PUFF
                "patch", "patches" -> PATCH
                "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> ML
                "mg", "milligram", "milligrams" -> MG
                else -> null
            }
        }
    }
}

typealias MedicationStockUnit = MedicationUnit

data class ParsedMedicationAmount(
    val amount: String,
    val unit: MedicationUnit,
    val customUnit: String? = null,
)

fun parseLegacyMedicationAmount(legacyValue: String?): ParsedMedicationAmount {
    val trimmed = legacyValue?.trim().orEmpty()
    if (trimmed.isBlank()) {
        return ParsedMedicationAmount(
            amount = "1",
            unit = MedicationUnit.OTHER,
            customUnit = "unit",
        )
    }

    val match = Regex("""^(\d+(?:\.\d+)?)\s+(.+)$""").matchEntire(trimmed)
    if (match != null) {
        val amount = match.groupValues[1]
        val rawUnit = match.groupValues[2].trim()
        val parsedUnit = MedicationUnit.fromLegacyLabel(rawUnit) ?: MedicationUnit.OTHER
        return ParsedMedicationAmount(
            amount = amount,
            unit = parsedUnit,
            customUnit = rawUnit.takeIf { parsedUnit == MedicationUnit.OTHER },
        )
    }

    return ParsedMedicationAmount(
        amount = "1",
        unit = MedicationUnit.OTHER,
        customUnit = trimmed,
    )
}

fun formatMedicationAmount(
    amount: String,
    unit: MedicationUnit,
    customUnit: String? = null,
): String {
    val normalizedAmount = amount.trim().ifBlank { "1" }
    val unitLabel = when (unit) {
        MedicationUnit.OTHER -> customUnit?.trim()?.takeIf(String::isNotBlank) ?: "unit"
        else -> unit.displayName
    }
    return "$normalizedAmount $unitLabel"
}

fun parseLegacyMedicationStock(legacyQuantity: String?): ParsedMedicationAmount = parseLegacyMedicationAmount(legacyQuantity)

fun formatMedicationStock(
    stockAmount: String,
    stockUnit: MedicationUnit,
    customStockUnit: String? = null,
): String = formatMedicationAmount(stockAmount, stockUnit, customStockUnit)

fun Medication.stockLabel(): String = formatMedicationAmount(stockAmount, stockUnit, customStockUnit)

fun Medication.doseLabel(): String = formatMedicationAmount(doseAmount, doseUnit, customDoseUnit)
