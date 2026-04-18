package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic

interface ClinicRepository {
    suspend fun getAllClinics(): List<Clinic>

    suspend fun getClinicById(id: String): Clinic?

    suspend fun getAvailableSlots(clinicId: String): List<AppointmentSlot>
}
