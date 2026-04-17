package com.group8.comp2300.domain.model.education

import kotlinx.serialization.Serializable

@Serializable
data class QuizAttempt(
    val id: String,
    val userId: String,
    val quizId: String?,
    val score: Int,
    val isFullScore: Boolean,
    val startedAt: Long,
    val submittedAt: Long
)
@Serializable
data class UserQuizAnswer(
    val id: String,
    val attemptId: String,
    val questionId: String?,
    val selectedOptionId: String?
)
@Serializable
data class QuizAttemptReview(
    val questionId: String?,
    val questionText: String?,
    val selectedText: String?,
    val wasCorrect: Boolean,
    val explanation: String?
)
@Serializable
data class UserQuizStats(
    val totalPerfectScores: Long,
    val averageTimeSpentSeconds: Double,
    val earnedBadges: List<String>
)
@Serializable
data class UserQuizAnswerInput(
    val questionId: String,
    val selectedOptionId: String
)