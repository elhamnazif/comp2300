package com.group8.comp2300.database

import com.group8.comp2300.database.Appointment
interface AppointmentRepository {
    fun insertAppointment(appointment: Appointment)
}