package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Clinic(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val tags: List<String>,
    val nextAvailableSlot: Long,
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val phone: String? = null,
    val imageUrl: String? = null,
    val pricingTier: PricingTier? = null,
    val serviceTypes: List<ServiceType> = emptyList(),
    val inclusivityFlags: InclusivityFlags = InclusivityFlags(),
) {
    val formattedDistance: String
        get() = if (distanceKm < 1) {
            "${(distanceKm * 1000).toInt()}m"
        } else {
            val rounded = (distanceKm * 10).toInt() / 10.0
            "${rounded}km"
        }
}

enum class PricingTier {
    LOW,
    MEDIUM,
    HIGH,
    ;

    fun displayIcon() = when (this) {
        LOW -> "$"
        MEDIUM -> "$$"
        HIGH -> "$$$"
    }
}

enum class ServiceType {
    PRIMARY_CARE,
    DENTISTRY,
    MENTAL_HEALTH,
    PEDIATRICS,
    OBGYN,
    ;

    fun displayName() = when (this) {
        PRIMARY_CARE -> "Primary Care"
        DENTISTRY -> "Dentistry"
        MENTAL_HEALTH -> "Mental Health"
        PEDIATRICS -> "Pediatrics"
        OBGYN -> "OB/GYN"
    }
}

@Serializable
data class InclusivityFlags(val lgbtqFriendly: Boolean = false, val wheelchairAccessible: Boolean = false)
