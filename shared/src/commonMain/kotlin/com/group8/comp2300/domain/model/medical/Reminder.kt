package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val userId: String,
    val medScheduleId: String? = null,
    val appointmentId: String? = null,
    val offsetMins: Int, // Allowed: 0, 5, 10, 15, 30, 60
    val isEnabled: Boolean = true,
)
