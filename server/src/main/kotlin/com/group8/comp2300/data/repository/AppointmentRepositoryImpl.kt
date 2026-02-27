package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.AppointmentEntity
import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.repository.AppointmentRepository

class AppointmentRepositoryImpl(private val database: ServerDatabase) : AppointmentRepository {

    override fun insertAppointment(appointment: Appointment) {
        database.appointmentQueries.insertAppointment(
            id = appointment.id,
            user_id = appointment.userId,
            title = appointment.title,
            appointment_time = appointment.appointmentTime,
            appointment_type = appointment.appointmentType,
            clinic_id = appointment.clinicId,
            booking_id = null,
            status = appointment.status,
            notes = appointment.notes,
            has_reminder = if (appointment.hasReminder) 1L else 0L,
            payment_method = appointment.paymentMethod,
            payment_status = appointment.paymentStatus,
            payment_amount = appointment.paymentAmount,
            transaction_id = appointment.transactionId,
        )
    }

    override fun getAppointmentById(id: String): Appointment? = database.appointmentQueries.selectAppointmentById(id)
        .executeAsOneOrNull()?.toDomain()

    override fun getConfirmedByUserId(userId: String): List<Appointment> =
        database.appointmentQueries.selectConfirmedApptsOfUser(userId)
            .executeAsList()
            .map { row ->

                Appointment(
                    id = row.id,
                    userId = row.user_id,
                    title = row.title,
                    appointmentTime = row.appointment_time,
                    appointmentType = row.appointment_type,
                    clinicId = row.clinic_id,
                    bookingId = row.booking_id,
                    status = row.status ?: "CONFIRMED",
                    notes = row.notes,
                    hasReminder = row.has_reminder == 1L,
                    paymentStatus = row.payment_status ?: "PENDING",
                    paymentMethod = row.payment_method,
                    paymentAmount = row.payment_amount,
                    transactionId = row.transaction_id,
                )
            }

    override fun updateAppointmentStatus(id: String, status: String) {
        database.appointmentQueries.updateAppointmentStatus(
            status = status,
            id = id,
        )
    }

    override fun updatePaymentDetails(
        id: String,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String?,
    ) {
        database.appointmentQueries.updatePaymentDetails(
            payment_method = paymentMethod,
            payment_status = paymentStatus,
            transaction_id = transactionId,
            id = id,
        )
    }

    override fun getByPaymentStatus(userId: String, paymentStatus: String): List<Appointment> =
        database.appointmentQueries.selectByUserAndPaymentStatus(userId, paymentStatus)
            .executeAsList()
            .map { it.toDomain() }

    override fun delete(id: String) {
        database.appointmentQueries.deleteAppointment(id)
    }
}

private fun AppointmentEntity.toDomain() = Appointment(
    id = id,
    userId = user_id,
    title = title,
    appointmentTime = appointment_time,
    appointmentType = appointment_type,
    clinicId = clinic_id,
    bookingId = booking_id,
    status = status ?: "CONFIRMED",
    notes = notes,
    hasReminder = has_reminder == 1L,
    paymentStatus = payment_status ?: "PENDING",
    paymentMethod = payment_method,
    paymentAmount = payment_amount,
    transactionId = transaction_id,
)
