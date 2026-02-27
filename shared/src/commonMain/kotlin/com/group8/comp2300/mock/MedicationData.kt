package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus

val sampleMedications =
    listOf(
        Medication(
            id = "1",
            userId = "user1",
            name = "Truvada",
            dosage = "200mg",
            quantity = "30 tablets",
            frequency = MedicationFrequency.DAILY,
            instruction = "Take with food",
            colorHex = "#42A5F5", // Blue
            startDate = "2024-10-01",
            endDate = "2024-12-31",
            hasReminder = true,
            status = MedicationStatus.ACTIVE,
        ),
        Medication(
            id = "2",
            userId = "user1",
            name = "Multivitamin",
            dosage = "1 tablet",
            quantity = "60 tablets",
            frequency = MedicationFrequency.DAILY,
            instruction = "",
            colorHex = "#66BB6A", // Green
            startDate = "2024-10-01",
            endDate = "2024-12-31",
            hasReminder = true,
            status = MedicationStatus.ACTIVE,
        ),
        Medication(
            id = "3",
            userId = "user1",
            name = "Ibuprofen",
            dosage = "400mg",
            quantity = "20 tablets",
            frequency = MedicationFrequency.ON_DEMAND,
            instruction = "For headache",
            colorHex = "#FFA726", // Orange
            startDate = "2024-10-01",
            endDate = "2024-12-31",
            hasReminder = false,
            status = MedicationStatus.ARCHIVED,
        ),
    )
