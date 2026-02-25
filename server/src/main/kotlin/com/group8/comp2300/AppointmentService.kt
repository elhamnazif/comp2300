package com.group8.comp2300

import com.group8.comp2300.database.Appointment
import com.group8.comp2300.database.AppointmentRepository
import com.group8.comp2300.database.AppointmentSlots
import java.util.UUID

interface SlotRepository {
    fun getSlotById(id: String): AppointmentSlots?
    fun updateSlotBookingStatus(id: String, isBooked: Long)
}

class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository
) {
    fun bookAppointment(
        userId: String,


        slotId: String,
        appointmentType: String,
        title: String
    ): Result<String> {

        // 1. Check if slot exists
        val slot = slotRepository.getSlotById(slotId)
            ?: return Result.failure(Exception("Slot not found"))

        // 2. Check if already booked
        if (slot.is_booked == 1L) {
            return Result.failure(Exception("Slot already booked"))
        }

        // 3. Create the appointment object
        val appointment = Appointment(
            id = UUID.randomUUID().toString(),
            user_id = userId,
            title = title,
            appointment_time = slot.start_time,
            appointment_type = appointmentType,
            clinic_id = slot.clinic_id,
            booking_id = slot.id,
            status = "BOOKED",
            notes = null,
            reminders_enabled = 0L
        )

        // 4. Save to DB
        appointmentRepository.insertAppointment(appointment)

        // 5. Update slot status
        slotRepository.updateSlotBookingStatus(slot.id, 1L)

        return Result.success("Appointment booked successfully!")
    }
}