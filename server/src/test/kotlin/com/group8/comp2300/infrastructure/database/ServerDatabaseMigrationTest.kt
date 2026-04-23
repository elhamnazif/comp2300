package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.database.ServerDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerDatabaseMigrationTest {
    @Test
    fun ensureOrderTablesAddsOrderAccessForExistingDatabase() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(
            null,
            """
            CREATE TABLE UserEntity (
                id TEXT NOT NULL PRIMARY KEY
            )
            """.trimIndent(),
            0,
        )
        driver.execute(
            null,
            """
            CREATE TABLE ProductEntity (
                id TEXT NOT NULL PRIMARY KEY
            )
            """.trimIndent(),
            0,
        )

        ensureOrderTables(driver)

        driver.execute(null, "INSERT INTO UserEntity (id) VALUES (?)", 1) {
            bindString(0, "user-1")
        }
        driver.execute(null, "INSERT INTO ProductEntity (id) VALUES (?)", 1) {
            bindString(0, "product-1")
        }
        driver.execute(
            null,
            """
            INSERT INTO ShopOrderEntity (
                id, user_id, status, subtotal, tax, shipping, created_at, updated_at, shipping_address
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            9,
        ) {
            bindString(0, "order-1")
            bindString(1, "user-1")
            bindString(2, "CONFIRMED")
            bindDouble(3, 20.0)
            bindDouble(4, 0.0)
            bindDouble(5, 0.0)
            bindLong(6, 1L)
            bindLong(7, 1L)
            bindString(8, "123 Test Street")
        }

        val database = ServerDatabase(driver)
        val orders = database.orderQueries.selectShopOrdersByUserId("user-1").executeAsList()
        assertEquals(1, orders.size)
        assertEquals("order-1", orders.single().id)
    }
}
