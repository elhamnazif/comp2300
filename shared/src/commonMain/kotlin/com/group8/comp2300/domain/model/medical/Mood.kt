package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
data class Mood(
    val id: String,
    val userId: String,
    val timestamp: Long, // Unix timestamp in seconds or milliseconds
    val moodType: MoodType,
    val feeling: String? = null,
    val journal: String? = null,
)