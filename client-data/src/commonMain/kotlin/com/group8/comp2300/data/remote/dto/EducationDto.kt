package com.group8.comp2300.data.remote.dto

import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(val id: String, val title: String, val articleCount: Long? = null)

@Serializable
data class OptionDto(val id: String, val text: String)

@Serializable
data class QuestionDto(val id: String, val title: String, val explanation: String, val options: List<OptionDto>)

@Serializable
data class QuizDto(val id: String, val title: String, val questions: List<QuestionDto>)

@Serializable
data class ArticleSummaryDto(
    val id: String,
    val title: String,
    val description: String,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: Long?,
    val categories: List<CategoryDto>,
)

@Serializable
data class ArticleDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val content: String,
    val thumbnailUrl: String?,
    val publisher: String?,
    val publishedDate: Long?,
    val categories: List<CategoryDto>,
    val quiz: QuizDto? = null,
)

@Serializable
data class BadgeDto(val id: String, val name: String, val iconPath: String)

@Serializable
data class EarnedBadgeDto(val id: String, val name: String, val iconPath: String, val earnedAt: Long)

@Serializable
data class QuizAttemptDto(
    val id: String,
    val userId: String,
    val quizId: String?,
    val score: Int,
    val isFullScore: Boolean,
    val startedAt: Long,
    val submittedAt: Long,
)

@Serializable
data class QuizSubmissionRequestDto(val startedAt: Long, val submittedAt: Long, val answers: List<UserQuizAnswerInput>)

@Serializable
data class QuizSubmissionResultDto(val attempt: QuizAttemptDto, val newlyAwardedBadges: List<String>)

@Serializable
data class UserQuizStatsDto(
    val totalPerfectScores: Long,
    val averageTimeSpentSeconds: Double,
    val earnedBadges: List<String>,
)
