package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Clinic

interface ClinicRepository {
    /** Return all available clinics. */
    fun getAllClinics(): List<Clinic>

    /** Return a clinic by its ID, or null if not found. */
    fun getClinicById(id: String): Clinic?
}
