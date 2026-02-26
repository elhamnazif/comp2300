package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Medication

interface MedicationRepository {

    /**
     * Retrieves all medications (both Active and Archived) for a specific user.
     */
    fun getAllByUserId(userId: String): List<Medication>

    /**
     * Retrieves only medications with an 'ACTIVE' status.
     */
    fun getActiveByUserId(userId: String): List<Medication>

    /**
     * Retrieves only medications with an 'ARCHIVED' status.
     */
    fun getArchivedByUserId(userId: String): List<Medication>

    /**
     * Retrieves a single medication by its unique ID.
     */
    fun getById(id: String): Medication?

    fun insert(medication: Medication)

    fun update(medication: Medication)

    /**
     * Permanently removes a medication by its ID.
     */
    fun delete(id: String)
}