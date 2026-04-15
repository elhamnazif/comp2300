package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.config.Environment
import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.mock.sampleProducts

fun createServerDatabase(databasePath: String = Environment.dbPath): ServerDatabase {
    val driver = JdbcSqliteDriver(url = databasePath, schema = ServerDatabase.Schema)
    val database = ServerDatabase(driver)
    seedProducts(database)
    return database
}

private fun seedProducts(database: ServerDatabase) {
    if (database.productQueries.selectAllProducts().executeAsList().isEmpty()) {
        val repository = ProductRepositoryImpl(database)
        sampleProducts.forEach { repository.insert(it) }
    }
}
