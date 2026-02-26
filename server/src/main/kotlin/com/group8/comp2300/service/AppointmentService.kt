package com.group8.comp2300.service

import com.group8.comp2300.database.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val slotRepository: SlotRepository,
    private val paymentService: PaymentService
) {

    // Updated bookAppointment method with payment
    fun bookAppointment(
        userId: String,
        slotId: String,
        appointmentType: String,
        title: String,
        paymentMethod: PaymentMethod
    ): Result<BookingResult> {

        // 1. Validate payment method for this appointment type
        if (!paymentService.validatePaymentMethod(appointmentType, paymentMethod)) {
            return Result.failure(Exception(
                "Payment method $paymentMethod is not allowed for $appointmentType appointments. " +
                        "Allowed methods: ${paymentService.getPaymentMethodsForAppointmentType(appointmentType)}"
            ))
        }

        // 2. Check if slot exists
        val slot = slotRepository.getSlotById(slotId)
            ?: return Result.failure(Exception("Slot not found"))

        // 3. Check if already booked
        if (slot.is_booked == 1L) {
            return Result.failure(Exception("Slot already booked"))
        }

        // 4. Calculate payment amount
        val paymentAmount = paymentService.calculatePaymentAmount(appointmentType)
        val requiresPrePayment = paymentService.requiresPrePayment(appointmentType)

        // 5. Process payment if online payment is selected and required
        var paymentStatus = PaymentStatus.PENDING
        var transactionId: String? = null
        var paymentMessage: String? = null

        if (paymentMethod == PaymentMethod.ONLINE && requiresPrePayment) {
            val paymentResult = paymentService.processOnlinePayment(
                appointmentId = UUID.randomUUID().toString(), // Temporary ID
                amount = paymentAmount
            )

            if (!paymentResult.success) {
                return Result.failure(Exception("Payment failed: ${paymentResult.message}"))
            }

            paymentStatus = paymentResult.paymentStatus
            transactionId = paymentResult.transactionId
            paymentMessage = paymentResult.message
        }

        // 6. Create the appointment object with payment details
        val appointment = Appointment(
            id = UUID.randomUUID().toString(),
            user_id = userId,
            title = title,
            appointment_time = slot.start_time,
            appointment_type = appointmentType,
            clinic_id = slot.clinic_id,
            booking_id = slot.id,
            status = if (paymentMethod == PaymentMethod.ONLINE && requiresPrePayment) "CONFIRMED" else "PENDING_PAYMENT",
            notes = null,
            reminders_enabled = 0L,
            payment_method = paymentMethod.name,
            payment_status = paymentStatus.name,
            payment_amount = paymentAmount,
            transaction_id = transactionId
        )

        // 7. Save to DB
        appointmentRepository.insertAppointment(appointment)

        // 8. Update slot status (only if payment successful or physical payment)
        if (paymentMethod != PaymentMethod.ONLINE || paymentStatus == PaymentStatus.COMPLETED) {
            slotRepository.updateSlotBookingStatus(slot.id, 1L)
        }

        // 9. Prepare booking result
        val bookingResult = BookingResult(
            appointmentId = appointment.id,
            success = true,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amount = paymentAmount,
            transactionId = transactionId,
            message = buildBookingMessage(paymentMethod, paymentStatus, paymentAmount),
            paymentInstructions = paymentService.getPaymentInstructions(paymentMethod, appointmentType)
        )

        return Result.success(bookingResult)
    }

    // Original cancelAppointment method
    fun cancelAppointment(appointmentId: String, userId: String): CancellationResult {
        try {
            val appointment = appointmentRepository.getAppointmentById(appointmentId)
                ?: return CancellationResult(
                    success = false,
                    message = "Appointment not found"
                )

            if (appointment.user_id != userId) {
                return CancellationResult(
                    success = false,
                    message = "You don't have permission to cancel this appointment"
                )
            }

            if (appointment.status == "CANCELLED") {
                return CancellationResult(
                    success = false,
                    message = "Appointment is already cancelled"
                )
            }

            val slot = appointment.booking_id?.let { slotRepository.getSlotById(it) }
            val refundInfo = calculateRefund(appointment)

            appointmentRepository.updateAppointmentStatus(appointmentId, "CANCELLED")

            if (slot != null) {
                slotRepository.updateSlotBookingStatus(slot.id, 0L)
            }

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

    private fun calculateRefund(appointment: Appointment): RefundInfo {
        val appointmentTime = LocalDateTime.parse(appointment.appointment_time,
            DateTimeFormatter.ISO_DATE_TIME)
        val now = LocalDateTime.now()
        val hoursUntilAppointment = java.time.Duration.between(now, appointmentTime).toHours()

        return when {
            hoursUntilAppointment >= 24 -> RefundInfo(
                amount = appointment.payment_amount ?: 0.0,
                status = "FULL_REFUND"
            )
            hoursUntilAppointment in 2..23 -> RefundInfo(
                amount = (appointment.payment_amount ?: 0.0) * 0.5,
                status = "PARTIAL_REFUND"
            )
            hoursUntilAppointment >= 0 && hoursUntilAppointment < 2 -> RefundInfo(
                amount = 0.0,
                status = "NO_REFUND"
            )
            else -> RefundInfo(
                amount = 0.0,
                status = "LATE_CANCELLATION"
            )
        }
    }

    private fun buildBookingMessage(
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        amount: Double
    ): String {
        return when (paymentMethod) {
            PaymentMethod.ONLINE -> when (paymentStatus) {
                PaymentStatus.COMPLETED -> "Booking confirmed! Payment of $$amount processed successfully."
                PaymentStatus.PENDING -> "Booking created. Please complete the payment of $$amount to confirm."
                else -> "Booking created. Payment status: $paymentStatus"
            }
            PaymentMethod.PHYSICAL -> "Booking confirmed! Please pay $$amount at the clinic."
            PaymentMethod.INSURANCE -> "Booking confirmed! Insurance will be billed for $$amount."
            PaymentMethod.PENDING -> "Booking created. Please select a payment method."
        }
    }

    private fun buildCancellationMessage(refundInfo: RefundInfo): String {
        return when (refundInfo.status) {
            "FULL_REFUND" -> "Appointment cancelled successfully. Full refund of $${refundInfo.amount} will be processed."
            "PARTIAL_REFUND" -> "Appointment cancelled successfully. Partial refund of $${refundInfo.amount} will be processed."
            "NO_REFUND" -> "Appointment cancelled successfully. No refund applicable as cancellation is within 2 hours."
            "LATE_CANCELLATION" -> "Appointment cancelled successfully. No refund for past appointments."
            else -> "Appointment cancelled successfully."
        }
    }

    // Helper function to get available payment methods
    fun getAvailablePaymentMethods(appointmentType: String): List<PaymentMethod> {
        return paymentService.getPaymentMethodsForAppointmentType(appointmentType)
    }

    // Helper function to update payment method
    fun updateAppointmentPaymentMethod(
        appointmentId: String,
        userId: String,
        newPaymentMethod: PaymentMethod
    ): Result<String> {
        val appointment = appointmentRepository.getAppointmentById(appointmentId)
            ?: return Result.failure(Exception("Appointment not found"))

        if (appointment.user_id != userId) {
            return Result.failure(Exception("Unauthorized to modify this appointment"))
        }

        if (!paymentService.validatePaymentMethod(appointment.appointment_type ?: "", newPaymentMethod)) {
            return Result.failure(Exception("Invalid payment method for this appointment type"))
        }

        appointmentRepository.updatePaymentDetails(
            id = appointmentId,
            paymentMethod = newPaymentMethod.name,
            paymentStatus = PaymentStatus.PENDING.name,
            transactionId = null
        )

        return Result.success("Payment method updated to $newPaymentMethod")
    }

    private data class RefundInfo(
        val amount: Double,
        val status: String
    )
}