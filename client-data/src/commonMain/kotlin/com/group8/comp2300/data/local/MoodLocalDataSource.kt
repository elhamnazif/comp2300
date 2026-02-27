package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodType

class MoodLocalDataSource(private val database: AppDatabase) {

    fun getAll(): List<Mood> =
        database.appDatabaseQueries.selectAllMoods().executeAsList().map { entity ->
            Mood(
                id = entity.id,
                userId = entity.userId,
                timestamp = entity.timestamp,
                moodType = MoodType.valueOf(entity.moodType),
                feeling = entity.feeling,
                journal = entity.journal,
            )
        }

    fun insert(mood: Mood) {
        database.appDatabaseQueries.insertMood(
            id = mood.id,
            userId = mood.userId,
            timestamp = mood.timestamp,
            moodType = mood.moodType.name,
            feeling = mood.feeling,
            journal = mood.journal,
        )
    }

    fun replaceAll(moods: List<Mood>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllMoods()
            moods.forEach { insert(it) }
        }
    }
}
