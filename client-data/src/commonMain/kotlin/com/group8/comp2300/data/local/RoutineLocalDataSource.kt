package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.database.RoutineEntity
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus

class RoutineLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<Routine> = database.appDatabaseQueries.selectAllRoutines()
        .executeAsList()
        .map(::toDomain)
        .sortedByRoutineTime()

    fun getById(id: String): Routine? = database.appDatabaseQueries.selectRoutineById(id)
        .executeAsOneOrNull()
        ?.let(::toDomain)

    fun insert(routine: Routine) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.insertRoutine(
                id = routine.id,
                userId = routine.userId,
                routineName = routine.name,
                repeatType = routine.repeatType.name,
                daysOfWeek = routine.daysOfWeek.joinToString(","),
                startDate = routine.startDate,
                endDate = routine.endDate,
                hasReminder = if (routine.hasReminder) 1L else 0L,
                status = routine.status.name,
            )
            database.appDatabaseQueries.deleteRoutineTimesByRoutineId(routine.id)
            routine.timesOfDayMs.sorted().distinct().forEach { timeOfDayMs ->
                database.appDatabaseQueries.insertRoutineTime(
                    id = "${routine.id}:$timeOfDayMs",
                    routineId = routine.id,
                    timeOfDayMs = timeOfDayMs,
                )
            }
            database.appDatabaseQueries.deleteRoutineMedicationLinksByRoutineId(routine.id)
            routine.medicationIds.distinct().forEach { medicationId ->
                database.appDatabaseQueries.insertRoutineMedicationLink(
                    id = "${routine.id}:$medicationId",
                    routineId = routine.id,
                    medicationId = medicationId,
                )
            }
            database.appDatabaseQueries.deleteRoutineReminderOffsetsByRoutineId(routine.id)
            routine.reminderOffsetsMins.sorted().distinct().forEach { offset ->
                database.appDatabaseQueries.insertRoutineReminderOffset(
                    id = "${routine.id}:$offset",
                    routineId = routine.id,
                    offsetMins = offset.toLong(),
                )
            }
        }
    }

    fun replaceAll(routines: List<Routine>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllRoutineTimes()
            database.appDatabaseQueries.deleteAllRoutineReminderOffsets()
            database.appDatabaseQueries.deleteAllRoutineMedicationLinks()
            database.appDatabaseQueries.deleteAllRoutines()
            routines.forEach(::insert)
        }
    }

    fun deleteById(id: String) {
        database.appDatabaseQueries.deleteRoutineTimesByRoutineId(id)
        database.appDatabaseQueries.deleteRoutineReminderOffsetsByRoutineId(id)
        database.appDatabaseQueries.deleteRoutineMedicationLinksByRoutineId(id)
        database.appDatabaseQueries.deleteRoutineById(id)
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllRoutineTimes()
        database.appDatabaseQueries.deleteAllRoutineReminderOffsets()
        database.appDatabaseQueries.deleteAllRoutineMedicationLinks()
        database.appDatabaseQueries.deleteAllRoutines()
    }

    private fun toDomain(entity: RoutineEntity): Routine = Routine(
        id = entity.id,
        userId = entity.userId,
        name = entity.routineName,
        timesOfDayMs =
        database.appDatabaseQueries.selectRoutineTimesByRoutineId(entity.id)
            .executeAsList()
            .map { it.timeOfDayMs },
        repeatType = RoutineRepeatType.valueOf(entity.repeatType),
        daysOfWeek = entity.daysOfWeek.split(',').mapNotNull { it.toIntOrNull() },
        startDate = entity.startDate,
        endDate = entity.endDate,
        hasReminder = entity.hasReminder == 1L,
        reminderOffsetsMins =
        database.appDatabaseQueries.selectRoutineReminderOffsetsByRoutineId(entity.id)
            .executeAsList()
            .map { it.offsetMins.toInt() },
        status = RoutineStatus.valueOf(entity.status),
        medicationIds =
        database.appDatabaseQueries.selectRoutineMedicationLinksByRoutineId(entity.id)
            .executeAsList()
            .map { it.medicationId },
    ).normalized()
}

private fun Routine.normalized(): Routine = copy(timesOfDayMs = timesOfDayMs.sorted().distinct())

private fun List<Routine>.sortedByRoutineTime(): List<Routine> =
    sortedWith(compareBy<Routine>({ it.timesOfDayMs.firstOrNull() ?: Long.MAX_VALUE }, { it.name }))
