package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class MoodType(val displayName: String) {
    VERY_SAD("Very Sad"),
    SAD("Sad"),
    NEUTRAL("Neutral"),
    GOOD("Good"),
    GREAT("Great"),
}
