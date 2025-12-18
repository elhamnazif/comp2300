package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.Medication
import com.group8.comp2300.domain.model.medical.MedicationFrequency
import com.group8.comp2300.domain.model.medical.MedicationStatus

val sampleMedications =
        listOf(
                Medication(
                        id = "1",
                        name = "Truvada",
                        dosage = "200mg",
                        frequency = MedicationFrequency.DAILY,
                        instructions = "Take with food",
                        colorHex = "#42A5F5", // Blue
                        status = MedicationStatus.ACTIVE
                ),
                Medication(
                        id = "2",
                        name = "Multivitamin",
                        dosage = "1 tablet",
                        frequency = MedicationFrequency.DAILY,
                        instructions = "",
                        colorHex = "#66BB6A", // Green
                        status = MedicationStatus.ACTIVE
                ),
                Medication(
                        id = "3",
                        name = "Ibuprofen",
                        dosage = "400mg",
                        frequency = MedicationFrequency.ON_DEMAND,
                        instructions = "For headache",
                        colorHex = "#FFA726", // Orange
                        status = MedicationStatus.ARCHIVED
                )
        )
