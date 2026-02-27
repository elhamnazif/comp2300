package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.calendar.CalendarCategory
import com.group8.comp2300.domain.model.calendar.MasterCalendarEvent
import com.group8.comp2300.domain.model.mood.MoodSummary
import kotlinx.datetime.LocalDate

interface CalendarRepository {
    fun getEventsForRange(userId: String, startDate: LocalDate, endDate: LocalDate): List<MasterCalendarEvent>
    fun getEventsByCategory(userId: String, category: CalendarCategory): List<MasterCalendarEvent>
    fun deleteCalendarEvent(eventId: String, type: CalendarCategory)
    fun updateMedicationStatus(logId: String, status: String)
    fun logMood(userId: String, mood: String, feeling: String? = null, journal: String? = null)
    fun getDailySummary(userId: String, date: LocalDate): List<MoodSummary>
    fun getMonthlySummary(userId: String, monthStart: LocalDate): List<MoodSummary>
}
