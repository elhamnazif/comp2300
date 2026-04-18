package com.group8.comp2300.data.repository

import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository

class RemoteClinicRepository(private val apiService: ApiService) : ClinicRepository {
    override suspend fun getAllClinics(): List<Clinic> = apiService.getClinics()

    override suspend fun getClinicById(id: String): Clinic? = runCatching { apiService.getClinic(id) }.getOrNull()

    override suspend fun getAvailableSlots(clinicId: String): List<AppointmentSlot> =
        apiService.getClinicAvailability(clinicId)
}
