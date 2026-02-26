package com.group8.comp2300.domain.model.reminder

import com.group8.comp2300.domain.model.calendar.CalendarCategory
import kotlinx.datetime.LocalDateTime

data class CalendarEvent(
    val id: String,
    val title: String,
    val category: CalendarCategory,
    val dateTime: LocalDateTime,
    val notes: String
)
