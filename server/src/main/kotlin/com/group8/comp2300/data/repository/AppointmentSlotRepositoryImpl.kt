package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.database.data.AppointmentSlotEnt
import com.group8.comp2300.domain.model.medical.AppointmentSlot
import com.group8.comp2300.domain.repository.AppointmentSlotRepository

class AppointmentSlotRepositoryImpl(private val database: ServerDatabase) : AppointmentSlotRepository {

    override fun createSlot(slot: AppointmentSlot) {
        database.appointmentSlotQueries.createApptSlot(
            id = slot.id,
            clinic_id = slot.clinicId,
            start_time = slot.startTime,
            end_time = slot.endTime,
            is_booked = if (slot.isBooked) 1L else 0L,
        )
    }

    override fun getSlotById(id: String): AppointmentSlot? = database.appointmentSlotQueries.selectSlotById(id)
        .executeAsOneOrNull()?.toDomain()

    override fun getAvailableByClinic(clinicId: String): List<AppointmentSlot> =
        database.appointmentSlotQueries.selectAvailableSlotsByClinic(clinicId)
            .executeAsList()
            .map { it.toDomain() }

    override fun updateSlotBookingStatus(id: String, isBooked: Boolean) {
        database.appointmentSlotQueries.updateSlotReservationStatus(
            is_booked = if (isBooked) 1L else 0L,
            id = id,
        )
    }

    override fun delete(id: String) {
        database.appointmentSlotQueries.deleteApptSlot(id)
    }
}

private fun AppointmentSlotEnt.toDomain() = AppointmentSlot(
    id = id,
    clinicId = clinic_id,
    startTime = start_time,
    endTime = end_time,
    isBooked = is_booked == 1L,
)
