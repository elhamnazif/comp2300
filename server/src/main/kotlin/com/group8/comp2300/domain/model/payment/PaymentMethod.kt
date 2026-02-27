package com.group8.comp2300.domain.model.payment

enum class PaymentMethod {
    ONLINE, // Pay online immediately
    PHYSICAL, // Pay at the clinic
    INSURANCE, // Pay through insurance
    PENDING, // Payment method not yet selected
}
