package com.group8.comp2300.domain.model.reminder

import com.group8.comp2300.domain.model.calendar.CalendarCategory
import kotlinx.datetime.LocalDateTime

data class MasterCalendarEvent(
    val eventId: String,
    val type: CalendarCategory,
    val title: String,
    val subtitle: String,
    val eventTime: LocalDateTime,
    val status: String
)
