package com.group8.comp2300.data.repository

import com.group8.comp2300.database.Slot

interface SlotRepository {
    fun getSlotById(slotId: String): Slot?
    fun updateSlotBookingStatus(slotId: String, status: Long)
}