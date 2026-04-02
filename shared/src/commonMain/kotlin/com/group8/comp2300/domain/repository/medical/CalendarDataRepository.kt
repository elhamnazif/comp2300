package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse

interface CalendarDataRepository {
    suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse>
}
