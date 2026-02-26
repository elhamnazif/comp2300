package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

/**
 * Medication model for tracking user medications. Note: colorHex is used instead of Compose Color
 * for server compatibility.
 */

/**
 * Old model excluded for now
@Serializable
data class Medication(
val id: String,
val userId: String? = null,
val name: String,
val dosage: String,
val frequency: MedicationFrequency,
val instructions: String = "",
val colorHex: String = "#42A5F5",
val status: MedicationStatus = MedicationStatus.ACTIVE,
val createdAt: Long = 0L,
val updatedAt: Long = createdAt
) {
companion object {
val PRESET_COLORS =
listOf(
"#42A5F5", // Blue
"#EF5350", // Red
"#66BB6A", // Green
"#FFA726", // Orange
"#AB47BC", // Purple
"#26C6DA", // Cyan
"#78909C" // Blue Grey
)
}
}
 */


/**
 * Temporarily store all medical data class in this file.
 * To be updated.
 */

@Serializable
data class Medication(
    val id: String,
    val userId: String,
    val name: String,
    val dosage: String, // Number of pills
    val quantity: String, // Units in weight
    val frequency: MedicationFrequency,
    val instruction: String? = null,
    val colourHex: String? = null,
    val startDate: String, // YYYY-MM-DD
    val endDate: String, // YYYY-MM-DD
    val hasReminder: Boolean = true,
    val status: MedicationStatus = MedicationStatus.ACTIVE
)



// --- Composite "Grouped" Class ---

/**
 * Represents a Medication along with its full weekly schedule.
 * Use this for the "Medication Details" screen.
 */
data class MedicationWithSchedules(
    val medication: Medication,
    val schedules: List<MedicationSchedule>
)

