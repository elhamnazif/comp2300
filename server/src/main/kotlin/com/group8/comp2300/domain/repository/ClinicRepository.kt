package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Clinic

interface ClinicRepository {
    fun getById(id: String): Clinic?

    /**
     * Adds a new clinic to the database.
     */
    fun insert(clinic: Clinic)

    /**
     * Updates the details of an existing clinic record.
     */
    fun update(clinic: Clinic)

    /**
     * Deletes a clinic from the database by its ID.
     */
    fun delete(id: String)
}
