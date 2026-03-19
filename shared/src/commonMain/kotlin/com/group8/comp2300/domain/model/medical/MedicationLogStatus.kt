package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationLogStatus(val displayName: String) {
    TAKEN("Taken"),
    SKIPPED("Skipped"),
    MISSED("Missed"),
    SNOOZED("Snoozed"),
    PENDING("Pending"),
}
