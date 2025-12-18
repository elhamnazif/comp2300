package com.group8.comp2300.domain.model.reminder

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class CalendarDay(
        val dayOfMonth: Int,
        val date: LocalDate,
        val status: AdherenceStatus,
        val isToday: Boolean
)