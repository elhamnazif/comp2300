package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.view.MasterCalendar
import com.group8.comp2300.domain.model.calendar.CalendarCategory
import com.group8.comp2300.domain.model.calendar.MasterCalendarEvent
import com.group8.comp2300.domain.model.mood.MoodSummary
import com.group8.comp2300.domain.repository.CalendarRepository
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.UUID
import kotlin.time.Instant

class CalendarRepositoryImpl(private val db: ServerDatabase) : CalendarRepository {

    override fun getEventsForRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<MasterCalendarEvent> = db.masterCalendarQueries.selectCalendarRange(
        userId,
        startDate.toString(),
        endDate.toString(),
    ).executeAsList().map { it.toDomain() }

    override fun getEventsByCategory(userId: String, category: CalendarCategory): List<MasterCalendarEvent> =
        db.masterCalendarQueries.selectFilteredEvent(
            userId,
            category.name,
        ).executeAsList().map { it.toDomain() }

    override fun deleteCalendarEvent(eventId: String, type: CalendarCategory) {
        when (type) {
            CalendarCategory.APPOINTMENT -> db.appointmentQueries.deleteAppointment(eventId)

            CalendarCategory.MEDICATION -> db.medicationLogQueries.deleteMedicationLog(eventId)

            CalendarCategory.MOOD -> db.moodQueries.deleteMoodById(eventId)

            CalendarCategory.MENSTRUAL_CYCLE ->
                throw NotImplementedError("Menstrual cycle event deletion is not yet implemented")
        }
    }

    override fun updateMedicationStatus(logId: String, status: String) {
        db.medicationLogQueries.updateMedicationStatus(
            status,
            logId,
        )
    }

    override fun logMood(userId: String, mood: String, feeling: String?, journal: String?) {
        db.moodQueries.insertMood(
            UUID.randomUUID().toString(),
            userId,
            System.currentTimeMillis(),
            mood,
            feeling,
            journal,
        )
    }

    override fun getDailySummary(userId: String, date: LocalDate): List<MoodSummary> {
        val rows = db.moodQueries.getDailyMoodCount(userId, date.toString()).executeAsList()

        val totalEntries = rows.sumOf { it.count }

        return rows.map { row ->
            MoodSummary(
                moodType = row.mood_type,
                count = row.count.toInt(),
                percentage = if (totalEntries > 0L) (row.count.toFloat() / totalEntries) * 100f else 0f,
            )
        }
    }

    override fun getMonthlySummary(userId: String, monthStart: LocalDate): List<MoodSummary> {
        val monthEnd = LocalDate(monthStart.year, monthStart.month, 1).plus(1, DateTimeUnit.MONTH)
        val rows = db.moodQueries.getMonthlyMoodCount(
            userId,
            monthStart.toString(),
            monthEnd.toString(),
        ).executeAsList()

        val totalEntries = rows.sumOf { it.count }

        return rows.map { row ->
            MoodSummary(
                moodType = row.mood_type,
                count = row.count.toInt(),
                percentage = if (totalEntries > 0L) (row.count.toFloat() / totalEntries) * 100f else 0f,
            )
        }
    }
}

fun MasterCalendar.toDomain(): MasterCalendarEvent = MasterCalendarEvent(
    eventId = this.event_id,
    type = CalendarCategory.fromString(this.event_type),
    title = this.display_title,
    subtitle = this.subtitle ?: "",
    eventTime = Instant.fromEpochMilliseconds(this.event_time).toLocalDateTime(TimeZone.currentSystemDefault()),
    status = this.event_status ?: "PENDING",
)
