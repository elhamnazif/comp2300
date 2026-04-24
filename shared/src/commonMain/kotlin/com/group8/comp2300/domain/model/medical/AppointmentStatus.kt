package com.group8.comp2300.domain.model.medical

enum class AppointmentStatus(val displayName: String) {
    PENDING_PAYMENT("Awaiting payment"),
    CONFIRMED("Confirmed"),
    CHECKED_IN("Checked in"),
    COMPLETED("Completed"),
    NO_SHOW("No-show"),
    CANCELLED("Cancelled"),
    ;

    val isTerminal: Boolean
        get() = this == COMPLETED || this == NO_SHOW || this == CANCELLED

    val isScheduled: Boolean
        get() = this == PENDING_PAYMENT || this == CONFIRMED || this == CHECKED_IN

    companion object {
        fun fromRaw(value: String?): AppointmentStatus? = entries.firstOrNull { it.name == value }
    }
}

fun Appointment.resolvedStatus(): AppointmentStatus = AppointmentStatus.fromRaw(status) ?: AppointmentStatus.CONFIRMED
