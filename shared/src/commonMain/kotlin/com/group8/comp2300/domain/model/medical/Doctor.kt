package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Doctor(
    val id: String,
    val name: String,
    val role: String,
    val isOnline: Boolean,
    val nextAvailableSlot: Long, // Unix timestamp
    val imageUrl: String? = null,
    val specializations: List<String> = emptyList()
)
