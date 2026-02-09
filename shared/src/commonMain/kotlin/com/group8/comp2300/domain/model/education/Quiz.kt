package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class Quiz(
    val id: String,
    val title: String,
    val description: String,
    val questions: List<QuizQuestion>,
    val category: ContentCategory,
    val difficultyLevel: QuizDifficulty = QuizDifficulty.MEDIUM,
    val estimatedMinutes: Int = 5
)
