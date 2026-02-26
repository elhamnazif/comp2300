package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodType

interface MoodRepository {
    fun insert(mood: Mood)
    fun getHistory(userId: String): List<Mood>
    fun getDailyMoods(userId: String, dateString: String): List<Mood>
    fun update(id: String, moodType: MoodType, feeling: String?, journal: String?)
    fun delete(id: String)
    fun getDailyCount(userId: String, dateString: String): Map<MoodType, Long>
    fun getMonthlyCount(userId: String, monthStart: String): Map<MoodType, Long>
}