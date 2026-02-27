package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Reminder

interface ReminderRepository {
    fun insert(reminder: Reminder)
    fun getByAppointmentId(appointmentId: String): List<Reminder>
    fun getByMedScheduleId(medScheduleId: String): List<Reminder>
    fun update(id: String, offsetMins: Int, isEnabled: Boolean)
    fun delete(id: String)
}
