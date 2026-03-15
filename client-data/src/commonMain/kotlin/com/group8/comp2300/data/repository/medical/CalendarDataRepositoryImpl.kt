package com.group8.comp2300.data.repository.medical

import com.group8.comp2300.data.local.AppointmentLocalDataSource
import com.group8.comp2300.data.local.MedicationLogLocalDataSource
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus
import com.group8.comp2300.domain.repository.medical.CalendarDataRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class CalendarDataRepositoryImpl(
    private val medicationLogLocal: MedicationLogLocalDataSource,
    private val appointmentLocal: AppointmentLocalDataSource,
) : CalendarDataRepository {
    override suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse> {
        val logsByDate = medicationLogLocal.getAll().groupBy { it.medicationTime.toLocalDateString() }
        val appointmentsByDate = appointmentLocal.getAll().groupBy { it.appointmentTime.toLocalDateString() }

        val results = mutableListOf<CalendarOverviewResponse>()
        var cursor = LocalDate.parse("$year-${month.toString().padStart(2, '0')}-01")

        while (cursor.month.number == month) {
            val date = cursor.toString()
            val medicationStatus = logsByDate[date].toOverviewStatus()
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

private fun List<MedicationLog>?.toOverviewStatus(): String = when {
    this.isNullOrEmpty() -> "NONE"
    any { it.status == MedicationLogStatus.TAKEN } -> "TAKEN"
    any { it.status == MedicationLogStatus.SKIPPED } -> "MISSED"
    else -> "NONE"
}

private fun Long.toLocalDateString(): String =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
