package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class QuizResult(
        val id: String,
        val quizId: String,
        val userId: String,
        val score: Int,
        val totalQuestions: Int,
        val completedAt: Long,
        val answers: List<Int> = emptyList()
) {
        val percentage: Int
                get() = if (totalQuestions > 0) (score * 100) / totalQuestions else 0
        val passed: Boolean
                get() = percentage >= 70
}