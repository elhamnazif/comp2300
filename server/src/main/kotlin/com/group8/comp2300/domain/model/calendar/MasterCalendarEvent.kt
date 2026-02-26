package com.group8.comp2300.domain.model.reminder

import kotlinx.datetime.LocalDateTime

// event models for user stories 5-8
data class MasterCalendarEvent(
    val eventId: String,
    val type: CalendarCategory,
    val title: String,
    val subtitle: String,
    val eventTime: LocalDateTime,
    val status: String
)

// categories for user stories 6-7
enum class CalendarCategory {
    APPOINTMENT,
    MEDICATION,
    MOOD,
    MENSTRUAL_CYCLE;

    companion object {
        fun fromString(value: String): CalendarCategory {
            return entries.find { it.name == value } ?: APPOINTMENT
        }
    }
}
