package com.group8.comp2300.domain.model.medical

enum class MedicationLogStatus (val displayName: String) {
    TAKEN ("Taken"),
    SKIPPED("Skipped"),
    SNOOZED("Snoozed"),
    PENDING("Pending")
}

