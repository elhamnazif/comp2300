package com.group8.comp2300.domain.model.payment

data class PaymentDetails(
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val amount: Double,
    val transactionId: String? = null,
    val paymentTime: String? = null,
    val requiresPrePayment: Boolean = true
)
