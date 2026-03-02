package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase

/**
 * Clears all offline-cached medical data from the local database.
 * Call this on logout to prevent stale data from leaking between users.
 */
class MedicalCacheManager(private val database: AppDatabase) {

    fun clearAll() {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllAppointments()
            database.appDatabaseQueries.deleteAllMoods()
            database.appDatabaseQueries.deleteAllMedicationLogs()
            database.appDatabaseQueries.deleteAllCalendarOverview()
        }
    }
}
