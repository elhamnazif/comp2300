package com.group8.comp2300.domain.model.medical

import kotlin.math.*

data class ClinicFilters(
    val pricingTiers: Set<PricingTier> = emptySet(),
    val serviceTypes: Set<ServiceType> = emptySet(),
    val requireLgbtqFriendly: Boolean = false,
    val requireWheelchairAccessible: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val maxDistanceKm: Double = 10.0
)

class ClinicFilterService {

    fun filterClinics(
        clinics: List<Clinic>,
        filters: ClinicFilters
    ): List<Clinic> {
        return clinics.filter { clinic ->
            applyFilters(clinic, filters)
        }.sortedBy { it.distanceKm }
    }

    private fun applyFilters(clinic: Clinic, filters: ClinicFilters): Boolean {
        // Pricing filter
        val pricingOk = filters.pricingTiers.isEmpty() ||
                (clinic.pricingTier != null && filters.pricingTiers.contains(clinic.pricingTier))

        // Service type filter
        val serviceOk = filters.serviceTypes.isEmpty() ||
                clinic.serviceTypes.any { filters.serviceTypes.contains(it) }

        // LGBTQ+ filter
        val lgbtqOk = !filters.requireLgbtqFriendly || clinic.inclusivityFlags.lgbtqFriendly

        // Wheelchair filter
        val wheelchairOk = !filters.requireWheelchairAccessible || clinic.inclusivityFlags.wheelchairAccessible

        // Location filter
        val locationOk = if (filters.locationLat != null && filters.locationLng != null) {
            val lat1 = filters.locationLat
            val lng1 = filters.locationLng
            val distance = calculateDistance(
                lat1,
                lng1,
                clinic.lat,
                clinic.lng
            )
            distance <= filters.maxDistanceKm
        } else {
            true
        }

        return pricingOk && serviceOk && lgbtqOk && wheelchairOk && locationOk
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0

        // Convert degrees to radians manually
        fun toRadians(degrees: Double): Double = degrees * PI / 180.0

        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(toRadians(lat1)) * cos(toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadiusKm * c
    }
}