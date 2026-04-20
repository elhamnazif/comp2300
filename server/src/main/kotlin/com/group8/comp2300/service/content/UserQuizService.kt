package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.*
import com.group8.comp2300.domain.repository.QuizRepository
import com.group8.comp2300.domain.repository.UserQuizRepository
import com.group8.comp2300.dto.QuizSubmissionResult
import java.util.*

class UserQuizService(
    private val userQuizRepository: UserQuizRepository,
    private val badgeService: UserBadgeService,
    private val quizRepository: QuizRepository,
) {
    fun submitQuizAttempt(
        userId: String,
        quizId: String,
        startedAt: Long,
        submittedAt: Long,
        rawAnswers: List<UserQuizAnswerInput>,
    ): QuizSubmissionResult {
        val quiz = quizRepository.getQuizById(quizId) ?: throw NoSuchElementException("Quiz not found")
        val previousBest = userQuizRepository.getBestScore(userId, quizId)
        val attemptId = UUID.randomUUID().toString()
        val selectedOptionIds = rawAnswers.associate { it.questionId to it.selectedOptionId }
        val computedScore = quiz.questions.count { question ->
            val selectedOptionId = selectedOptionIds[question.id]
            question.options.any { option -> option.id == selectedOptionId && option.isCorrect }
        }
        val currentAttempt = QuizAttempt(
            id = attemptId,
            userId = userId,
            quizId = quizId,
            score = computedScore,
            isFullScore = computedScore == quiz.questions.size,
            startedAt = startedAt,
            submittedAt = submittedAt,
        )

        var newBadges = emptyList<String>()
        // if 1st attempt or if better than previous
        if (previousBest == null || computedScore >= previousBest) {
            val answers = quiz.questions.mapNotNull { question ->
                selectedOptionIds[question.id]?.let { selectedOptionId ->
                    UserQuizAnswer(
                        id = UUID.randomUUID().toString(),
                        attemptId = attemptId,
                        questionId = question.id,
                        selectedOptionId = selectedOptionId,
                    )
                }
            }
            userQuizRepository.saveBestAttempt(currentAttempt, answers)
            newBadges = badgeService.checkForNewBadges(userId)
        }
        return QuizSubmissionResult(
            attempt = currentAttempt,
            newlyAwardedBadges = newBadges,
        )
    }

    fun getUserProfileStats(userId: String): UserQuizStats {
        val perfectScores = userQuizRepository.countPerfectScores(userId)
        val avgTimeMs = userQuizRepository.getAverageTimeSpent(userId) ?: 0.0
        val avgTimeSeconds = avgTimeMs / 1000.0

        val earnedBadges = badgeService
            .getFullAchievementProfile(userId)
            .map { it.badge.name }

        return UserQuizStats(
            totalPerfectScores = perfectScores,
            averageTimeSpentSeconds = avgTimeSeconds,
            earnedBadges = earnedBadges,
        )
    }

    fun getReviewForAttempt(attemptId: String): List<QuizAttemptReview> =
        userQuizRepository.getDetailedReview(attemptId)

    /**
     * For Article list UI checkmarks.
     */
    fun hasCompletedArticleQuiz(userId: String, quizId: String): Boolean =
        userQuizRepository.hasPerfectScore(userId, quizId)

    /**
     * Wipes progress if the user wants a clean slate.
     */
    fun resetQuizProgress(userId: String, quizId: String) {
        userQuizRepository.deletePreviousAttempt(userId, quizId)
    }
}
