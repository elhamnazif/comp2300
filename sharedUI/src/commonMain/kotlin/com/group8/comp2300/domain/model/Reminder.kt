package com.group8.comp2300.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderType {
    MEDICATION,
    APPOINTMENT,
    LAB_TEST,
    EDUCATION,
    GENERAL
}

@Serializable
enum class ReminderFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Serializable
data class Reminder(
    val id: String,
    val title: String,
    val description: String? = null,
    val reminderTime: Long,
    val type: ReminderType,
    val frequency: ReminderFrequency,
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long
)
