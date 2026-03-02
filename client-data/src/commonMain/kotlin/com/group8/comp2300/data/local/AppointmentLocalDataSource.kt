package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.Appointment

class AppointmentLocalDataSource(private val database: AppDatabase) {

    fun getAll(): List<Appointment> =
        database.appDatabaseQueries.selectAllAppointments().executeAsList().map { entity ->
            Appointment(
                id = entity.id,
                userId = entity.userId,
                title = entity.title,
                appointmentTime = entity.appointmentTime,
                appointmentType = entity.appointmentType,
                clinicId = entity.clinicId,
                bookingId = entity.bookingId,
                status = entity.status,
                notes = entity.notes,
                hasReminder = entity.hasReminder != 0L,
                paymentStatus = entity.paymentStatus,
            )
        }

    fun insert(appointment: Appointment) {
        database.appDatabaseQueries.insertAppointment(
            id = appointment.id,
            userId = appointment.userId,
            title = appointment.title,
            appointmentTime = appointment.appointmentTime,
            appointmentType = appointment.appointmentType,
            clinicId = appointment.clinicId,
            bookingId = appointment.bookingId,
            status = appointment.status,
            notes = appointment.notes,
            hasReminder = if (appointment.hasReminder) 1L else 0L,
            paymentStatus = appointment.paymentStatus,
        )
    }

    fun replaceAll(appointments: List<Appointment>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllAppointments()
            appointments.forEach { insert(it) }
        }
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllAppointments()
    }
}
