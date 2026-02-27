package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus

interface MedicationLogRepository {

    /**
     * Records a new medication log entry (usually pre-generated or user-created).
     */
    fun insert(log: MedicationLog)

    /**
     * Retrieves all log history for a specific medication.
     */
    fun getLogsByMedication(medicationId: String): List<MedicationLog>

    /**
     * Retrieves a single log entry by its unique ID.
     */
    fun getById(id: String): MedicationLog?

    /**
     * Returns the medication schedule/agenda for a specific day.
     * @param userId
     * @param dateString Expected format: "YYYY-MM-DD".
     */
    fun getDailyAgenda(userId: String, dateString: String): List<MedicationLog>

    /**
     * Updates the status of a log (e.g., from 'PENDING' to 'TAKEN' or 'SKIPPED').
     */
    fun updateStatus(id: String, status: MedicationLogStatus)

    /**
     * Removes a log entry from the database.
     */
    fun delete(id: String)
}
