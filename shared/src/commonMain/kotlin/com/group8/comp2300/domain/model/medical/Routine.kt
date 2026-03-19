package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Routine(
    val id: String,
    val userId: String,
    val name: String,
    val timesOfDayMs: List<Long> = emptyList(),
    val repeatType: RoutineRepeatType,
    val daysOfWeek: List<Int> = emptyList(),
    val startDate: String,
    val endDate: String? = null,
    val hasReminder: Boolean = true,
    val reminderOffsetsMins: List<Int> = emptyList(),
    val status: RoutineStatus = RoutineStatus.ACTIVE,
    val medicationIds: List<String> = emptyList(),
)

@Serializable
data class RoutineMedicationLink(
    val id: String,
    val routineId: String,
    val medicationId: String,
)
