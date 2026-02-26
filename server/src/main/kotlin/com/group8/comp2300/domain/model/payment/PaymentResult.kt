package com.group8.comp2300.domain.model.payment

data class PaymentResult(
    val success: Boolean,
    val transactionId: String?,
    val message: String,
    val paymentStatus: PaymentStatus
)
