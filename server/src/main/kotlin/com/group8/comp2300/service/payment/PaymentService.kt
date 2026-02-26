package com.group8.comp2300.service.payment

import com.group8.comp2300.domain.model.payment.PaymentMethod
import com.group8.comp2300.domain.model.payment.PaymentResult

interface PaymentService {
    fun validatePaymentMethod(appointmentType: String, paymentMethod: PaymentMethod): Boolean
    fun getPaymentMethodsForAppointmentType(appointmentType: String): List<PaymentMethod>
    fun requiresPrePayment(appointmentType: String): Boolean
    fun calculatePaymentAmount(appointmentType: String): Double
    fun processOnlinePayment(appointmentId: String, amount: Double): PaymentResult
    fun getPaymentInstructions(paymentMethod: PaymentMethod, appointmentType: String): String
}
