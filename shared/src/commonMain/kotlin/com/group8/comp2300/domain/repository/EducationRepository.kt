package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.education.ArticleDetail
import com.group8.comp2300.domain.model.education.ArticleSummary
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.model.education.EducationProgress
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizSubmissionResult
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput

interface EducationRepository {
    suspend fun getCategories(): List<Category>

    suspend fun getArticles(): List<ArticleSummary>

    suspend fun getArticleDetail(id: String): ArticleDetail?

    suspend fun getQuizById(id: String): Quiz?

    suspend fun submitQuizAttempt(
        quizId: String,
        startedAt: Long,
        submittedAt: Long,
        answers: List<UserQuizAnswerInput>,
    ): QuizSubmissionResult

    suspend fun getProgress(): EducationProgress
}
