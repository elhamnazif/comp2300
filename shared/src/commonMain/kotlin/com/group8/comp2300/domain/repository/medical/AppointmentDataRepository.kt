package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.Appointment
import com.group8.comp2300.domain.model.medical.ClinicBookingRequest

interface AppointmentDataRepository {
    suspend fun getAppointments(): List<Appointment>

    suspend fun bookClinicAppointment(request: ClinicBookingRequest): Appointment
}
