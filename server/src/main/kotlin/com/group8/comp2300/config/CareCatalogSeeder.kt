package com.group8.comp2300.config

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.mock.sampleClinics
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*

object CareCatalogSeeder {
    fun seedIfEmpty(database: ServerDatabase) {
        if (database.clinicQueries.selectAllClinics().executeAsList().isNotEmpty()) return

        sampleClinics.forEach { clinic ->
            database.clinicQueries.insertClinic(
                id = clinic.id,
                name = clinic.name,
                address = clinic.address.orEmpty(),
                phone = clinic.phone,
                latitude = clinic.lat,
                longitude = clinic.lng,
            )
            clinic.tags.forEach { tag ->
                database.clinicTagQueries.addClinicTag(
                    clinic_id = clinic.id,
                    tag_name = tag,
                )
            }
        }

        val zoneId = ZoneId.systemDefault()
        val firstBookableDay = LocalDate.now(zoneId).plusDays(1)
        val slotTimes = listOf(
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0),
            LocalTime.of(16, 0),
        )

        sampleClinics.forEachIndexed { clinicIndex, clinic ->
            repeat(10) { dayOffset ->
                val slotDate = firstBookableDay.plusDays(dayOffset.toLong())
                slotTimes.forEachIndexed { slotIndex, slotTime ->
                    val startTime = slotDate
                        .atTime(slotTime)
                        .atZone(zoneId)
                        .toInstant()
                        .toEpochMilli()
                    database.appointmentSlotQueries.createApptSlot(
                        id = UUID.randomUUID().toString(),
                        clinic_id = clinic.id,
                        start_time = startTime,
                        end_time = startTime + (30 * 60_000L),
                        is_booked = if ((clinicIndex + dayOffset + slotIndex) % 9 == 0) 1L else 0L,
                    )
                }
            }
        }
    }
}
