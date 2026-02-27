package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class LabStatus(val displayName: String) {
    PENDING("Pending"),
    NEGATIVE("Negative"),
    POSITIVE("Positive"),
    INCONCLUSIVE("Inconclusive"),
}
