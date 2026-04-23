package com.group8.comp2300.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals

class AppDatabaseMigrationTest {
    @Test
    fun ensureCartTableAddsCartAccessForExistingDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE ProductEntity (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                price REAL NOT NULL,
                category TEXT NOT NULL,
                insuranceCovered INTEGER NOT NULL DEFAULT 0,
                imageUrl TEXT
            )
            """.trimIndent(),
            0,
        )

        ensureCartTable(driver)

        driver.execute(null, "INSERT INTO CartEntity (userId, productId, quantity, priceAtAdd) VALUES (?, ?, ?, ?)", 4) {
            bindString(0, "user-1")
            bindString(1, "product-1")
            bindLong(2, 2)
            bindDouble(3, 19.99)
        }

        val database = AppDatabase(driver)
        val cartItems = database.appDatabaseQueries.selectCartItemsByUserId("user-1").executeAsList()
        assertEquals(1, cartItems.size)
        assertEquals("product-1", cartItems.single().productId)
    }
}
