package com.group8.comp2300.domain.model.medical

data class ClinicFilters(
    val pricingTiers: Set<PricingTier> = emptySet(),
    val serviceTypes: Set<ServiceType> = emptySet(),
    val requireLgbtqFriendly: Boolean = false,
    val requireWheelchairAccessible: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val maxDistanceKm: Double = 10.0,
)
