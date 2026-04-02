package com.group8.comp2300.domain.repository.medical

import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest

interface MoodDataRepository {
    suspend fun logMood(request: MoodEntryRequest): Mood
}
