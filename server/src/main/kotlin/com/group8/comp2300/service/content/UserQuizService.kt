package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.QuizAttempt
import com.group8.comp2300.domain.model.education.QuizAttemptReview
import com.group8.comp2300.domain.model.education.UserQuizAnswer
import com.group8.comp2300.domain.model.education.UserQuizStats
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import com.group8.comp2300.domain.repository.UserQuizRepository
import com.group8.comp2300.dto.QuizSubmissionResult
import java.util.UUID

class UserQuizService(
    private val userQuizRepository: UserQuizRepository,
    private val badgeService: UserBadgeService
) {
    fun submitQuizAttempt(
        userId: String,
        quizId: String,
        newScore: Int,
        maxPossibleScore: Int,
        startedAt: Long,
        submittedAt: Long,
        rawAnswers: List<UserQuizAnswerInput>
    ): QuizSubmissionResult {
        val previousBest = userQuizRepository.getBestScore(userId, quizId)
        val attemptId = UUID.randomUUID().toString()
        val currentAttempt = QuizAttempt(
            id = attemptId,
            userId = userId,
            quizId = quizId,
            score = newScore,
            isFullScore = newScore == maxPossibleScore,
            startedAt = startedAt,
            submittedAt = submittedAt
        )

        val newBadges = emptyList<String>()
        // if 1st attempt or if better than previous
        if (previousBest == null || newScore >= previousBest) {
            val answers = rawAnswers.map { input ->
                UserQuizAnswer(
                    id = UUID.randomUUID().toString(),
                    attemptId = attemptId,
                    questionId = input.questionId,
                    selectedOptionId = input.selectedOptionId
                )
            }
            userQuizRepository.saveQuizAttempt(currentAttempt, answers)
            userQuizRepository.deletePreviousAttempt(userId, quizId) // 1 best attempt per user per quiz
            badgeService.checkForNewBadges(userId)
        }
        return QuizSubmissionResult(
            attempt = currentAttempt,
            newlyAwardedBadges = newBadges
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
            earnedBadges = earnedBadges
        )
    }


    fun getReviewForAttempt(attemptId: String): List<QuizAttemptReview> {
        return userQuizRepository.getDetailedReview(attemptId)
    }

    /**
     * For Article list UI checkmarks.
     */
    fun hasCompletedArticleQuiz(userId: String, quizId: String): Boolean {
        return userQuizRepository.hasPerfectScore(userId, quizId)
    }

    /**
     * Wipes progress if the user wants a clean slate.
     */
    fun resetQuizProgress(userId: String, quizId: String) {
        userQuizRepository.deletePreviousAttempt(userId, quizId)
    }
}
