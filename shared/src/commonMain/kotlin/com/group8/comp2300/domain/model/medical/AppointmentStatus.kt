package com.group8.comp2300.domain.model.medical

enum class AppointmentStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    CANCELLED("Cancelled"),
    PENDING_PAYMENT("Pending Payment"),
}
