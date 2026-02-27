package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest
import com.group8.comp2300.domain.model.medical.CalendarOverviewResponse
import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogRequest
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest

interface MedicalRepository {
    fun getRecentLabResults(): List<LabResult>

    suspend fun getCalendarOverview(year: Int, month: Int): List<CalendarOverviewResponse>

    suspend fun getAppointments(): List<Appointment>

    suspend fun scheduleAppointment(request: AppointmentRequest): Appointment

    suspend fun logMedication(request: MedicationLogRequest): MedicationLog

    suspend fun getMedicationAgenda(date: String): List<MedicationLog>

    suspend fun logMood(request: MoodEntryRequest): Mood
}
