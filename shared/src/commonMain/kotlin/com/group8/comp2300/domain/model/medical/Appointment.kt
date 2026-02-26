package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String,
    val userId: String,
    val title: String,
    val appointmentTime: Long,
    val appointmentType: String,
    val clinicId: String?,
    val bookingId: String?,
    val status: String,
    val notes: String?,
    val hasReminder: Boolean,
    val paymentStatus: String = "PENDING",
    val paymentMethod: String? = null,
    val paymentAmount: Double? = null,
    val transactionId: String? = null
)