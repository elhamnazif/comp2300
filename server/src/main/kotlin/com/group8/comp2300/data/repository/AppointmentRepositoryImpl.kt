package com.group8.comp2300.data.repository

import com.group8.comp2300.database.Appointment
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.AppointmentRepository

class AppointmentRepositoryImpl(private val database: ServerDatabase) : AppointmentRepository {

    override fun insertAppointment(appointment: Appointment) {
        database.serverDatabaseQueries.insertAppointment(
            id = appointment.id,
            user_id = appointment.user_id,
            title = appointment.title,
            appointment_time = appointment.appointment_time,
            appointment_type = appointment.appointment_type,
            clinic_id = appointment.clinic_id,
            booking_id = appointment.booking_id,
            status = appointment.status,
            notes = appointment.notes,
            reminders_enabled = appointment.reminders_enabled,
            payment_method = appointment.payment_method,
            payment_status = appointment.payment_status,
            payment_amount = appointment.payment_amount,
            transaction_id = appointment.transaction_id
        )
    }

    override fun getAppointmentById(id: String): Appointment? =
        database.serverDatabaseQueries.selectAppointmentById(id).executeAsOneOrNull()

    override fun updateAppointmentStatus(id: String, status: String) {
        database.serverDatabaseQueries.updateAppointmentStatus(status, id)
    }

    override fun updatePaymentDetails(
        id: String,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String?
    ) {
        database.serverDatabaseQueries.updatePaymentDetails(
            payment_method = paymentMethod,
            payment_status = paymentStatus,
            transaction_id = transactionId,
            id = id
        )
    }

    override fun getAppointmentsByUserAndPaymentStatus(userId: String, paymentStatus: String): List<Appointment> =
        database.serverDatabaseQueries.selectByUserAndPaymentStatus(userId, paymentStatus).executeAsList()
}
