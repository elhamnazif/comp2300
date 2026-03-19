package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.RoutineEnt
import com.group8.comp2300.domain.model.medical.Routine
import com.group8.comp2300.domain.model.medical.RoutineRepeatType
import com.group8.comp2300.domain.model.medical.RoutineStatus
import com.group8.comp2300.domain.repository.RoutineRepository

class RoutineRepositoryImpl(private val database: ServerDatabase) : RoutineRepository {
    override fun getAllByUserId(userId: String): List<Routine> =
        database.routineQueries.selectRoutinesByUserId(userId)
            .executeAsList()
            .map(::toDomain)
            .sortedByRoutineTime()

    override fun getById(id: String): Routine? =
        database.routineQueries.selectRoutineById(id)
            .executeAsOneOrNull()
            ?.let(::toDomain)

    override fun insert(routine: Routine) {
        database.routineQueries.transaction {
            database.routineQueries.insertRoutine(
                id = routine.id,
                user_id = routine.userId,
                routine_name = routine.name,
                repeat_type = routine.repeatType.name,
                days_of_week = routine.daysOfWeek.joinToString(","),
                start_date = routine.startDate,
                end_date = routine.endDate,
                has_reminder = if (routine.hasReminder) 1L else 0L,
                status = routine.status.name,
            )
            persistChildren(routine)
        }
    }

    override fun update(routine: Routine) {
        database.routineQueries.transaction {
            database.routineQueries.updateRoutineById(
                routine_name = routine.name,
                repeat_type = routine.repeatType.name,
                days_of_week = routine.daysOfWeek.joinToString(","),
                start_date = routine.startDate,
                end_date = routine.endDate,
                has_reminder = if (routine.hasReminder) 1L else 0L,
                status = routine.status.name,
                id = routine.id,
            )
            persistChildren(routine)
        }
    }

    override fun delete(id: String) {
        database.routineQueries.deleteRoutineById(id)
    }

    private fun persistChildren(routine: Routine) {
        database.routineTimeQueries.deleteRoutineTimesByRoutineId(routine.id)
        routine.timesOfDayMs.sorted().distinct().forEach { timeOfDayMs ->
            database.routineTimeQueries.insertRoutineTime(
                id = "${routine.id}:$timeOfDayMs",
                routine_id = routine.id,
                time_of_day_ms = timeOfDayMs,
            )
        }
        database.routineMedicationQueries.deleteRoutineMedicationsByRoutineId(routine.id)
        routine.medicationIds.distinct().forEach { medicationId ->
            database.routineMedicationQueries.insertRoutineMedication(
                id = "${routine.id}:$medicationId",
                routine_id = routine.id,
                medication_id = medicationId,
            )
        }
        database.routineReminderQueries.deleteRoutineRemindersByRoutineId(routine.id)
        routine.reminderOffsetsMins.sorted().distinct().forEach { offset ->
            database.routineReminderQueries.insertRoutineReminder(
                id = "${routine.id}:$offset",
                routine_id = routine.id,
                offset_mins = offset.toLong(),
            )
        }
    }

    private fun toDomain(entity: RoutineEnt): Routine = Routine(
        id = entity.id,
        userId = entity.user_id,
        name = entity.routine_name,
        timesOfDayMs =
        database.routineTimeQueries.selectRoutineTimes(entity.id)
            .executeAsList()
            .map { it.time_of_day_ms },
        repeatType = RoutineRepeatType.valueOf(entity.repeat_type),
        daysOfWeek = entity.days_of_week.split(',').mapNotNull { it.toIntOrNull() },
        startDate = entity.start_date,
        endDate = entity.end_date,
        hasReminder = entity.has_reminder == 1L,
        reminderOffsetsMins =
        database.routineReminderQueries.selectRoutineReminders(entity.id)
            .executeAsList()
            .map { it.offset_mins.toInt() },
        status = RoutineStatus.valueOf(entity.status),
        medicationIds =
        database.routineMedicationQueries.selectRoutineMedications(entity.id)
            .executeAsList()
            .map { it.medication_id },
    ).normalized()
}

private fun Routine.normalized(): Routine = copy(timesOfDayMs = timesOfDayMs.sorted().distinct())

private fun List<Routine>.sortedByRoutineTime(): List<Routine> =
    sortedWith(compareBy<Routine>({ it.timesOfDayMs.firstOrNull() ?: Long.MAX_VALUE }, { it.name }))
