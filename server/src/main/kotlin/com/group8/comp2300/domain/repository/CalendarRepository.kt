package com.group8.comp2300.domain.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.calendar.MasterCalendarEvent
import com.group8.comp2300.domain.model.calendar.CalendarCategory
import com.group8.comp2300.domain.model.mood.MoodSummary
import com.group8.comp2300.domain.repository.toDomainModel
import kotlinx.datetime.LocalDate
import java.util.UUID

class CalendarRepository(private val db: ServerDatabase) {

    private val queries = db.serverDatabaseQueries

    // USER STORY 5: fetching date ranges for calendar view

    fun getEventsForRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<MasterCalendarEvent> {
        return queries.selectCalendarRange(
            user_id = userId,
            event_time = startDate.toString(),
            event_time_ = endDate.toString()
        ).executeAsList().map { it.toDomainModel() }
    }

    // USER STORY 6: filtering events by categories

    fun getEventsByCategory(userId: String, category: CalendarCategory): List<MasterCalendarEvent> {
        return queries.selectFilteredEvent(
            user_id = userId,
            event_type = category.name
        ).executeAsList().map { it.toDomainModel() }
    }


    // USER STORY 9 & 10: editing and deleting calendar events

    // deleting an event based on type

    fun deleteCalendarEvent(eventId: String, type: CalendarCategory) {
        when (type) {
            CalendarCategory.APPOINTMENT -> queries.deleteAppointment(eventId)
            CalendarCategory.MEDICATION -> queries.deleteMedicationLog(eventId)
            CalendarCategory.MOOD -> queries.deleteMoodById(eventId)
            CalendarCategory.MENSTRUAL_CYCLE -> {} // missing
        }
    }

    // updating an event's status

    fun updateMedicationStatus(logId: String, status: String) {
        queries.updateMedicationStatus(
            status = status,
            id = logId,
        )
    }

    // logging mood
    fun logMood(userId: String, mood: String, feeling: String? = null, journal: String? = null) {
        queries.insertMood(
            id = java.util.UUID.randomUUID().toString(),
            user_id = userId,
            timestamp = null,
            mood_type = mood,
            feeling = feeling,
            journal = journal
        )
    }

    fun getDailySummary(userId: String, date: LocalDate): List<MoodSummary> {
        val rows = queries.getDailyMoodCount(userId, date.toString()).executeAsList()

        val totalEntries = rows.sumOf { it.count }

        return rows.map { row ->
            MoodSummary(
                moodType = row.mood_type,
                count = row.count.toInt(),
                percentage = if (totalEntries > 0L) (row.count.toFloat() / totalEntries) * 100f else 0f
            )
        }
    }

    fun getMonthlySummary(userId: String, monthStart: LocalDate): List<MoodSummary> {

        val rows  = queries.getMonthlyMoodCount(
            user_id = userId,
            timestamp = monthStart.toString(),
            timestamp_ = monthStart.toString()
        ).executeAsList()

        val totalEntries = rows.sumOf { it.count }

        return rows.map { row ->
            MoodSummary(
                moodType = row.mood_type,
                count = row.count.toInt(),
                percentage = if (totalEntries > 0L) (row.count.toFloat() / totalEntries) * 100f else 0f
            )
        }
    }
}

