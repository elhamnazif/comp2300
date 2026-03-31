package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.util.hashPin
import kotlin.time.Clock

class PinDataSource(private val database: AppDatabase) {

    suspend fun savePin(pin: String) {
        database.appDatabaseQueries.upsertPin(
            id = "pin",
            pinHash = hashPin(pin),
            createdAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = database.appDatabaseQueries.selectPin().executeAsOneOrNull()
        return stored != null && stored.pinHash == hashPin(pin)
    }

    suspend fun isPinSet(): Boolean = database.appDatabaseQueries.selectPin().executeAsOneOrNull() != null

    suspend fun clearPin() {
        database.appDatabaseQueries.deletePin()
    }
}
