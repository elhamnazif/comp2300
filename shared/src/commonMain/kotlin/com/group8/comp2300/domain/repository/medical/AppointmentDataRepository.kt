package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.AppointmentRequest

interface AppointmentDataRepository {
    suspend fun getAppointments(): List<Appointment>

    suspend fun scheduleAppointment(request: AppointmentRequest): Appointment
}
