package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationStatus {
    ACTIVE,
    ARCHIVED,
}
