package com.group8.comp2300.database

interface AppointmentRepository {
    fun insertAppointment(appointment: Appointment)
    fun getAppointmentById(id: String): Appointment?
    fun updateAppointmentStatus(id: String, status: String)
    fun updatePaymentDetails(id: String, paymentMethod: String, paymentStatus: String, transactionId: String?)
    fun getAppointmentsByUserAndPaymentStatus(userId: String, paymentStatus: String): List<Appointment>
}