package com.group8.comp2300.service

import com.group8.comp2300.database.Appointment
import com.group8.comp2300.database.AppointmentRepository
import com.group8.comp2300.database.AppointmentSlots
import com.group8.comp2300.database.CancellationResult  // Add this import
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

// SlotRepository interface
interface SlotRepository {
    fun getSlotById(id: String): AppointmentSlots?
    fun updateSlotBookingStatus(id: String, isBooked: Long)
    fun getSlotByBookingId(bookingId: String): AppointmentSlots?  // This will be used later
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

    // Cancel appointment method
    fun cancelAppointment(appointmentId: String, userId: String): CancellationResult {
        try {
            // 1. Get the appointment
            val appointment = appointmentRepository.getAppointmentById(appointmentId)
                ?: return CancellationResult(
                    success = false,
                    message = "Appointment not found"
                )

            // 2. Verify that this appointment belongs to the user
            if (appointment.user_id != userId) {
                return CancellationResult(
                    success = false,
                    message = "You don't have permission to cancel this appointment"
                )
            }

            // 3. Check if appointment is already cancelled
            if (appointment.status == "CANCELLED") {
                return CancellationResult(
                    success = false,
                    message = "Appointment is already cancelled"
                )
            }

            // 4. Get the associated slot
            val slot = appointment.booking_id?.let { slotRepository.getSlotById(it) }

            // 5. Calculate refund if applicable
            val refundInfo = calculateRefund(appointment)

            // 6. Update appointment status to CANCELLED
            appointmentRepository.updateAppointmentStatus(appointmentId, "CANCELLED")

            // 7. Free up the slot (set is_booked to 0)
            if (slot != null) {
                slotRepository.updateSlotBookingStatus(slot.id, 0L)
            }

            // 8. Return success with refund information
            return CancellationResult(
                success = true,
                message = buildCancellationMessage(refundInfo),
                refundAmount = refundInfo.amount,
                refundStatus = refundInfo.status
            )

        } catch (e: Exception) {
            return CancellationResult(
                success = false,
                message = "Cancellation failed: ${e.message}"
            )
        }
    }

    // Helper function to calculate refund based on cancellation timing
    private fun calculateRefund(appointment: Appointment): RefundInfo {
        val appointmentTime = LocalDateTime.parse(appointment.appointment_time,
            DateTimeFormatter.ISO_DATE_TIME)
        val now = LocalDateTime.now()
        val hoursUntilAppointment = java.time.Duration.between(now, appointmentTime).toHours()

        return when {
            // More than 24 hours before appointment: full refund
            hoursUntilAppointment >= 24 -> RefundInfo(
                amount = calculateAppointmentCost(appointment.appointment_type),
                status = "FULL_REFUND"
            )
            // Between 2-24 hours before appointment: 50% refund
            hoursUntilAppointment in 2..23 -> RefundInfo(
                amount = calculateAppointmentCost(appointment.appointment_type) * 0.5,
                status = "PARTIAL_REFUND"
            )
            // Less than 2 hours before appointment: no refund
            hoursUntilAppointment >= 0 && hoursUntilAppointment < 2 -> RefundInfo(
                amount = 0.0,
                status = "NO_REFUND"
            )
            // Appointment already passed
            else -> RefundInfo(
                amount = 0.0,
                status = "LATE_CANCELLATION"
            )
        }
    }

    // Helper function to calculate appointment cost
    private fun calculateAppointmentCost(appointmentType: String): Double {
        return when (appointmentType.lowercase()) {
            "consultation" -> 100.0
            "followup" -> 50.0
            "checkup" -> 75.0
            else -> 80.0
        }
    }

    // Helper function to build user-friendly cancellation message
    private fun buildCancellationMessage(refundInfo: RefundInfo): String {
        return when (refundInfo.status) {
            "FULL_REFUND" -> "Appointment cancelled successfully. Full refund of $${refundInfo.amount} will be processed."
            "PARTIAL_REFUND" -> "Appointment cancelled successfully. Partial refund of $${refundInfo.amount} will be processed."
            "NO_REFUND" -> "Appointment cancelled successfully. No refund applicable as cancellation is within 2 hours."
            "LATE_CANCELLATION" -> "Appointment cancelled successfully. No refund for past appointments."
            else -> "Appointment cancelled successfully."
        }
    }

    // Data class for refund information
    private data class RefundInfo(
        val amount: Double,
        val status: String
    )

    // This method can be implemented later when needed
    // fun getUserAppointments(userId: String): List<Appointment> {
    //     // Implementation here
    //     return emptyList()
    // }
}