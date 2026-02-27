package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class AppointmentSlot(
    val id: String,
    val clinicId: String,
    val startTime: Long,
    val endTime: Long,
    val isBooked: Boolean = false,
)
