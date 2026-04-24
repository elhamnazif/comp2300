package com.group8.comp2300.infrastructure.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.group8.comp2300.database.ServerDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerDatabaseSchemaTest {
    @Test
    fun `appointment schema stores payment fields and allows lifecycle progression`() {
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).use { driver ->
            ServerDatabase.Schema.create(driver)
            val database = ServerDatabase(driver)
            database.userQueries.insertUser(
                id = "user-1",
                email = "user-1@example.com",
                passwordHash = "hash",
                firstName = "Test",
                lastName = "User",
                phone = null,
                dateOfBirth = null,
                gender = null,
                sexualOrientation = null,
                profileImageUrl = null,
                createdAt = 1234,
                preferredLanguage = "en",
                isActivated = 1,
                deactivatedAt = null,
            )
            database.clinicQueries.insertClinic(
                id = "clinic-1",
                name = "Test Clinic",
                address = "123 Test Street",
                phone = "123456",
                latitude = 3.139,
                longitude = 101.6869,
            )
            database.appointmentSlotQueries.createApptSlot(
                id = "slot-1",
                clinic_id = "clinic-1",
                start_time = 1200,
                end_time = 1800,
                is_booked = 0,
            )
            database.appointmentQueries.insertAppointment(
                id = "appt-1",
                user_id = "user-1",
                title = "Test appointment",
                appointment_time = 1234,
                appointment_type = "STI_TESTING",
                clinic_id = "clinic-1",
                booking_id = "slot-1",
                status = "PENDING_PAYMENT",
                notes = "Bring ID",
                has_reminder = 1,
                payment_method = "VISA_4242",
                payment_status = "PAID",
                payment_amount = 35.0,
                transaction_id = "txn-1",
            )

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
