package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.Clinic
import com.group8.comp2300.domain.model.medical.Doctor
import kotlin.time.Clock

// Helper to create timestamps (hours from now)
private fun hoursFromNow(hours: Int): Long = Clock.System.now().toEpochMilliseconds() + (hours * 60 * 60 * 1000L)

private val clinicImageNames = listOf(
    "clinic_photo_medical_center_glass.jpg",
    "clinic_photo_frontage_night.jpg",
    "clinic_photo_waiting_room.jpg",
    "clinic_photo_exam_room.jpg",
    "clinic_photo_treatment_room.jpg",
)

private fun clinicImageUrl(index: Int): String = "/images/${clinicImageNames[index % clinicImageNames.size]}"

private val rawSampleClinics =
    listOf(
        // Kuala Lumpur Area
        Clinic(
            id = "clinic-001",
            name = "Klinik Kesihatan Medan Tuanku",
            distanceKm = 2.1,
            tags = listOf("general", "women health"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 3.1478,
            lng = 101.6894,
            address = "No. 12, Jalan Tuanku Abdul Rahman, Kuala Lumpur 50300, Malaysia",
            phone = "+60 3-2698 1234",
        ),
        Clinic(
            id = "clinic-002",
            name = "Klinik Seri Puteri Bangsar",
            distanceKm = 3.4,
            tags = listOf("general", "children"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 3.1150,
            lng = 101.6773,
            address = "No. 78, Jalan Bangsar Utama 1, Bangsar 59000, Kuala Lumpur, Malaysia",
            phone = "+60 3-2287 4567",
        ),
        Clinic(
            id = "clinic-003",
            name = "Titiwangsa Health Centre",
            distanceKm = 4.2,
            tags = listOf("general", "dental"),
            nextAvailableSlot = hoursFromNow(72),
            lat = 3.1692,
            lng = 101.6950,
            address = "B-2-15, Jalan Titiwangsa 3, Titiwangsa 53200, Kuala Lumpur, Malaysia",
            phone = "+60 3-4023 8901",
        ),
        Clinic(
            id = "clinic-004",
            name = "Klinik Mega Cheras",
            distanceKm = 5.8,
            tags = listOf("general", "skin"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 3.1085,
            lng = 101.7315,
            address = "No. 55, Jalan Cheras Selintang, Cheras 56100, Kuala Lumpur, Malaysia",
            phone = "+60 3-9100 3456",
        ),
        Clinic(
            id = "clinic-005",
            name = "Mont Kiara Medical Centre",
            distanceKm = 6.1,
            tags = listOf("general", "dental", "skin"),
            nextAvailableSlot = hoursFromNow(96),
            lat = 3.1625,
            lng = 101.6540,
            address = "No. 8, Jalan Solaris, Mont Kiara 50480, Kuala Lumpur, Malaysia",
            phone = "+60 3-6203 7890",
        ),
        // Petaling Jaya Area
        Clinic(
            id = "clinic-006",
            name = "Petaling Jaya Wellness Clinic",
            distanceKm = 2.8,
            tags = listOf("general", "women health"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 3.1023,
            lng = 101.5921,
            address = "No. 30, Jalan PJU 1A/1, Amcorp Mall, Petaling Jaya 47301, Selangor, Malaysia",
            phone = "+60 3-7876 2345",
        ),
        Clinic(
            id = "clinic-007",
            name = "Klinik Damansara Heights",
            distanceKm = 3.5,
            tags = listOf("general", "dental"),
            nextAvailableSlot = hoursFromNow(72),
            lat = 3.1355,
            lng = 101.6285,
            address = "No. 22, Jalan Damansara Endah, Damansara Heights 50490, Kuala Lumpur, Malaysia",
            phone = "+60 3-2095 6789",
        ),
        Clinic(
            id = "clinic-008",
            name = "SS2 Family Health Clinic",
            distanceKm = 4.0,
            tags = listOf("general", "children"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 3.1185,
            lng = 101.6156,
            address = "No. 67, Jalan SS 2/60, Petaling Jaya 47300, Selangor, Malaysia",
            phone = "+60 3-7877 8901",
        ),
        Clinic(
            id = "clinic-009",
            name = "Bandar Sunway Medical Centre",
            distanceKm = 5.2,
            tags = listOf("general", "dental", "skin", "women health"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 3.0725,
            lng = 101.6075,
            address = "No. 5, Jalan PJS 11/2, Bandar Sunway 46150, Petaling Jaya, Selangor, Malaysia",
            phone = "+60 3-5637 1234",
        ),
        Clinic(
            id = "clinic-010",
            name = "Klinik Kelana Jaya",
            distanceKm = 6.3,
            tags = listOf("general", "women health"),
            nextAvailableSlot = hoursFromNow(96),
            lat = 3.0885,
            lng = 101.6025,
            address = "No. 14, Jalan SS 3/56, Kelana Jaya 47301, Petaling Jaya, Selangor, Malaysia",
            phone = "+60 3-7803 4567",
        ),
        // Shah Alam / Subang Area
        Clinic(
            id = "clinic-011",
            name = "Shah Alam City Medical Centre",
            distanceKm = 7.1,
            tags = listOf("general", "dental", "children"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 3.0652,
            lng = 101.5175,
            address = "No. 18, Jalan Nekmat 7, Seksyen 7, Shah Alam 40000, Selangor, Malaysia",
            phone = "+60 3-5512 7890",
        ),
        Clinic(
            id = "clinic-012",
            name = "Klinik Subang Jaya",
            distanceKm = 5.5,
            tags = listOf("general", "skin"),
            nextAvailableSlot = hoursFromNow(72),
            lat = 3.0795,
            lng = 101.5862,
            address = "No. 42, Jalan SS 15/4D, Subang Jaya 47500, Selangor, Malaysia",
            phone = "+60 3-5634 2345",
        ),
        Clinic(
            id = "clinic-013",
            name = "USJ Taipan Health Clinic",
            distanceKm = 6.8,
            tags = listOf("general", "dental", "women health"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 3.0468,
            lng = 101.5885,
            address = "No. 88, Jalan USJ 12-2, USJ 12 47600, Subang Jaya, Selangor, Malaysia",
            phone = "+60 3-8023 6789",
        ),
        Clinic(
            id = "clinic-014",
            name = "Kota Kemuning Medical Centre",
            distanceKm = 8.2,
            tags = listOf("general", "children"),
            nextAvailableSlot = hoursFromNow(96),
            lat = 3.0105,
            lng = 101.5495,
            address = "No. 6, Jalan Kota Kemuning 1, Kota Kemuning 40400, Shah Alam, Selangor, Malaysia",
            phone = "+60 3-5123 4567",
        ),
        // Penang
        Clinic(
            id = "clinic-015",
            name = "Georgetown Family Clinic",
            distanceKm = 3.2,
            tags = listOf("general", "dental"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 5.4185,
            lng = 100.3325,
            address = "No. 25, Jalan Magazine, George Town 10300, Penang, Malaysia",
            phone = "+60 4-262 7890",
        ),
        Clinic(
            id = "clinic-016",
            name = "Batu Ferringhi Wellness Centre",
            distanceKm = 5.4,
            tags = listOf("general", "skin", "women health"),
            nextAvailableSlot = hoursFromNow(72),
            lat = 5.4275,
            lng = 100.2945,
            address = "No. 112, Jalan Batu Ferringhi, Batu Ferringhi 11100, Penang, Malaysia",
            phone = "+60 4-881 2345",
        ),
        // Johor Bahru
        Clinic(
            id = "clinic-017",
            name = "Johor Bahru City Medical Centre",
            distanceKm = 2.6,
            tags = listOf("general", "dental", "children"),
            nextAvailableSlot = hoursFromNow(24),
            lat = 1.4975,
            lng = 103.7445,
            address = "No. 33, Jalan Wong Ah Fook, Johor Bahru 80000, Johor, Malaysia",
            phone = "+60 7-222 3456",
        ),
        Clinic(
            id = "clinic-018",
            name = "Skudai Klinik Kesihatan",
            distanceKm = 4.8,
            tags = listOf("general", "women health"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 1.5175,
            lng = 103.6815,
            address = "No. 7, Jalan Skudai, Skudai 81300, Johor Bahru, Johor, Malaysia",
            phone = "+60 7-556 7890",
        ),
        // Additional KL Area
        Clinic(
            id = "clinic-019",
            name = "Wangsa Maju Health Centre",
            distanceKm = 5.1,
            tags = listOf("general", "dental"),
            nextAvailableSlot = hoursFromNow(72),
            lat = 3.2015,
            lng = 101.7395,
            address = "No. 16, Jalan Wangsa Maju 1, Wangsa Maju 53300, Kuala Lumpur, Malaysia",
            phone = "+60 3-4145 6789",
        ),
        Clinic(
            id = "clinic-020",
            name = "Setapak Wellness Clinic",
            distanceKm = 4.5,
            tags = listOf("general", "skin", "children"),
            nextAvailableSlot = hoursFromNow(48),
            lat = 3.2085,
            lng = 101.7155,
            address = "No. 52, Jalan Setapak, Setapak 53000, Kuala Lumpur, Malaysia",
            phone = "+60 3-4028 1234",
        ),
    )

val sampleClinics = rawSampleClinics.mapIndexed { index, clinic ->
    clinic.copy(imageUrl = clinicImageUrl(index))
}

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
