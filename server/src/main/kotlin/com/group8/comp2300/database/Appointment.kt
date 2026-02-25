package com.group8.comp2300.database

public data class Appointment(
    public val id: String,
    public val user_id: String,
    public val title: String,
    public val appointment_time: String,
    public val appointment_type: String,
    public val clinic_id: String?,
    public val booking_id: String?,
    public val status: String?,
    public val notes: String?,
    public val reminders_enabled: Long?,
    // New payment fields
    public val payment_method: String? = null,
    public val payment_status: String? = null,
    public val payment_amount: Double? = null,
    public val transaction_id: String? = null
)