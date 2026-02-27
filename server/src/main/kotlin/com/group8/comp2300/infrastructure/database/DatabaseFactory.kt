package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.config.JwtConfig
import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.mock.sampleProducts
import com.group8.comp2300.security.PasswordHasher

fun createServerDatabase(databasePath: String = System.getenv("DB_PATH") ?: "jdbc:sqlite:vita.db"): ServerDatabase {
    val driver = JdbcSqliteDriver(url = databasePath, schema = ServerDatabase.Schema)
    val database = ServerDatabase(driver)
    seedProducts(database)
    if (JwtConfig.devAuthBypass) seedDevUser(database)
    return database
}

private fun seedProducts(database: ServerDatabase) {
    if (database.productQueries.selectAllProducts().executeAsList().isEmpty()) {
        val repository = ProductRepositoryImpl(database)
        sampleProducts.forEach { repository.insert(it) }
    }
}

private fun seedDevUser(database: ServerDatabase) {
    val repo = UserRepositoryImpl(database)
    if (repo.findById(JwtConfig.DEV_USER_ID) == null) {
        repo.insert(
            id = JwtConfig.DEV_USER_ID,
            email = "dev@vita.local",
            passwordHash = PasswordHasher.hash("devpassword1"),
            firstName = "Dev",
            lastName = "User",
            phone = null,
            dateOfBirth = null,
            gender = null,
            sexualOrientation = null,
            preferredLanguage = "en",
        )
    }
}
