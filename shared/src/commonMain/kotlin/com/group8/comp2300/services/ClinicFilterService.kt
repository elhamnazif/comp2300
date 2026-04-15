package com.group8.comp2300.services

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicFilters
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ClinicFilterService {
    fun filterClinics(
        clinics: List<Clinic>,
        filters: ClinicFilters = ClinicFilters(),
        searchQuery: String = "",
    ): List<Clinic> {
        val normalizedQuery = searchQuery.trim()

        return clinics
            .filter { clinic ->
                matchesSearch(clinic, normalizedQuery) &&
                    matchesStructuredFilters(clinic, filters)
            }
            .sortedBy(Clinic::distanceKm)
    }

    private fun matchesSearch(clinic: Clinic, query: String): Boolean {
        if (query.isBlank()) return true

        return clinic.name.contains(query, ignoreCase = true) ||
            clinic.address?.contains(query, ignoreCase = true) == true ||
            clinic.tags.any { it.contains(query, ignoreCase = true) }
    }

    private fun matchesStructuredFilters(clinic: Clinic, filters: ClinicFilters): Boolean {
        val pricingMatches = filters.pricingTiers.isEmpty() ||
            clinic.pricingTier in filters.pricingTiers

        val serviceMatches = filters.serviceTypes.isEmpty() ||
            clinic.serviceTypes.any(filters.serviceTypes::contains)

        val lgbtqMatches = !filters.requireLgbtqFriendly || clinic.inclusivityFlags.lgbtqFriendly
        val accessibilityMatches =
            !filters.requireWheelchairAccessible || clinic.inclusivityFlags.wheelchairAccessible

        val locationMatches = if (filters.locationLat != null && filters.locationLng != null) {
            calculateDistanceKm(
                lat1 = filters.locationLat,
                lon1 = filters.locationLng,
                lat2 = clinic.lat,
                lon2 = clinic.lng,
            ) <= filters.maxDistanceKm
        } else {
            true
        }

        return pricingMatches && serviceMatches && lgbtqMatches && accessibilityMatches && locationMatches
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0

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
