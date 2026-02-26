package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class MedicationLog(
    val id: String,
    val medicationId: String,
    val medicationTime: Long, // Unix Epoch Milliseconds
    val status: MedicationLogStatus,
    val medicationName: String? = null // For agenda view joins
)