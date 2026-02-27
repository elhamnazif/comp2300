package com.group8.comp2300.domain.model.medical

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class Appointment(
    val id: String,
    val title: String,
    val date: LocalDate,
    val time: LocalTime,
    val type: AppointmentType,
    val clinicId: String? = null,
    val doctorId: String? = null,
    val notes: String? = null,
    val reminderEnabled: Boolean = true,
)
