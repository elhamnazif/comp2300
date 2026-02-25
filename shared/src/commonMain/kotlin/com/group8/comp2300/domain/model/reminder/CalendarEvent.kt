package com.group8.comp2300.domain.model.reminder

import kotlinx.datetime.LocalDateTime

/* Acceptance Criteria
   User can create events with:
   Title
   Category (appointment, medication, mood, menstrual cycle)
   Date & time
   Notes
   Events are saved and appear in the calendar
*/


date class CalendarEvent(
    val id: String,
    val title: String,
    val category: CalendarCategory,
    val dateTime: LocalDateTime,
    val notes: String
)

