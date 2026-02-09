package com.group8.comp2300.domain.model.reminder

import kotlinx.serialization.Serializable

@Serializable
enum class ReminderFrequency {
    ONCE,
    DAILY,
    WEEKLY,
    MONTHLY
}
