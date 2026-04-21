package com.group8.comp2300.dto

import com.group8.comp2300.domain.model.education.QuizAttempt
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import kotlinx.serialization.Serializable

@Serializable
data class OptionResponse(val id: String, val text: String)

@Serializable
data class QuestionResponse(
    val id: String,
    val title: String,
    val explanation: String,
    val options: List<OptionResponse>,
)

@Serializable
data class QuizResponse(val id: String, val title: String, val questions: List<QuestionResponse>)

@Serializable
data class UserQuizAnswerInput(val questionId: String, val selectedOptionId: String)

@Serializable
data class QuizSubmissionRequest(val startedAt: Long, val submittedAt: Long, val answers: List<UserQuizAnswerInput>)

@Serializable
data class QuizSubmissionResult(val attempt: QuizAttempt, val newlyAwardedBadges: List<String>)

@Serializable
data class UserQuizStats(
    val totalPerfectScores: Long,
    val averageTimeSpentSeconds: Double,
    val earnedBadges: List<String>,
)
