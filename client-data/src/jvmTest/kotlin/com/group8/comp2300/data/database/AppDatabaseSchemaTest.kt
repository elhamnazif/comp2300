package com.group8.comp2300.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDatabaseSchemaTest {
    @Test
    fun `appointment schema stores payment fields and current status values`() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            AppDatabase.Schema.create(driver)
            val database = AppDatabase(driver)
            database.appDatabaseQueries.insertAppointment(
                id = "appt-1",
                userId = "user-1",
                title = "Test appointment",
                appointmentTime = 1234,
                appointmentType = "STI_TESTING",
                clinicId = "clinic-1",
                bookingId = "slot-1",
                status = "PENDING_PAYMENT",
                notes = "Bring ID",
                hasReminder = 1,
                paymentStatus = "PENDING",
                paymentMethod = null,
                paymentAmount = null,
                transactionId = null,
            )

            val migratedRow = database.appDatabaseQueries.selectAllAppointments().executeAsOne()

            assertEquals("PENDING_PAYMENT", migratedRow.status)
            assertNull(migratedRow.paymentMethod)
            assertNull(migratedRow.paymentAmount)
            assertNull(migratedRow.transactionId)

            database.appDatabaseQueries.insertAppointment(
                id = "appt-2",
                userId = "user-1",
                title = "Paid appointment",
                appointmentTime = 5678,
                appointmentType = "FOLLOW_UP",
                clinicId = "clinic-1",
                bookingId = "slot-2",
                status = "CONFIRMED",
                notes = "Bring report",
                hasReminder = 1,
                paymentStatus = "PAID",
                paymentMethod = "VISA_4242",
                paymentAmount = 45.0,
                transactionId = "txn-1",
            )

            val inserted = database.appDatabaseQueries.selectAllAppointments().executeAsList()
                .first { it.id == "appt-2" }

            assertEquals("VISA_4242", inserted.paymentMethod)
            assertEquals(45.0, inserted.paymentAmount)
            assertEquals("txn-1", inserted.transactionId)
        }
    }
}
