package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class AppointmentType(val displayName: String) {
    CHECKUP("Check-up"),
    CONSULTATION("Consultation"),
    SCREENING("Screening"),
    FOLLOW_UP("Follow-up"),
    VACCINATION("Vaccination"),
    OTHER("Other"),
}
