package com.group8.comp2300.domain.model.calendar

/* As a user, I want to filter categories in calendar
(appointments/medications/moods/menstrual cycle) so that I can choose what to focus on.


Acceptance Criteria
User can create events with:
Title
Category (appointment, medication, mood, menstrual cycle)
Date & time
Notes
Events are saved and appear in the calendar
*/

enum class CalendarCategory {
    APPOINTMENT,
    MEDICATION,
    MOOD,
    MENSTRUAL_CYCLE
}
