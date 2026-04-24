package com.group8.comp2300.data.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppDatabaseMigrationTest {
    @Test
    fun `appointment migration adds payment columns for existing installs`() {
        val dbFile = File.createTempFile("comp2300-app", ".db")
        dbFile.deleteOnExit()
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

        JdbcSqliteDriver(dbUrl).use { driver ->
            driver.execute(
                null,
                """
                CREATE TABLE AppointmentEntity (
                    id TEXT NOT NULL PRIMARY KEY,
                    userId TEXT NOT NULL,
                    title TEXT NOT NULL,
                    appointmentTime INTEGER NOT NULL,
                    appointmentType TEXT NOT NULL,
                    clinicId TEXT,
                    bookingId TEXT,
                    status TEXT NOT NULL,
                    notes TEXT,
                    hasReminder INTEGER NOT NULL DEFAULT 0,
                    paymentStatus TEXT NOT NULL DEFAULT 'PENDING'
                )
                """.trimIndent(),
                0,
                null,
            )
            driver.execute(
                null,
                """
                INSERT INTO AppointmentEntity (
                    id, userId, title, appointmentTime, appointmentType, clinicId, bookingId, status, notes, hasReminder, paymentStatus
                ) VALUES (
                    'appt-1', 'user-1', 'Test appointment', 1234, 'STI_TESTING', 'clinic-1', 'slot-1', 'PENDING', 'Bring ID', 1, 'PENDING'
                )
                """.trimIndent(),
                0,
                null,
            )
            driver.execute(null, "PRAGMA user_version = 1", 0, null)
        }

        JdbcSqliteDriver(url = dbUrl, schema = AppDatabase.Schema).use { driver ->
            val database = AppDatabase(driver)
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
