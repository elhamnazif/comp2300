package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Appointment

interface AppointmentRepository {

    /**
     * Persists a new appointment or updates an existing one using the domain model.
     */
    fun insertAppointment(appointment: Appointment)

    /**
     * Retrieves a specific appointment by its unique identifier.
     */
    fun getAppointmentById(id: String): Appointment?

    /**
     * Fetches all appointments for a user that are in a 'CONFIRMED' state.
     */
    fun getConfirmedByUserId(userId: String): List<Appointment>

    /**
     * Updates the status of an appointment (e.g., 'CANCELLED', 'COMPLETED').
     */
    fun updateAppointmentStatus(id: String, status: String)

    /**
     * Updates financial tracking information for a specific appointment.
     */
    fun updatePaymentDetails(
        id: String,
        paymentMethod: String,
        paymentStatus: String,
        transactionId: String?
    )

    /**
     * Filters appointments based on their payment state (e.g., 'PAID', 'PENDING').
     */
    fun getByPaymentStatus(userId: String, paymentStatus: String): List<Appointment>

    fun delete(id: String)
}