package com.group8.comp2300.domain.model.quiz

data class Option(
    val id: String,
    val text: String
)

data class Question(
    val id: String,
    val text: String,
    val options: List<Option>,
    val correctAnswerId: String,
    val explanation: String, // for review system
    val points: Int = 10 // points per question
)

data class UserAnswer(
    val questionId: String,
    val selectedOptionId: String? // null in case of skips
)

data class EvaluatedAnswer(
    val question: Question,
    val userSelectedOptionId: String?,
    val isCorrect: Boolean
) {
    val correctAnswer: Option?
        get() = question.options.find { it.id == question.correctAnswerId }

    val selectedAnswer: Option?
        get() = question.options.find { it.id == userSelectedOptionId }
}

