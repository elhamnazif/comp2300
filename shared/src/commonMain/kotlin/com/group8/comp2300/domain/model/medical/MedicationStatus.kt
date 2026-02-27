package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationStatus(val displayName: String) {
    ACTIVE("Active"),
    ARCHIVED("Archived"),
}
