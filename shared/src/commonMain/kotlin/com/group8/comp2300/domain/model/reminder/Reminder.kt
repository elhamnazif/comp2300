package com.group8.comp2300.domain.model.reminder

import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val userId: String,
    val title: String,
    val message: String = "",
    val scheduledTime: Long, // Unix timestamp for first occurrence
    val type: ReminderType,
    val frequency: ReminderFrequency = ReminderFrequency.ONCE,
    val isEnabled: Boolean = true,
    val relatedEntityId: String? = null, // e.g., medicationId or appointmentId
    val createdAt: Long = 0L
)
