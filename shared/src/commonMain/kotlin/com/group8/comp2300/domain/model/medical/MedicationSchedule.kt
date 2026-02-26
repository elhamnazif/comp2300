package com.group8.comp2300.domain.model.medical

data class MedicationSchedule(
    val id: String,
    val medicationId: String,
    val dayOfWeek: Int,
    val timeOfDay: Long // Represented in milliseconds (e.g., 28,800,000 for 8:00 AM)
)