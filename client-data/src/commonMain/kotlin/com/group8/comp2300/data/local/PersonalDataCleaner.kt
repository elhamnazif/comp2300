package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase

class PersonalDataCleaner(private val database: AppDatabase) {
    fun clearAllPersonalData() {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllAppointments()
            database.appDatabaseQueries.deleteAllMoods()
            database.appDatabaseQueries.deleteAllMedications()
            database.appDatabaseQueries.deleteAllMedicationLogs()
            database.appDatabaseQueries.deleteAllReminders()
            database.appDatabaseQueries.deleteAllOutbox()
        }
    }
}
