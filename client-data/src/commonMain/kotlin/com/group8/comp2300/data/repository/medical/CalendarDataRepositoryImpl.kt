package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.MedicationLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.data.local.RoutineLocalDataSource
import com.group8.comp2300.data.local.RoutineOccurrenceOverrideLocalDataSource
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.buildCalendarOverviewStatus
import com.group8.comp2300.domain.repository.medical.CalendarDataRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

class CalendarDataRepositoryImpl(
    private val medicationLocal: MedicationLocalDataSource,
    private val routineLocal: RoutineLocalDataSource,
    private val routineOccurrenceOverrideLocal: RoutineOccurrenceOverrideLocalDataSource,
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val appointmentLocal: AppointmentLocalDataSource,
) : CalendarDataRepository {
    override suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse> {
        val medications = medicationLocal.getAll()
        val routines = routineLocal.getAll()
        val overrides = routineOccurrenceOverrideLocal.getAll()
        val logs = medicationLogLocal.getAll()
        val appointmentsByDate = appointmentLocal
            .getAll()
            .filterNot(::isCancelledAppointment)
            .groupBy { it.appointmentTime.toLocalDateString() }
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val timeZone = TimeZone.currentSystemDefault()

        val results = mutableListOf<CalendarOverviewResponse>()
        var cursor = LocalDate.parse("$year-${month.toString().padStart(2, '0')}-01")

        while (cursor.month.number == month) {
            val date = cursor.toString()
            val medicationStatus =
                buildCalendarOverviewStatus(routines, medications, logs, overrides, cursor, nowMs, timeZone)
            val status = when {
                medicationStatus != "NONE" -> medicationStatus
                !appointmentsByDate[date].isNullOrEmpty() -> "APPOINTMENT"
                else -> "NONE"
            }
            results += CalendarOverviewResponse(date = date, status = status)
            cursor = cursor.plus(1, DateTimeUnit.DAY)
        }

        return results
    }
}

private fun isCancelledAppointment(appointment: com.group8.comp2300.domain.model.medical.Appointment): Boolean =
    appointment.status == "CANCELLED"

private fun Long.toLocalDateString(): String = Instant.fromEpochMilliseconds(this)
    .toLocalDateTime(TimeZone.currentSystemDefault())
    .date
    .toString()
