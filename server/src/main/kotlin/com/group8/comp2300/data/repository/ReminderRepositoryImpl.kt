package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.ReminderEntity
import com.group8.comp2300.domain.model.medical.Reminder
import com.group8.comp2300.domain.repository.ReminderRepository

class ReminderRepositoryImpl(private val database: ServerDatabase) : ReminderRepository {

    override fun insert(reminder: Reminder) {
        database.reminderQueries.insertReminder(
            id = reminder.id,
            user_id = reminder.userId,
            med_schedule_id = reminder.medScheduleId,
            appointment_id = reminder.appointmentId,
            offset_mins = reminder.offsetMins.toLong(),
            is_enabled = if (reminder.isEnabled) 1L else 0L,
        )
    }

    override fun getByAppointmentId(appointmentId: String): List<Reminder> =
        database.reminderQueries.selectAppointmentReminders(appointmentId)
            .executeAsList()
            .map { it.toDomain() }

    override fun getByMedScheduleId(medScheduleId: String): List<Reminder> =
        database.reminderQueries.selectMedicationReminders(medScheduleId)
            .executeAsList()
            .map { it.toDomain() }

    override fun update(id: String, offsetMins: Int, isEnabled: Boolean) {
        database.reminderQueries.updateReminderStatus(
            offset_mins = offsetMins.toLong(),
            is_enabled = if (isEnabled) 1L else 0L,
            id = id,
        )
    }

    override fun delete(id: String) {
        database.reminderQueries.deleteReminder(id)
    }
}

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    userId = user_id,
    medScheduleId = med_schedule_id,
    appointmentId = appointment_id,
    offsetMins = offset_mins.toInt(),
    isEnabled = is_enabled == 1L,
)
