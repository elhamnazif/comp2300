package com.group8.comp2300.domain.model.payment

import com.group8.comp2300.domain.model.appointment.AppointmentType

data class PaymentOption(
    val appointmentType: AppointmentType,
    val allowedPaymentMethods: List<PaymentMethod>,
    val requiresPrePayment: Boolean,
    val notes: String? = null,
)
