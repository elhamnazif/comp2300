package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

/**
 * Medication model for tracking user medications. Note: colorHex is used instead of Compose Color
 * for server compatibility.
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
    val colorHex: String? = null,
    val startDate: String, // YYYY-MM-DD
    val endDate: String, // YYYY-MM-DD
    val hasReminder: Boolean = true,
    val status: MedicationStatus = MedicationStatus.ACTIVE,
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
                "#78909C", // Blue Grey
            )
    }
}

// --- Composite "Grouped" Class ---

/**
 * Represents a Medication along with its full weekly schedule.
 * Use this for the "Medication Details" screen.
 */
data class MedicationWithSchedules(val medication: Medication, val schedules: List<MedicationSchedule>)
