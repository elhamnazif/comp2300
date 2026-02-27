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
