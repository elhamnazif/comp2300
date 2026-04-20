package com.group8.comp2300.data.repository

import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.dto.ArticleDetailDto
import com.group8.comp2300.data.remote.dto.ArticleSummaryDto
import com.group8.comp2300.data.remote.dto.CategoryDto
import com.group8.comp2300.data.remote.dto.EarnedBadgeDto
import com.group8.comp2300.data.remote.dto.OptionDto
import com.group8.comp2300.data.remote.dto.QuestionDto
import com.group8.comp2300.data.remote.dto.QuizAttemptDto
import com.group8.comp2300.data.remote.dto.QuizDto
import com.group8.comp2300.data.remote.dto.QuizSubmissionRequestDto
import com.group8.comp2300.data.remote.dto.QuizSubmissionResultDto
import com.group8.comp2300.data.remote.dto.UserQuizStatsDto
import com.group8.comp2300.domain.model.education.ArticleDetail
import com.group8.comp2300.domain.model.education.ArticleSummary
import com.group8.comp2300.domain.model.education.Badge
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.model.education.EarnedBadge
import com.group8.comp2300.domain.model.education.EducationProgress
import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizAttempt
import com.group8.comp2300.domain.model.education.QuizOption
import com.group8.comp2300.domain.model.education.QuizQuestion
import com.group8.comp2300.domain.model.education.QuizSubmissionResult
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import com.group8.comp2300.domain.model.education.UserQuizStats
import com.group8.comp2300.domain.repository.EducationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class EducationRepositoryImpl(private val apiService: ApiService) : EducationRepository {
    override suspend fun getCategories(): List<Category> =
        apiService.getEducationCategories().map(CategoryDto::toDomain)

    override suspend fun getArticles(): List<ArticleSummary> =
        apiService.getEducationArticles().map(ArticleSummaryDto::toDomain)

    override suspend fun getArticleDetail(id: String): ArticleDetail? = runCatching {
        apiService.getEducationArticle(id).toDomain()
    }.getOrNull()

    override suspend fun getQuizById(id: String): Quiz? = runCatching {
        apiService.getEducationQuiz(id).toDomain()
    }.getOrNull()

    override suspend fun submitQuizAttempt(
        quizId: String,
        startedAt: Long,
        submittedAt: Long,
        answers: List<UserQuizAnswerInput>,
    ): QuizSubmissionResult = apiService.submitEducationQuiz(
        quizId = quizId,
        request = QuizSubmissionRequestDto(
            startedAt = startedAt,
            submittedAt = submittedAt,
            answers = answers,
        ),
    ).toDomain()

    override suspend fun getProgress(): EducationProgress = coroutineScope {
        val statsDeferred = async { apiService.getEducationQuizStats().toDomain() }
        val earnedDeferred = async { apiService.getEducationEarnedBadges().map(EarnedBadgeDto::toDomain) }
        EducationProgress(
            stats = statsDeferred.await(),
            earnedBadges = earnedDeferred.await(),
        )
    }
}

private fun CategoryDto.toDomain(): Category = Category(
    id = id,
    title = title,
)

private fun ArticleSummaryDto.toDomain(): ArticleSummary = ArticleSummary(
    id = id,
    title = title,
    description = description,
    thumbnailUrl = thumbnailUrl,
    publisher = publisher,
    publishedDate = publishedDate,
    categories = categories.map(CategoryDto::toDomain),
)

private fun ArticleDetailDto.toDomain(): ArticleDetail = ArticleDetail(
    id = id,
    title = title,
    description = description,
    content = content,
    thumbnailUrl = thumbnailUrl,
    publisher = publisher,
    publishedDate = publishedDate,
    categories = categories.map(CategoryDto::toDomain),
    quiz = quiz?.toDomain(),
)

private fun QuizDto.toDomain(articleId: String = ""): Quiz = Quiz(
    id = id,
    articleId = articleId,
    title = title,
    questions = questions.map(QuestionDto::toDomain),
)

private fun QuestionDto.toDomain(): QuizQuestion = QuizQuestion(
    id = id,
    title = title,
    explanation = explanation,
    options = options.map(OptionDto::toDomain),
)

private fun OptionDto.toDomain(): QuizOption = QuizOption(
    id = id,
    text = text,
)

private fun QuizAttemptDto.toDomain(): QuizAttempt = QuizAttempt(
    id = id,
    userId = userId,
    quizId = quizId,
    score = score,
    isFullScore = isFullScore,
    startedAt = startedAt,
    submittedAt = submittedAt,
)

private fun QuizSubmissionResultDto.toDomain(): QuizSubmissionResult = QuizSubmissionResult(
    attempt = attempt.toDomain(),
    newlyAwardedBadges = newlyAwardedBadges,
)

private fun UserQuizStatsDto.toDomain(): UserQuizStats = UserQuizStats(
    totalPerfectScores = totalPerfectScores,
    averageTimeSpentSeconds = averageTimeSpentSeconds,
    earnedBadges = earnedBadges,
)

private fun EarnedBadgeDto.toDomain(): EarnedBadge = EarnedBadge(
    badge = Badge(
        id = id,
        name = name,
        iconPath = iconPath,
        isLocked = false,
    ),
    earnedAt = earnedAt,
)
