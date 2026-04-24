package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.database.ServerDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerDatabaseMigrationTest {
    @Test
    fun `appointment migration preserves legacy rows and allows lifecycle progression`() {
        val dbFile = File.createTempFile("comp2300-server", ".db")
        dbFile.deleteOnExit()
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"

        JdbcSqliteDriver(dbUrl).use { driver ->
            driver.execute(null, "CREATE TABLE UserEntity (id TEXT PRIMARY KEY)", 0, null)
            driver.execute(null, "CREATE TABLE ClinicEntity (id TEXT PRIMARY KEY)", 0, null)
            driver.execute(null, "CREATE TABLE AppointmentSlotEnt (id TEXT PRIMARY KEY)", 0, null)
            driver.execute(
                null,
                """
                CREATE TABLE AppointmentEntity (
                    id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES UserEntity(id) ON DELETE CASCADE,
                    title TEXT NOT NULL,
                    appointment_time INTEGER NOT NULL,
                    appointment_type TEXT NOT NULL CHECK (
                        appointment_type IN (
                            'CHECKUP',
                            'CONSULTATION',
                            'SCREENING',
                            'FOLLOW_UP',
                            'VACCINATION',
                            'OTHER',
                            'FOLLOWUP',
                            'EMERGENCY',
                            'VIRTUAL_CONSULTATION',
                            'PHYSICAL_THERAPY',
                            'LAB_TEST',
                            'STI_TESTING',
                            'SYMPTOMS',
                            'CONTRACEPTION',
                            'PREP_PEP'
                        )
                    ),
                    clinic_id TEXT REFERENCES ClinicEntity(id) ON DELETE SET NULL,
                    booking_id TEXT REFERENCES AppointmentSlotEnt(id) ON DELETE SET NULL,
                    status TEXT DEFAULT 'CONFIRMED' CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'PENDING_PAYMENT')),
                    notes TEXT,
                    has_reminder INTEGER DEFAULT 1 CHECK (has_reminder IN (0, 1)),
                    payment_method TEXT,
                    payment_status TEXT,
                    payment_amount REAL,
                    transaction_id TEXT
                )
                """.trimIndent(),
                0,
                null,
            )
            driver.execute(null, "INSERT INTO UserEntity (id) VALUES ('user-1')", 0, null)
            driver.execute(null, "INSERT INTO ClinicEntity (id) VALUES ('clinic-1')", 0, null)
            driver.execute(null, "INSERT INTO AppointmentSlotEnt (id) VALUES ('slot-1')", 0, null)
            driver.execute(
                null,
                """
                INSERT INTO AppointmentEntity (
                    id, user_id, title, appointment_time, appointment_type, clinic_id, booking_id, status, notes, has_reminder,
                    payment_method, payment_status, payment_amount, transaction_id
                ) VALUES (
                    'appt-1', 'user-1', 'Test appointment', 1234, 'STI_TESTING', 'clinic-1', 'slot-1', 'PENDING',
                    'Bring ID', 1, 'VISA_4242', 'PAID', 35.0, 'txn-1'
                )
                """.trimIndent(),
                0,
                null,
            )
            driver.execute(null, "PRAGMA user_version = 1", 0, null)
        }

        JdbcSqliteDriver(url = dbUrl, schema = ServerDatabase.Schema).use { driver ->
            val database = ServerDatabase(driver)
            val migrated = database.appointmentQueries.selectAppointmentById("appt-1").executeAsOne()

            assertEquals("PENDING_PAYMENT", migrated.status)
            assertEquals(35.0, migrated.payment_amount)

            database.appointmentQueries.updateAppointmentStatus(status = "CHECKED_IN", id = "appt-1")
            database.appointmentQueries.updateAppointmentStatus(status = "COMPLETED", id = "appt-1")

            val progressed = database.appointmentQueries.selectAppointmentById("appt-1").executeAsOne()
            assertEquals("COMPLETED", progressed.status)
        }
    }
}
