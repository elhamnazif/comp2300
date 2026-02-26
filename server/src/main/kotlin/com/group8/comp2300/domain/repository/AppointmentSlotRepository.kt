package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.AppointmentSlot

interface AppointmentSlotRepository {
    fun getSlotById(id: String): AppointmentSlot?
    fun updateSlotBookingStatus(id: String, isBooked: Boolean)
    fun getAvailableByClinic(clinicId: String): List<AppointmentSlot>
    fun createSlot(slot: AppointmentSlot)
    fun delete(id: String)
}