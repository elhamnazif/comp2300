package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus

class MedicationLogLocalDataSource(private val database: AppDatabase) {

    fun getAll(): List<MedicationLog> =
        database.appDatabaseQueries.selectAllMedicationLogs().executeAsList().map { entity ->
            MedicationLog(
                id = entity.id,
                medicationId = entity.medicationId,
                medicationTime = entity.medicationTime,
                status = MedicationLogStatus.valueOf(entity.status),
                medicationName = entity.medicationName,
            )
        }

    /**
     * Get medication logs for a specific date range.
     * [startOfDayMs] and [endOfDayMs] are epoch milliseconds bounding the day.
     */
    fun getByDateRange(startOfDayMs: Long, endOfDayMs: Long): List<MedicationLog> =
        database.appDatabaseQueries.selectMedicationLogsByDate(startOfDayMs, endOfDayMs)
            .executeAsList()
            .map { entity ->
                MedicationLog(
                    id = entity.id,
                    medicationId = entity.medicationId,
                    medicationTime = entity.medicationTime,
                    status = MedicationLogStatus.valueOf(entity.status),
                    medicationName = entity.medicationName,
                )
            }

    fun insert(log: MedicationLog) {
        database.appDatabaseQueries.insertMedicationLog(
            id = log.id,
            medicationId = log.medicationId,
            medicationTime = log.medicationTime,
            status = log.status.name,
            medicationName = log.medicationName,
        )
    }

    fun replaceAll(logs: List<MedicationLog>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllMedicationLogs()
            logs.forEach { insert(it) }
        }
    }
}
