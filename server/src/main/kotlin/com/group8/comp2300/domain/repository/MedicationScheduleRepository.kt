package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.MedicationSchedule

interface MedicationScheduleRepository {

    /**
     * Retrieves all recurring schedules associated with a specific medication.
     */
    fun getAllByMedicationId(medicationId: String): List<MedicationSchedule>

    /**
     * Saves a new scheduling rule (Day of week + Time).
     */
    fun insert(schedule: MedicationSchedule)

    /**
     * Updates an existing scheduling rule.
     */
    fun update(schedule: MedicationSchedule)

    /**
     * Deletes a specific scheduling rule by its ID.
     */
    fun delete(id: String)
}