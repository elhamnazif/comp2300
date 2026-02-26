package com.group8.comp2300.domain.model.mood

// represents calculated trend (monthly)

data class MoodSummary(
    val moodType: String,
    val count: Int,
    val percentage: Float)
