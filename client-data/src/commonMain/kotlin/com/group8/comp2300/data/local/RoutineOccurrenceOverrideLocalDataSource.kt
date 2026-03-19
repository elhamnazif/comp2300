package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.RoutineOccurrenceOverride

class RoutineOccurrenceOverrideLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<RoutineOccurrenceOverride> =
        database.appDatabaseQueries.selectAllRoutineOccurrenceOverrides()
            .executeAsList()
            .map { entity ->
                RoutineOccurrenceOverride(
                    id = entity.id,
                    routineId = entity.routineId,
                    originalOccurrenceTimeMs = entity.originalOccurrenceTimeMs,
                    rescheduledOccurrenceTimeMs = entity.rescheduledOccurrenceTimeMs,
                )
            }

    fun insert(override: RoutineOccurrenceOverride) {
        database.appDatabaseQueries.insertRoutineOccurrenceOverride(
            id = override.id,
            routineId = override.routineId,
            originalOccurrenceTimeMs = override.originalOccurrenceTimeMs,
            rescheduledOccurrenceTimeMs = override.rescheduledOccurrenceTimeMs,
        )
    }

    fun replaceAll(overrides: List<RoutineOccurrenceOverride>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllRoutineOccurrenceOverrides()
            overrides.forEach(::insert)
        }
    }

    fun deleteById(id: String) {
        database.appDatabaseQueries.deleteRoutineOccurrenceOverrideById(id)
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllRoutineOccurrenceOverrides()
    }
}
