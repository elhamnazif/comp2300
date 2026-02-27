package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.Doctor
import kotlin.time.Clock

// Helper to create timestamps (hours from now)
private fun hoursFromNow(hours: Int): Long = Clock.System.now().toEpochMilliseconds() + (hours * 60 * 60 * 1000L)

val sampleClinics =
    listOf(
        Clinic(
            id = "1",
            name = "Vita Central KL",
            distanceKm = 0.8,
            tags = listOf("LGBTQ+ Friendly", "Free Testing"),
            nextAvailableSlot = hoursFromNow(2),
            lat = 3.1390,
            lng = 101.6869,
            address = "123 Jalan Bukit Bintang, Kuala Lumpur",
            phone = "+60 3-1234-5678",
        ),
        Clinic(
            id = "2",
            name = "Universiti Malaya Medical",
            distanceKm = 2.4,
            tags = listOf("Walk-ins", "PrEP Avail"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 3.1424,
            lng = 101.6827,
            address = "Universiti Malaya Campus, Petaling Jaya",
        ),
        Clinic(
            id = "3",
            name = "Community Health Hub",
            distanceKm = 5.1,
            tags = listOf("Anonymous", "Late Hours"),
            nextAvailableSlot = hoursFromNow(6),
            lat = 3.1400,
            lng = 101.6850,
        ),
    )

val sampleDoctors =
    listOf(
        Doctor(
            id = "d1",
            name = "Dr. Sarah Lee",
            role = "Sexual Health Specialist",
            isOnline = true,
            nextAvailableSlot =
            Clock.System.now().toEpochMilliseconds(), // Available now
            specializations = listOf("HIV/AIDS", "PrEP", "STI Testing"),
        ),
        Doctor(
            id = "d2",
            name = "Dr. Azman Shah",
            role = "Infectious Disease",
            isOnline = false,
            nextAvailableSlot = hoursFromNow(4),
            specializations = listOf("Hepatitis", "HIV Treatment"),
        ),
        Doctor(
            id = "d3",
            name = "Nurse Alex",
            role = "PrEP Counselor",
            isOnline = true,
            nextAvailableSlot = Clock.System.now().toEpochMilliseconds(),
        ),
    )
