package com.group8.comp2300.domain.repository

// converts sql rows -> domain models

import com.group8.comp2300.domain.model.reminder.CalendarCategory
import com.group8.comp2300.domain.model.reminder.MasterCalendarEvent
import com.group8.comp2300.database.MasterCalendar
import kotlinx.datetime.toLocalDateTime

fun MasterCalendar.toDomainModel(): MasterCalendarEvent {
    return MasterCalendarEvent(
        eventId = this.event_id,
        type = CalendarCategory.fromString(this.event_type),
        title = this.display_title,
        subtitle = this.subtitle ?: "",
        eventTime = this.event_time.toLocalDateTime(),
        status = this.event_status ?: "PENDING"
    )
}
