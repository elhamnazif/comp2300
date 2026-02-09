package com.group8.comp2300.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.data.repository.ProductRepository
import com.group8.comp2300.mock.sampleProducts

fun createServerDatabase(databasePath: String = "jdbc:sqlite:vita.db"): ServerDatabase {
    val driver = JdbcSqliteDriver(url = databasePath, schema = ServerDatabase.Schema)
    val database = ServerDatabase(driver)
    seedProducts(database)
    return database
}

private fun seedProducts(database: ServerDatabase) {
    if (database.serverDatabaseQueries.selectAllProducts().executeAsList().isEmpty()) {
        val repository = ProductRepository(database)
        sampleProducts.forEach { repository.insert(it) }
    }
}
