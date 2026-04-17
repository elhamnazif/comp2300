package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.QuizAttemptReview
import com.group8.comp2300.domain.model.education.QuizAttempt
import com.group8.comp2300.domain.model.education.UserQuizAnswer

interface UserQuizRepository {

    fun saveQuizAttempt(attempt: QuizAttempt, answers: List<UserQuizAnswer>)

    fun getAttemptsByUserId(userId: String): List<QuizAttempt>

    fun getBestScore(userId: String, quizId: String): Int?

    fun hasPerfectScore(userId: String, quizId: String): Boolean

    fun countPerfectScores(userId: String): Long

    fun getAverageTimeSpent(userId: String): Double?

    fun getDetailedReview(attemptId: String): List<QuizAttemptReview>

    fun deletePreviousAttempt(userId: String, quizId: String)

    fun getIncorrectQuestionIds(attemptId: String): List<String>

    //use saveQuizAttempt only for now, since previous attempt is deleted if bested
    //fun saveNewBestAttempt(attempt: QuizAttempt, answers: List<UserQuizAnswer>)
}
