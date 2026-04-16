package com.group8.comp2300.service

import com.group8.comp2300.domain.model.medical.*
import kotlin.test.*

class ClinicFilterServiceTest {

    private lateinit var filterService: ClinicFilterService
    private lateinit var mockClinics: List<Clinic>

    @BeforeTest
    fun setup() {
        filterService = ClinicFilterService()

        mockClinics = listOf(
            Clinic(
                id = "1",
                name = "Affordable Clinic",
                distanceKm = 2.0,
                tags = emptyList(),
                nextAvailableSlot = 0,
                lat = 40.7128,
                lng = -74.0060,
                pricingTier = PricingTier.LOW,
                serviceTypes = listOf(ServiceType.PRIMARY_CARE),
                inclusivityFlags = InclusivityFlags(lgbtqFriendly = true)
            ),
            Clinic(
                id = "2",
                name = "Premium Hospital",
                distanceKm = 5.0,
                tags = emptyList(),
                nextAvailableSlot = 0,
                lat = 40.7140,
                lng = -74.0070,
                pricingTier = PricingTier.HIGH,
                serviceTypes = listOf(ServiceType.DENTISTRY, ServiceType.PRIMARY_CARE),
                inclusivityFlags = InclusivityFlags()
            ),
            Clinic(
                id = "3",
                name = "Accessible Clinic",
                distanceKm = 8.0,
                tags = emptyList(),
                nextAvailableSlot = 0,
                lat = 40.7200,
                lng = -74.0100,
                pricingTier = PricingTier.MEDIUM,
                serviceTypes = listOf(ServiceType.MENTAL_HEALTH),
                inclusivityFlags = InclusivityFlags(wheelchairAccessible = true, lgbtqFriendly = true)
            ),
            Clinic(
                id = "4",
                name = "Far Clinic",
                distanceKm = 15.0,
                tags = emptyList(),
                nextAvailableSlot = 0,
                lat = 40.8000,
                lng = -74.1000,
                pricingTier = PricingTier.LOW,
                serviceTypes = listOf(ServiceType.PEDIATRICS),
                inclusivityFlags = InclusivityFlags()
            )
        )
    }

    @Test
    fun testFilterByPricingLow() {
        val filters = ClinicFilters(pricingTiers = setOf(PricingTier.LOW))
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(2, result.size)
        assertTrue(result.all { it.pricingTier == PricingTier.LOW })
    }

    @Test
    fun testFilterByPricingMedium() {
        val filters = ClinicFilters(pricingTiers = setOf(PricingTier.MEDIUM))
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(1, result.size)
        assertEquals("Accessible Clinic", result[0].name)
    }

    @Test
    fun testFilterByPricingHigh() {
        val filters = ClinicFilters(pricingTiers = setOf(PricingTier.HIGH))
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(1, result.size)
        assertEquals("Premium Hospital", result[0].name)
    }

    @Test
    fun testFilterByServiceType() {
        val filters = ClinicFilters(serviceTypes = setOf(ServiceType.MENTAL_HEALTH))
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(1, result.size)
        assertEquals("Accessible Clinic", result[0].name)
    }

    @Test
    fun testFilterByLgbtqFriendly() {
        val filters = ClinicFilters(requireLgbtqFriendly = true)
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(2, result.size)
        assertTrue(result.all { it.inclusivityFlags.lgbtqFriendly })
    }

    @Test
    fun testFilterByWheelchairAccessible() {
        val filters = ClinicFilters(requireWheelchairAccessible = true)
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(1, result.size)
        assertEquals("Accessible Clinic", result[0].name)
    }

    @Test
    fun testEmptyFiltersReturnsAll() {
        val filters = ClinicFilters()
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(4, result.size)
    }

    @Test
    fun testSortingByDistance() {
        val filters = ClinicFilters()
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals("Affordable Clinic", result[0].name)
        assertEquals("Premium Hospital", result[1].name)
        assertEquals("Accessible Clinic", result[2].name)
        assertEquals("Far Clinic", result[3].name)
    }

    @Test
    fun testMultipleFiltersCombined() {
        val filters = ClinicFilters(
            pricingTiers = setOf(PricingTier.LOW, PricingTier.MEDIUM),
            requireLgbtqFriendly = true
        )
        val result = filterService.filterClinics(mockClinics, filters)

        assertEquals(2, result.size)
    }
}
