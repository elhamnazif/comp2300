package com.group8.comp2300.domain.model.appointment

import com.group8.comp2300.domain.model.payment.PaymentMethod
import com.group8.comp2300.domain.model.payment.PaymentStatus

data class BookingResult(
    val appointmentId: String,
    val success: Boolean,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val amount: Double,
    val transactionId: String?,
    val message: String,
    val paymentInstructions: String
)
