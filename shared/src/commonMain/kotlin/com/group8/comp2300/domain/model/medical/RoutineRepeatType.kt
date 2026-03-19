package com.group8.comp2300.domain.model.medical

import kotlinx.serialization.Serializable

@Serializable
enum class RoutineRepeatType(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Selected days"),
}
