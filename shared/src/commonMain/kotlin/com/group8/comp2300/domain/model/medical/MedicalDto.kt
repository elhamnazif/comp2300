package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

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
)

@Serializable
data class MedicationCreateRequest(
    val name: String,
    val dosage: String,
    val quantity: String = "",
    val frequency: String = "DAILY",
    val instruction: String? = null,
    val colorHex: String? = null,
    val startDate: String, // YYYY-MM-DD
    val endDate: String, // YYYY-MM-DD
    val hasReminder: Boolean = true,
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
