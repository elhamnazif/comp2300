package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationFrequency(val displayName: String) {
    DAILY("Daily"),
    TWICE_DAILY("Twice Daily"),
    WEEKLY("Weekly"),
    ON_DEMAND("On Demand")
}
