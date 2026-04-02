package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MedicationLogLinkMode {
    ATTACH_TO_OCCURRENCE,
    EXTRA_DOSE,
}

@Serializable
data class CalendarOverviewResponse(
    // format "YYYY-MM-DD"
    val date: String,
    // "TAKEN", "MISSED", "APPOINTMENT", "NONE"
    val status: String,
)

@Serializable
data class MedicationLogRequest(
    val medicationId: String,
    // "TAKEN", "MISSED", "PENDING"
    val status: String,
    val timestampMs: Long? = null,
    val routineId: String? = null,
    val occurrenceTimeMs: Long? = null,
    val linkMode: MedicationLogLinkMode? = null,
)

@Serializable
data class MedicationOccurrenceCandidate(
    val medicationId: String,
    val routineId: String,
    val routineName: String,
    val occurrenceTimeMs: Long,
    val status: MedicationLogStatus,
)

@Serializable
data class MedicationCreateRequest(
    val name: String,
    val dosage: String,
    val quantity: String = "",
    val frequency: String = "DAILY",
    val instruction: String? = null,
    val colorHex: String? = null,
    val status: String = MedicationStatus.ACTIVE.name,
)

@Serializable
data class RoutineCreateRequest(
    val name: String,
    val timesOfDayMs: List<Long>,
    val repeatType: String,
    val daysOfWeek: List<Int> = emptyList(),
    val startDate: String,
    val endDate: String? = null,
    val hasReminder: Boolean = true,
    val reminderOffsetsMins: List<Int> = emptyList(),
    val status: String = RoutineStatus.ACTIVE.name,
    val medicationIds: List<String> = emptyList(),
)

@Serializable
data class AppointmentRequest(
    val title: String,
    val appointmentTime: Long,
    val appointmentType: String,
    val notes: String? = null,
    val doctorName: String? = null,
)

@Serializable
data class MoodEntryRequest(
    val moodScore: Int,
    val tags: List<String>,
    val symptoms: List<String>,
    val notes: String? = null,
)
