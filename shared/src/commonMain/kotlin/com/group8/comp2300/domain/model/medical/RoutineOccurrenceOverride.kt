package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class RoutineOccurrenceOverride(
    val id: String,
    val routineId: String,
    val originalOccurrenceTimeMs: Long,
    val rescheduledOccurrenceTimeMs: Long,
)

@Serializable
data class RoutineOccurrenceOverrideRequest(
    val routineId: String,
    val originalOccurrenceTimeMs: Long,
    val rescheduledOccurrenceTimeMs: Long,
)
