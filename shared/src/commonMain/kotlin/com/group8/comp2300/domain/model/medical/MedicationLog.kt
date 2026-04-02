package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class MedicationLog(
    val id: String,
    val medicationId: String,
    val medicationTime: Long,
    val status: MedicationLogStatus,
    val routineId: String? = null,
    val occurrenceTimeMs: Long? = null,
    val medicationName: String? = null,
    val routineName: String? = null,
)
