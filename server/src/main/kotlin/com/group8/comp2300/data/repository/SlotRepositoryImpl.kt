package com.group8.comp2300.data.repository

import com.group8.comp2300.database.AppointmentSlots
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.repository.SlotRepository

class SlotRepositoryImpl(private val database: ServerDatabase) : SlotRepository {

    override fun getSlotById(id: String): AppointmentSlots? =
        database.serverDatabaseQueries.selectSlotById(id).executeAsOneOrNull()

    override fun updateSlotBookingStatus(id: String, isBooked: Long) {
        database.serverDatabaseQueries.updateSlotBookingStatus(isBooked, id)
    }

    override fun getSlotByBookingId(bookingId: String): AppointmentSlots? =
        database.serverDatabaseQueries.selectSlotByBookingId(bookingId).executeAsOneOrNull()
}
