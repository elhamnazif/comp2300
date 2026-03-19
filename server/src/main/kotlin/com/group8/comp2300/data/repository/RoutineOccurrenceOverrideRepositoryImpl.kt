package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.RoutineOccurrenceOverrideEnt
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride
import com.group8.comp2300.domain.repository.RoutineOccurrenceOverrideRepository

class RoutineOccurrenceOverrideRepositoryImpl(private val database: ServerDatabase) : RoutineOccurrenceOverrideRepository {
    override fun getAllByUserId(userId: String): List<RoutineOccurrenceOverride> =
        database.routineOccurrenceOverrideQueries.selectRoutineOccurrenceOverridesByUserId(userId)
            .executeAsList()
            .map(RoutineOccurrenceOverrideEnt::toDomain)

    override fun getByRoutineAndOriginal(routineId: String, originalOccurrenceTimeMs: Long): RoutineOccurrenceOverride? =
        database.routineOccurrenceOverrideQueries
            .selectRoutineOccurrenceOverrideByRoutineAndOriginal(routineId, originalOccurrenceTimeMs)
            .executeAsOneOrNull()
            ?.toDomain()

    override fun insert(override: RoutineOccurrenceOverride) {
        database.routineOccurrenceOverrideQueries.insertRoutineOccurrenceOverride(
            id = override.id,
            routine_id = override.routineId,
            original_occurrence_time_ms = override.originalOccurrenceTimeMs,
            rescheduled_occurrence_time_ms = override.rescheduledOccurrenceTimeMs,
        )
    }
}

private fun RoutineOccurrenceOverrideEnt.toDomain() = RoutineOccurrenceOverride(
    id = id,
    routineId = routine_id,
    originalOccurrenceTimeMs = original_occurrence_time_ms,
    rescheduledOccurrenceTimeMs = rescheduled_occurrence_time_ms,
)
