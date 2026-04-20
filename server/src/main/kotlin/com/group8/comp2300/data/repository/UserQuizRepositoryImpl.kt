package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.education.QuizAttempt
import com.group8.comp2300.domain.model.education.QuizAttemptReview
import com.group8.comp2300.domain.model.education.UserQuizAnswer
import com.group8.comp2300.domain.repository.UserQuizRepository

class UserQuizRepositoryImpl(private val database: ServerDatabase) : UserQuizRepository {

    private val attemptQueries = database.quizAttemptQueries
    private val answerQueries = database.userQuizAnswerQueries

    override fun saveBestAttempt(attempt: QuizAttempt, answers: List<UserQuizAnswer>) {
        database.transaction {
            attemptQueries.insertQuizAttempt(
                id = attempt.id,
                userId = attempt.userId,
                quizId = attempt.quizId,
                score = attempt.score.toLong(),
                isFullScore = if (attempt.isFullScore) 1L else 0L,
                startedAt = attempt.startedAt,
                submittedAt = attempt.submittedAt,
            )

            answers.forEach { answer ->
                answerQueries.insertUserAnswer(
                    id = answer.id,
                    attemptId = answer.attemptId,
                    questionId = answer.questionId,
                    selectedOptionId = answer.selectedOptionId,
                )
            }

            attempt.quizId?.let { quizId ->
                attemptQueries.deleteOtherAttemptsForQuiz(
                    userId = attempt.userId,
                    quizId = quizId,
                    attemptId = attempt.id,
                )
            }
        }
    }

    override fun getAttemptsByUserId(userId: String): List<QuizAttempt> =
        attemptQueries.getAttemptsByUserId(userId).executeAsList().map { row ->
            QuizAttempt(
                id = row.id,
                userId = row.user_id,
                quizId = row.quiz_id,
                score = row.score.toInt(),
                isFullScore = row.is_full_score == 1L,
                startedAt = row.started_at,
                submittedAt = row.submitted_at,
            )
        }

    override fun getBestScore(userId: String, quizId: String): Int? =
        attemptQueries.getBestScoreForQuiz(userId, quizId).executeAsOneOrNull()?.MAX?.toInt()

    override fun hasPerfectScore(userId: String, quizId: String): Boolean =
        attemptQueries.hasPerfectScore(userId, quizId).executeAsOne()

    override fun countPerfectScores(userId: String): Long = attemptQueries.countFullScores(userId).executeAsOne()

    override fun getAverageTimeSpent(userId: String): Double? =
        attemptQueries.getAverageTimeSpent(userId).executeAsOneOrNull()?.AVG

    override fun getDetailedReview(attemptId: String): List<QuizAttemptReview> =
        answerQueries.getDetailedReviewByAttempt(attemptId).executeAsList().map { row ->
            QuizAttemptReview(
                questionId = row.question_id,
                questionText = row.question_text,
                selectedText = row.selected_text,
                wasCorrect = row.was_correct == 1L,
                explanation = row.explanation,
            )
        }

    override fun deletePreviousAttempt(userId: String, quizId: String) {
        attemptQueries.deletePreviousAttempt(userId, quizId)
    }

    override fun getIncorrectQuestionIds(attemptId: String): List<String> =
        answerQueries.getIncorrectAnswersByAttempt(attemptId)
            .executeAsList()
            .mapNotNull { it.question_id }
/*
    override fun saveNewBestAttempt(attempt: QuizAttempt, answers: List<UserQuizAnswer>) {
        database.transaction {
            attempt.quizId.let { quizId ->
                attemptQueries.deletePreviousAttempt(attempt.userId, quizId)
            }
            // Insert the new attempt
            attemptQueries.insertQuizAttempt(
                id = attempt.id,
                userId = attempt.userId,
                quizId = attempt.quizId,
                score = attempt.score.toLong(),
                isFullScore = if (attempt.isFullScore) 1L else 0L,
                startedAt = attempt.startedAt,
                submittedAt = attempt.submittedAt
            )

            // Insert the new answers
            answers.forEach { answer ->
                answerQueries.insertUserAnswer(
                    id = answer.id,
                    attemptId = answer.attemptId,
                    questionId = answer.questionId,
                    selectedOptionId = answer.selectedOptionId
                )
            }
        }
    }*/
}
