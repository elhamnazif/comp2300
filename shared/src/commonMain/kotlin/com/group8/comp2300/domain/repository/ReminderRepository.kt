package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.reminder.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun getReminders(): Flow<List<Reminder>>

    suspend fun getById(id: String): Reminder?

    suspend fun insert(reminder: Reminder)

    suspend fun update(reminder: Reminder)

    suspend fun delete(id: String)
}
