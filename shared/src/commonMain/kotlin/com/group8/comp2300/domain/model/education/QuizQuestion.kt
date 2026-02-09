package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)
