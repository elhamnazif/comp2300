package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.data.database.ReminderEntity
import com.group8.comp2300.domain.model.reminder.Reminder
import com.group8.comp2300.domain.model.reminder.ReminderFrequency
import com.group8.comp2300.domain.model.reminder.ReminderType

class ReminderLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<Reminder> = database.appDatabaseQueries.selectAllReminders()
        .executeAsList()
        .map { it.toDomain() }
}

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    userId = "",
    title = title,
    message = description.orEmpty(),
    scheduledTime = reminderTime,
    type = parseReminderType(type),
    frequency = parseReminderFrequency(frequency),
    isEnabled = isEnabled == 1L,
    relatedEntityId = null,
    createdAt = createdAt,
)

private fun parseReminderType(type: String): ReminderType = when (type) {
    "LAB_TEST" -> ReminderType.SCREENING
    "EDUCATION", "GENERAL" -> ReminderType.CUSTOM
    else -> ReminderType.entries.firstOrNull { it.name == type } ?: ReminderType.CUSTOM
}

private fun parseReminderFrequency(frequency: String): ReminderFrequency = when (frequency) {
    "YEARLY" -> ReminderFrequency.MONTHLY
    else -> ReminderFrequency.entries.firstOrNull { it.name == frequency } ?: ReminderFrequency.ONCE
}
