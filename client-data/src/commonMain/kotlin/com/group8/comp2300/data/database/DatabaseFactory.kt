package com.group8.comp2300.data.database

import app.cash.sqldelight.db.SqlDriver
import com.group8.comp2300.`data`.database.AppDatabase

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()
    return AppDatabase(driver)
}
