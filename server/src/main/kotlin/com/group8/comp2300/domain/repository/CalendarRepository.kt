package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.calendar.CalendarCategory
import com.group8.comp2300.domain.model.reminder.MasterCalendarEvent
import com.group8.comp2300.database.ServerDatabase
import kotlinx.datetime.LocalDate

class CalendarRepository(private val db: ServerDatabase) {

    // USER STORY 5: fetching date ranges for calendar view

    fun getEventsforRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<MasterCalendarEvent> {
        return db.serverDatabaseQueries.selectCalendarRange(
                user_id = userId,
                event_time = startDate.toString(),
                event_time_ = endDate.toString(),
            ).executeAsList().map { it.toDomainModel() }
    }



    // USER STORY 6: filtering events by categories

    fun getEventsbyCategory(userId: String, category: CalendarCategory): List<MasterCalendarEvent> {
        return db.serverDatabaseQueries.selectFilteredEvent(
            userId,
            category.name
        ).executeAsList().map { it.toDomainModel() }
    }



   // USER STORY 9 & 10: editing and deleting calendar events

   // deleting an event based on type

    fun deleteCalendarEvent(eventId: String, type: CalendarCategory) {
        when (type) {
            CalendarCategory.APPOINTMENT -> db.serverDatabaseQueries.deleteAppointment(eventId)
            CalendarCategory.MEDICATION -> db.serverDatabaseQueries.deleteMedicationLog(eventId)
            CalendarCategory.MOOD -> db.serverDatabaseQueries.deleteMoodById(eventId)
            // CalendarCategory.MENSTRUAL_CYCLE -> db.serverDatabaseQueries.delete?(eventId) !! NEED TO ADD MENSTRUAL CYCLE TO DB
            else -> { }
        }
    }

   // updating an event's status

    fun updateMedicationStatus(logId: String, status: String) {
        db.serverDatabaseQueries.updateMedicationStatus(
            status = status,
            id = logId)
    }

    // logging mood
    fun logMood (id: String, userId: String, mood: String, feeling: String?, journal: String?) {
        db.serverDatabaseQueries.insertMood(
            id = id,
            user_id = userId,
            timestamp = null,
            mood_type = mood,
            feeling = feeling,
            journal = journal
        )
    }

}
