package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse

class CalendarOverviewLocalDataSource(private val database: AppDatabase) {

    private fun yearMonthKey(year: Int, month: Int): String =
        "$year-${month.toString().padStart(2, '0')}"

    fun getByYearMonth(year: Int, month: Int): List<CalendarOverviewResponse> {
        val key = yearMonthKey(year, month)
        return database.appDatabaseQueries.selectCalendarOverviewByYearMonth(key)
            .executeAsList()
            .map { entity ->
                CalendarOverviewResponse(
                    date = entity.date,
                    status = entity.status,
                )
            }
    }

    fun replaceForYearMonth(year: Int, month: Int, items: List<CalendarOverviewResponse>) {
        val key = yearMonthKey(year, month)
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteCalendarOverviewByYearMonth(key)
            items.forEach { item ->
                database.appDatabaseQueries.insertCalendarOverview(
                    date = item.date,
                    status = item.status,
                    yearMonth = key,
                )
            }
        }
    }
}
