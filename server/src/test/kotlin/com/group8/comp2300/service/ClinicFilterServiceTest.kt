package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.PricingTier
import com.group8.comp2300.domain.model.medical.ServiceType
import kotlin.math.*

data class ClinicFilters(
    val pricingTiers: Set<PricingTier> = emptySet(),
    val serviceTypes: Set<ServiceType> = emptySet(),
    val requireLgbtqFriendly: Boolean = false,
    val requireWheelchairAccessible: Boolean = false,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val maxDistanceKm: Double = 10.0,
    val searchQuery: String? = null,
)

class ClinicFilterService {

    fun filterClinics(clinics: List<Clinic>, filters: ClinicFilters = ClinicFilters()): List<Clinic> =
        clinics.filter { clinic ->
            applyFilters(clinic, filters)
        }.sortedBy { it.distanceKm }

    // Overload for direct searchQuery (used by test)
    fun filterClinics(clinics: List<Clinic>, searchQuery: String?): List<Clinic> {
        val filters = if (!searchQuery.isNullOrBlank()) {
            ClinicFilters(searchQuery = searchQuery)
        } else {
            ClinicFilters()
        }
        return filterClinics(clinics, filters)
    }

    private fun applyFilters(clinic: Clinic, filters: ClinicFilters): Boolean {
        // Search filter – includes service type display name
        val searchOk = if (!filters.searchQuery.isNullOrBlank()) {
            val query = filters.searchQuery.lowercase()
            clinic.name.lowercase().contains(query) ||
                clinic.tags.any { it.lowercase().contains(query) } ||
                clinic.serviceTypes.any { serviceType ->
                    serviceType.name.lowercase().contains(query) ||
                        serviceType.displayName().lowercase().contains(query)
                }
        } else {
            true
        }

        val pricingOk = filters.pricingTiers.isEmpty() ||
            (clinic.pricingTier != null && filters.pricingTiers.contains(clinic.pricingTier))

        val serviceOk = filters.serviceTypes.isEmpty() ||
            clinic.serviceTypes.any { filters.serviceTypes.contains(it) }

        val lgbtqOk = !filters.requireLgbtqFriendly || clinic.inclusivityFlags.lgbtqFriendly

        val wheelchairOk = !filters.requireWheelchairAccessible || clinic.inclusivityFlags.wheelchairAccessible

        val locationOk = if (filters.locationLat != null && filters.locationLng != null) {
            val distance = calculateDistance(
                filters.locationLat,
                filters.locationLng,
                clinic.lat,
                clinic.lng,
            )
            distance <= filters.maxDistanceKm
        } else {
            true
        }

        return searchOk && pricingOk && serviceOk && lgbtqOk && wheelchairOk && locationOk
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        fun toRadians(deg: Double) = deg * PI / 180.0
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(toRadians(lat1)) * cos(toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }
}
