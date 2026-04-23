package com.group8.comp2300.data.database

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): AppDatabase {
    val driver = driverFactory.createDriver()
    ensureCartTable(driver)
    return AppDatabase(driver)
}

internal fun ensureCartTable(driver: SqlDriver) {
    driver.execute(
        null,
        """
        CREATE TABLE IF NOT EXISTS CartEntity (
            userId TEXT NOT NULL,
            productId TEXT NOT NULL,
            quantity INTEGER NOT NULL,
            priceAtAdd REAL NOT NULL,
            PRIMARY KEY (userId, productId)
        )
        """.trimIndent(),
        0,
    )
}
