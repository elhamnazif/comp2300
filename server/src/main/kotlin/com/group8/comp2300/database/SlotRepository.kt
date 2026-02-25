package com.group8.comp2300.database

import com.group8.comp2300.database.AppointmentSlots

interface SlotRepository {
    fun getSlotById(id: String): AppointmentSlots?
    fun updateSlotBookingStatus(id: String, isBooked: Long)
    fun getSlotByBookingId(bookingId: String): AppointmentSlots?  // New method to find slot by appointment ID
}
