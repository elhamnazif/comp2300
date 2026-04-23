package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.medical.MedicationLog
import com.group8.comp2300.domain.model.medical.MedicationLogStatus

class MedicationLogLocalDataSource(private val database: AppDatabase) {
    fun getAll(): List<MedicationLog> =
        database.appDatabaseQueries.selectAllMedicationLogs().executeAsList().map(::toMedicationLog)

    fun getById(id: String): MedicationLog? =
        database.appDatabaseQueries.selectMedicationLogById(id).executeAsOneOrNull()?.let(::toMedicationLog)

    /**
     * Get medication logs for a specific date range.
     * [startOfDayMs] and [endOfDayMs] are epoch milliseconds bounding the day.
     */
    fun getByDateRange(startOfDayMs: Long, endOfDayMs: Long): List<MedicationLog> =
        database.appDatabaseQueries.selectMedicationLogsByDate(startOfDayMs, endOfDayMs)
            .executeAsList()
            .map(::toMedicationLog)

    fun insert(log: MedicationLog) {
        database.appDatabaseQueries.insertMedicationLog(
            id = log.id,
            medicationId = log.medicationId,
            medicationTime = log.medicationTime,
            status = log.status.name,
            routineId = log.routineId,
            occurrenceTimeMs = log.occurrenceTimeMs,
            medicationName = log.medicationName,
        )
    }

    fun deleteById(id: String) {
        database.appDatabaseQueries.deleteMedicationLogById(id)
    }

    fun replaceAll(logs: List<MedicationLog>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllMedicationLogs()
            logs.forEach(::insert)
        }
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllMedicationLogs()
    }

    private fun toMedicationLog(entity: com.group8.comp2300.data.database.MedicationLogEntity): MedicationLog =
        MedicationLog(
            id = entity.id,
            medicationId = entity.medicationId,
            medicationTime = entity.medicationTime,
            status = MedicationLogStatus.valueOf(entity.status),
            routineId = entity.routineId,
            occurrenceTimeMs = entity.occurrenceTimeMs,
            medicationName = entity.medicationName,
        )
}
