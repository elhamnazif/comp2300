package com.group8.comp2300.domain.model.reminder

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderType(val displayName: String) {
    MEDICATION("Medication"),
    APPOINTMENT("Appointment"),
    SCREENING("Screening"),
    CUSTOM("Custom")
}
