package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable
import net.sergeych.sprintf.format

@Serializable
data class Clinic(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val tags: List<String>,
    val nextAvailableSlot: Long, // Unix timestamp
    val lat: Double,
    val lng: Double,
    val address: String? = null,
    val phone: String? = null,
) {
    val formattedDistance: String
        get() =
            if (distanceKm < 1) {
                "${(distanceKm * 1000).toInt()}m"
            } else {
                "%.1fkm".format(distanceKm)
            }
}
