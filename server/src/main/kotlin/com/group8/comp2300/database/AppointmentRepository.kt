package com.group8.comp2300.database
interface AppointmentRepository {
    fun insertAppointment(appointment: Appointment)
    fun getAppointmentById(id: String): Appointment?
    fun updateAppointmentStatus(id: String, status: String)
}
