package com.group8.comp2300.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.group8.comp2300.`data`.database.AppDatabase
import com.group8.comp2300.`data`.database.ReminderEntity
import com.group8.comp2300.domain.model.Reminder
import com.group8.comp2300.domain.model.ReminderFrequency
import com.group8.comp2300.domain.model.ReminderType
import com.group8.comp2300.domain.repository.ReminderRepository
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ReminderRepositoryImpl(private val database: AppDatabase) : ReminderRepository {
    override fun getReminders(): Flow<List<Reminder>> = database.appDatabaseQueries
        .selectAll()
        .asFlow()
        .mapToList(Dispatchers.Default)
        .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getById(id: String): Reminder? = withContext(Dispatchers.Default) {
        database.appDatabaseQueries.selectById(id).executeAsOneOrNull()?.toDomain()
    }

    override suspend fun insert(reminder: Reminder) = withContext(Dispatchers.Default) {
        database.appDatabaseQueries.insert(
            id = reminder.id,
            title = reminder.title,
            description = reminder.description,
            reminderTime = reminder.reminderTime,
            type = reminder.type.name,
            frequency = reminder.frequency.name,
            isEnabled = if (reminder.isEnabled) 1L else 0L,
            createdAt = reminder.createdAt,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        Unit
    }

    override suspend fun update(reminder: Reminder) = withContext(Dispatchers.Default) {
        database.appDatabaseQueries.update(
            title = reminder.title,
            description = reminder.description,
            reminderTime = reminder.reminderTime,
            type = reminder.type.name,
            frequency = reminder.frequency.name,
            isEnabled = if (reminder.isEnabled) 1L else 0L,
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = reminder.id
        )
        Unit
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        database.appDatabaseQueries.delete(id)
        Unit
    }
}

private fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    description = description,
    reminderTime = reminderTime,
    type = ReminderType.valueOf(type),
    frequency = ReminderFrequency.valueOf(frequency),
    isEnabled = isEnabled == 1L,
    createdAt = createdAt,
    updatedAt = updatedAt
)
