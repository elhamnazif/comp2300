package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.MoodEntity
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.repository.MoodRepository

class MoodRepositoryImpl(private val database: ServerDatabase) : MoodRepository {

    override fun insert(mood: Mood) {
        database.moodQueries.insertMood(
            id = mood.id,
            user_id = mood.userId,
            timestamp = mood.timestamp,
            mood_type = mood.moodType.name,
            feeling = mood.feeling,
            journal = mood.journal,
        )
    }

    override fun getHistory(userId: String): List<Mood> = database.moodQueries.selectMoodHistory(userId)
        .executeAsList()
        .map { it.toDomain() }

    override fun getDailyMoods(userId: String, dateString: String): List<Mood> =
        database.moodQueries.selectDayMoods(userId, dateString)
            .executeAsList()
            .map { it.toDomain() }

    override fun getDailyCount(userId: String, dateString: String): Map<MoodType, Long> =
        database.moodQueries.getDailyMoodCount(userId, dateString)
            .executeAsList()
            .associate { row ->
                MoodType.valueOf(row.mood_type) to row.count
            }

    override fun getMonthlyCount(userId: String, monthStart: String): Map<MoodType, Long> {
        // monthStart should be in 'YYYY-MM-DD' format (e.g., '2024-05-01')
        return database.moodQueries.getMonthlyMoodCount(userId, monthStart, monthStart)
            .executeAsList()
            .associate { row ->
                MoodType.valueOf(row.mood_type) to row.count
            }
    }

    override fun update(id: String, moodType: MoodType, feeling: String?, journal: String?) {
        database.moodQueries.updateMoodById(
            mood_type = moodType.name,
            feeling = feeling,
            journal = journal,
            id = id,
        )
    }

    override fun delete(id: String) {
        database.moodQueries.deleteMoodById(id)
    }
}

private fun MoodEntity.toDomain() = Mood(
    id = id,
    userId = user_id,
    timestamp = timestamp,
    moodType = MoodType.valueOf(mood_type),
    feeling = feeling,
    journal = journal,
)
