package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.reminder.AdherenceStatus
import com.group8.comp2300.domain.model.reminder.CalendarDay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

val sampleAppointments =
    listOf(
        Appointment(
            id = "1",
            userId = "user1",
            title = "HIV/STI Screening",
            appointmentTime = 1729728000000, // 2024-10-24 00:00:00 UTC
            appointmentType = "SCREENING",
            clinicId = "1",
            bookingId = "slot1",
            status = "CONFIRMED",
            notes = null,
            hasReminder = true,
        ),
        Appointment(
            id = "2",
            userId = "user1",
            title = "PrEP Follow-up",
            appointmentTime = 1731360000000, // 2024-11-12 00:00:00 UTC
            appointmentType = "FOLLOW_UP",
            clinicId = null,
            bookingId = "slot2",
            status = "CONFIRMED",
            notes = null,
            hasReminder = true,
        ),
    )

// Helper: Date Generator
fun generateCurrentMonthData(): List<CalendarDay> {
    val now = Clock.System.now()
    val systemZone = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(systemZone).date

    val year = today.year
    val month = today.month

    // Calculate days in month
    val firstDayOfNextMonth = LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH)
    val lastDayOfCurrentMonth = firstDayOfNextMonth.minus(1, DateTimeUnit.DAY)
    val daysInMonth = lastDayOfCurrentMonth.day

    return (1..daysInMonth).map { day ->
        val date = LocalDate(year, month, day)
        val status =
            when {
                date > today -> AdherenceStatus.NONE

                date == today -> AdherenceStatus.NONE

                // Logic for today handled in UI state
                day % 5 == 0 -> AdherenceStatus.MISSED

                // Mock logic
                day == 15 -> AdherenceStatus.APPOINTMENT

                else -> AdherenceStatus.TAKEN
            }
        CalendarDay(day, date, status, date == today)
    }
}
