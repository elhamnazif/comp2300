package com.group8.comp2300.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = JdbcSqliteDriver(
        url = "jdbc:sqlite::memory:",
        schema = AppDatabase.Schema
    )
}
