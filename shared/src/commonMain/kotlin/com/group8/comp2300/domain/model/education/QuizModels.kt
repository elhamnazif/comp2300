package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class QuizOption(val id: String, val text: String, val isCorrect: Boolean = false)

@Serializable
data class QuizQuestion(
    val id: String,
    val title: String,
    val explanation: String,
    val options: List<QuizOption> = emptyList(),
)

@Serializable
data class Quiz(
    val id: String,
    val articleId: String,
    val title: String,
    val questions: List<QuizQuestion> = emptyList(),
)
