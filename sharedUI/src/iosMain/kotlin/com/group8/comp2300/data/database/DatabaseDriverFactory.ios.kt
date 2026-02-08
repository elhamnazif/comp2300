package com.group8.comp2300.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = getDatabasePath()
        return NativeSqliteDriver(
            schema = AppDatabase.Schema,
            name = databasePath
        )
    }

    private fun getDatabasePath(): String {
        val paths =
            NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory,
                NSUserDomainMask,
                true
            )
        val documentsDirectory = paths.first() as? String ?: return "comp2300.db"
        return "$documentsDirectory/comp2300.db"
    }
}
