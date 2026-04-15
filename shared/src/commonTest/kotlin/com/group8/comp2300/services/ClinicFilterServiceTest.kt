package com.group8.comp2300.services

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.ClinicFilters
import com.group8.comp2300.domain.model.medical.InclusivityFlags
import com.group8.comp2300.domain.model.medical.PricingTier
import com.group8.comp2300.domain.model.medical.ServiceType
import kotlin.test.Test
import kotlin.test.assertEquals

class ClinicFilterServiceTest {
    private val service = ClinicFilterService()

    private val clinics = listOf(
        Clinic(
            id = "nearby-primary",
            name = "Nearby Health",
            distanceKm = 1.2,
            tags = listOf("prep", "testing"),
            nextAvailableSlot = 0L,
            lat = -35.2809,
            lng = 149.13,
            address = "City Centre",
            pricingTier = PricingTier.LOW,
            serviceTypes = listOf(ServiceType.PRIMARY_CARE),
            inclusivityFlags = InclusivityFlags(lgbtqFriendly = true, wheelchairAccessible = true),
        ),
        Clinic(
            id = "dental",
            name = "Dental Corner",
            distanceKm = 4.5,
            tags = listOf("cleaning"),
            nextAvailableSlot = 0L,
            lat = -35.29,
            lng = 149.15,
            address = "Northside",
            pricingTier = PricingTier.MEDIUM,
            serviceTypes = listOf(ServiceType.DENTISTRY),
            inclusivityFlags = InclusivityFlags(lgbtqFriendly = false, wheelchairAccessible = true),
        ),
        Clinic(
            id = "mental-health",
            name = "Calm Minds",
            distanceKm = 2.4,
            tags = listOf("therapy"),
            nextAvailableSlot = 0L,
            lat = -35.3005,
            lng = 149.12,
            address = "Southside",
            pricingTier = PricingTier.HIGH,
            serviceTypes = listOf(ServiceType.MENTAL_HEALTH),
            inclusivityFlags = InclusivityFlags(lgbtqFriendly = true, wheelchairAccessible = false),
        ),
    )

    @Test
    fun filtersBySearchAndSortsByDistance() {
        val results = service.filterClinics(
            clinics = clinics,
            searchQuery = "health",
        )

        assertEquals(listOf("nearby-primary", "mental-health"), results.map(Clinic::id))
    }

    @Test
    fun filtersByStructuredCriteria() {
        val results = service.filterClinics(
            clinics = clinics,
            filters = ClinicFilters(
                pricingTiers = setOf(PricingTier.LOW, PricingTier.MEDIUM),
                serviceTypes = setOf(ServiceType.PRIMARY_CARE),
                requireLgbtqFriendly = true,
                requireWheelchairAccessible = true,
            ),
        )

        assertEquals(listOf("nearby-primary"), results.map(Clinic::id))
    }

    @Test
    fun filtersByDistanceWhenLocationProvided() {
        val results = service.filterClinics(
            clinics = clinics,
            filters = ClinicFilters(
                locationLat = -35.2809,
                locationLng = 149.13,
                maxDistanceKm = 2.0,
            ),
        )

        assertEquals(listOf("nearby-primary"), results.map(Clinic::id))
    }
}
