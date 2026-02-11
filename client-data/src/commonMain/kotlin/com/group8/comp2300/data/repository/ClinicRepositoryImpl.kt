package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.repository.ClinicRepository
import com.group8.comp2300.mock.sampleClinics

class ClinicRepositoryImpl : ClinicRepository {
    override fun getAllClinics(): List<Clinic> = sampleClinics

    override fun getClinicById(id: String): Clinic? = sampleClinics.find { it.id == id }
}
