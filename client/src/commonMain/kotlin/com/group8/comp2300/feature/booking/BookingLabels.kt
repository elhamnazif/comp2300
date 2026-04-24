package com.group8.comp2300.feature.booking

import com.group8.comp2300.domain.model.medical.AppointmentStatus
import com.group8.comp2300.domain.model.medical.BookingPaymentMethod

internal fun appointmentTypeLabel(value: String): String = when (value) {
    "STI_TESTING" -> "STI testing"
    "SYMPTOMS" -> "Symptoms"
    "CONTRACEPTION" -> "Contraception"
    "PREP_PEP" -> "PrEP / PEP"
    "FOLLOW_UP" -> "Follow-up"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

internal fun appointmentStatusLabel(value: String): String = AppointmentStatus.fromRaw(value)?.displayName
    ?: value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)

internal fun paymentStatusLabel(value: String?): String = when (value) {
    null, "" -> "Pending"
    "PAID" -> "Paid"
    "PENDING" -> "Pending"
    else -> value.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

internal fun paymentMethodLabel(value: String?): String = BookingPaymentMethod.fromRaw(value)?.displayLabel
    ?: value?.replace('_', ' ')?.lowercase()?.replaceFirstChar(Char::uppercase)
    ?: "Not selected"
