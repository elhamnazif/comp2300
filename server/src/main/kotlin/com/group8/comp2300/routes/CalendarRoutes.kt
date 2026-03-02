package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.repository.CalendarRepository
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.koin.ktor.ext.inject

fun Route.calendarRoutes() {
    val calendarRepository: CalendarRepository by inject()

    route("/api/calendar") {
        get("/overview") {
            withUserId { userId ->
                val yearStr = call.request.queryParameters["year"]
                val monthStr = call.request.queryParameters["month"]

                if (yearStr == null || monthStr == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "year and month parameters are required"))
                    return@withUserId
                }

                try {
                    val year = yearStr.toInt()
                    val month = monthStr.toInt()
                    val startDate = LocalDate(year, month, 1)
                    val endDate = startDate.plus(1, DateTimeUnit.MONTH)

                    // Get events for the month from the calendar repository
                    val events = calendarRepository.getEventsForRange(userId, startDate, endDate)

                    // Group events by date and derive status for each day
                    val eventsByDate = events.groupBy { it.eventTime.date.toString() }

                    val overview = mutableListOf<CalendarOverviewResponse>()
                    var currentDate = startDate
                    while (currentDate < endDate) {
                        val dateStr = currentDate.toString()
                        val dayEvents = eventsByDate[dateStr] ?: emptyList()

                        val status = when {
                            dayEvents.any { it.type.name == "APPOINTMENT" } -> "APPOINTMENT"
                            dayEvents.any { it.status == "TAKEN" } -> "TAKEN"
                            dayEvents.any { it.status == "MISSED" } -> "MISSED"
                            else -> "NONE"
                        }

                        overview.add(CalendarOverviewResponse(date = dateStr, status = status))
                        currentDate = currentDate.plus(1, DateTimeUnit.DAY)
                    }

                    call.respond(HttpStatusCode.OK, overview)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid year or month"))
                }
            }
        }
    }
}
