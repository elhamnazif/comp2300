package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.util.CurrentPinHashVersion
import com.group8.comp2300.util.hashPinSecure
import com.group8.comp2300.util.verifyPinHash
import kotlin.time.Clock

class PinDataSource(private val database: AppDatabase) {

    fun savePin(pin: String) {
        val result = hashPinSecure(pin)
        database.appDatabaseQueries.upsertPin(
            id = "pin",
            pinHash = result.hash,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            pinSalt = result.salt,
            pinIterations = result.iterations.toLong(),
            pinVersion = result.version.toLong(),
        )
    }

    fun verifyPin(pin: String): Boolean {
        val stored = database.appDatabaseQueries.selectPin().executeAsOneOrNull() ?: return false
        val matches = verifyPinHash(
            pin = pin,
            storedHash = stored.pinHash,
            salt = stored.pinSalt,
            iterations = stored.pinIterations.toInt(),
            version = stored.pinVersion.toInt(),
        )
        if (matches && stored.pinVersion.toInt() < CurrentPinHashVersion) {
            savePin(pin)
        }
        return matches
    }

    fun isPinSet(): Boolean = database.appDatabaseQueries.selectPin().executeAsOneOrNull() != null

    fun clearPin() {
        database.appDatabaseQueries.deletePin()
    }
}
