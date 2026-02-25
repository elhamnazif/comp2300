package com.group8.comp2300.database

enum class PaymentMethod {
    ONLINE,      // Pay online immediately
    PHYSICAL,    // Pay at the clinic
    INSURANCE,   // Pay through insurance
    PENDING      // Payment method not yet selected
}

enum class PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    REFUNDED,
    WAIVED
}

enum class AppointmentType {
    CONSULTATION,
    FOLLOWUP,
    CHECKUP,
    EMERGENCY,
    VIRTUAL_CONSULTATION,
    PHYSICAL_THERAPY,
    LAB_TEST
}

data class PaymentDetails(
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val amount: Double,
    val transactionId: String? = null,
    val paymentTime: String? = null,
    val requiresPrePayment: Boolean = true
)

data class PaymentOption(
    val appointmentType: AppointmentType,
    val allowedPaymentMethods: List<PaymentMethod>,
    val requiresPrePayment: Boolean,
    val notes: String? = null
)

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

data class PaymentResult(
    val success: Boolean,
    val transactionId: String?,
    val message: String,
    val paymentStatus: PaymentStatus
)