package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class BookingPaymentMethod(val displayLabel: String) {
    VISA_4242("Visa ending 4242"),
    MASTERCARD_1881("Mastercard ending 1881"),
    DIGITAL_WALLET("Digital wallet"),
    ;

    companion object {
        fun fromRaw(value: String?): BookingPaymentMethod? = entries.firstOrNull { it.name == value }
    }
}

fun consultationFeeFor(pricingTier: PricingTier?): Double = when (pricingTier) {
    PricingTier.LOW -> 25.0
    PricingTier.MEDIUM -> 45.0
    PricingTier.HIGH -> 65.0
    null -> 35.0
}

fun Clinic.bookingConsultationFee(): Double = consultationFeeFor(pricingTier)
