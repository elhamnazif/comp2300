package com.group8.comp2300.domain.model.appointment

data class CancellationResult(
    val success: Boolean,
    val message: String,
    val refundAmount: Double? = null,
    val refundStatus: String? = null,
)
