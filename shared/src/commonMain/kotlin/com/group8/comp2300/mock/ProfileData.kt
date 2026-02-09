package com.group8.comp2300.mock

import com.group8.comp2300.domain.model.medical.LabResult
import com.group8.comp2300.domain.model.medical.LabStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

// Helper to convert date to timestamp
@OptIn(kotlin.time.ExperimentalTime::class)
private fun dateToTimestamp(year: Int, month: Int, day: Int): Long =
    LocalDate(year, month, day).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

val sampleResults =
    listOf(
        LabResult(
            id = "1",
            testName = "HIV 4th Gen",
            testDate = dateToTimestamp(2024, 11, 28),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "2",
            testName = "Chlamydia",
            testDate = dateToTimestamp(2024, 11, 28),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "3",
            testName = "Gonorrhea",
            testDate = dateToTimestamp(2024, 11, 28),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "4",
            testName = "Syphilis",
            testDate = dateToTimestamp(2024, 10, 15),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "5",
            testName = "HIV Test",
            testDate = dateToTimestamp(2024, 9, 10),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "6",
            testName = "Hepatitis B",
            testDate = dateToTimestamp(2024, 8, 5),
            status = LabStatus.NEGATIVE
        ),
        LabResult(
            id = "7",
            testName = "Hepatitis C",
            testDate = dateToTimestamp(2024, 8, 5),
            status = LabStatus.NEGATIVE
        )
    )
