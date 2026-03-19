package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride

interface RoutineOccurrenceOverrideRepository {
    fun getAllByUserId(userId: String): List<RoutineOccurrenceOverride>

    fun getByRoutineAndOriginal(routineId: String, originalOccurrenceTimeMs: Long): RoutineOccurrenceOverride?

    fun insert(override: RoutineOccurrenceOverride)
}
