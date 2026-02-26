package com.group8.comp2300.service

import com.group8.comp2300.database.AppointmentType
import com.group8.comp2300.database.PaymentMethod
import com.group8.comp2300.database.PaymentOption
import com.group8.comp2300.database.PaymentResult
import com.group8.comp2300.database.PaymentStatus
import java.util.*

interface PaymentService {
    fun validatePaymentMethod(appointmentType: String, paymentMethod: PaymentMethod): Boolean
    fun getPaymentMethodsForAppointmentType(appointmentType: String): List<PaymentMethod>
    fun requiresPrePayment(appointmentType: String): Boolean
    fun calculatePaymentAmount(appointmentType: String): Double
    fun processOnlinePayment(appointmentId: String, amount: Double): PaymentResult
    fun getPaymentInstructions(paymentMethod: PaymentMethod, appointmentType: String): String
}

class PaymentServiceImpl : PaymentService {

    private val paymentOptionsConfig = mapOf(
        "CONSULTATION" to PaymentOption(
            appointmentType = AppointmentType.CONSULTATION,
            allowedPaymentMethods = listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL, PaymentMethod.INSURANCE),
            requiresPrePayment = true,
            notes = "Online payment required for consultation bookings"
        ),
        "FOLLOWUP" to PaymentOption(
            appointmentType = AppointmentType.FOLLOWUP,
            allowedPaymentMethods = listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL),
            requiresPrePayment = false,
            notes = "Payment can be made at the clinic"
        ),
        "CHECKUP" to PaymentOption(
            appointmentType = AppointmentType.CHECKUP,
            allowedPaymentMethods = listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL, PaymentMethod.INSURANCE),
            requiresPrePayment = true,
            notes = "Pre-payment required for checkups"
        ),
        "EMERGENCY" to PaymentOption(
            appointmentType = AppointmentType.EMERGENCY,
            allowedPaymentMethods = listOf(PaymentMethod.PHYSICAL, PaymentMethod.INSURANCE),
            requiresPrePayment = false,
            notes = "Payment can be arranged upon arrival"
        ),
        "VIRTUAL_CONSULTATION" to PaymentOption(
            appointmentType = AppointmentType.VIRTUAL_CONSULTATION,
            allowedPaymentMethods = listOf(PaymentMethod.ONLINE),
            requiresPrePayment = true,
            notes = "Online payment required for virtual consultations"
        )
    )

    override fun getPaymentMethodsForAppointmentType(appointmentType: String): List<PaymentMethod> =
        paymentOptionsConfig[appointmentType.uppercase()]?.allowedPaymentMethods
            ?: listOf(PaymentMethod.ONLINE, PaymentMethod.PHYSICAL)

    override fun validatePaymentMethod(appointmentType: String, paymentMethod: PaymentMethod): Boolean {
        val allowedMethods = getPaymentMethodsForAppointmentType(appointmentType)
        return allowedMethods.contains(paymentMethod)
    }

    override fun requiresPrePayment(appointmentType: String): Boolean =
        paymentOptionsConfig[appointmentType.uppercase()]?.requiresPrePayment ?: true

    override fun calculatePaymentAmount(appointmentType: String): Double = when (appointmentType.uppercase()) {
        "CONSULTATION" -> 100.0
        "FOLLOWUP" -> 50.0
        "CHECKUP" -> 75.0
        "EMERGENCY" -> 200.0
        "VIRTUAL_CONSULTATION" -> 80.0
        "PHYSICAL_THERAPY" -> 120.0
        "LAB_TEST" -> 150.0
        else -> 80.0
    }

    override fun processOnlinePayment(appointmentId: String, amount: Double): PaymentResult {
        // Simulate payment processing
        // In a real app, this would integrate with Stripe, PayPal, etc.
        return if (amount > 0) {
            PaymentResult(
                success = true,
                transactionId = "TXN${UUID.randomUUID().toString().take(8).uppercase()}",
                message = "Payment processed successfully",
                paymentStatus = PaymentStatus.COMPLETED
            )
        } else {
            PaymentResult(
                success = false,
                transactionId = null,
                message = "Invalid payment amount",
                paymentStatus = PaymentStatus.FAILED
            )
        }
    }

    override fun getPaymentInstructions(paymentMethod: PaymentMethod, appointmentType: String): String =
        when (paymentMethod) {
            PaymentMethod.ONLINE ->
                "Please complete online payment to confirm your booking. " +
                    "A payment link will be sent to your email."

            PaymentMethod.PHYSICAL ->
                "Payment will be collected at the clinic. " +
                    "Please arrive 15 minutes before your appointment."

            PaymentMethod.INSURANCE ->
                "Insurance details will be verified. " +
                    "Please bring your insurance card to the appointment."

            PaymentMethod.PENDING -> "Please select a payment method to complete your booking."
        }
}
