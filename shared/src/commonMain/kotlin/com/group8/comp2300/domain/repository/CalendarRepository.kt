package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.reminder.*
import com.group8.comp2300.database.AppDatabase
import kotlinx.datetime*

class CalendarRepository(private val db: AppDatabase) {



    // USER STORY 5: fetching date ranges for calendar view

    fun getEventsforRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<MasterCalendarEvent> {
        return db.appDatabaseQueries
            .selectCalendarRange(
                user_id = userId,
                event_time = startDate.toString(),
                event_time_ = endDate.toString(),
            )
            .executeAsList()
            .map { it.toDomainModel() }
    }



    // USER STORY 6: filtering events by categories

    fun getEventsbyCategory(userId: String, category: CalendarCategory): List<MasterCalendarEvent> {
        return db.appDatabaseQueries
            .selectFilteredEvent(userId, category.name)
            .executeAsList()
            .map { it.toDomainModel() }
    }



   // USER STORY 9 & 10: editing and deleting calendar events

   // deleting an event based on type

    fun deleteCalendarEvent(eventId: String, type: CalendarCategory): List<MasterCalendarEvent> {
        when (type) {
            CalendarCategory.APPOINTMENT -> db.appDatabaseQueries.deleteAppointment(eventId)
            CalendarCategory.MEDICATION -> db.appDatabaseQueries.deleteMedicationLog(eventId)
            CalendarCategory.MOOD -> db.appDatabaseQueries.deleteMoodById(eventId)
            // CalendarCategory.MENSTRUAL_CYCLE -> db.appDatabaseQueries.delete?(eventId) !! NEED TO ADD MENSTRUAL CYCLE TO DB
        }
    }

   // updating an event's status

    fun updateMedicationStatus(logId: String, status: String) {
        db.appDatabaseQueries.updateMedicationStatus(status, logId)
    }
}