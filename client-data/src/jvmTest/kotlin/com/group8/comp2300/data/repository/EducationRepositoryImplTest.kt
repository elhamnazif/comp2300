package com.group8.comp2300.data.repository

import com.group8.comp2300.data.remote.dto.*
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class EducationRepositoryImplTest {
    private val apiService = EducationApiServiceStub()
    private val repository = EducationRepositoryImpl(apiService)

    @Test
    fun getProgressCombinesStatsAndEarnedBadges() = runTest {
        val progress = repository.getProgress()

        assertEquals(3L, progress.stats.totalPerfectScores)
        assertEquals(42.5, progress.stats.averageTimeSpentSeconds)
        assertEquals(2, progress.earnedBadges.size)
        assertEquals("Quiz Champion", progress.earnedBadges.first().badge.name)
    }

    @Test
    fun getQuizByIdDoesNotExposeAnswerKeys() = runTest {
        val quiz = repository.getQuizById("quiz-1")

        assertEquals(false, quiz?.questions?.first()?.options?.first()?.isCorrect)
        assertEquals(false, quiz?.questions?.first()?.options?.last()?.isCorrect)
    }

    @Test
    fun submitQuizAttemptForwardsSubmissionPayloadAndMapsResponse() = runTest {
        val result = repository.submitQuizAttempt(
            quizId = "quiz-1",
            startedAt = 1000L,
            submittedAt = 4000L,
            answers = listOf(
                UserQuizAnswerInput(questionId = "question-1", selectedOptionId = "option-2"),
                UserQuizAnswerInput(questionId = "question-2", selectedOptionId = "option-3"),
            ),
        )

        assertEquals("quiz-1", apiService.lastSubmissionQuizId)
        assertEquals(
            QuizSubmissionRequestDto(
                startedAt = 1000L,
                submittedAt = 4000L,
                answers = listOf(
                    UserQuizAnswerInput(questionId = "question-1", selectedOptionId = "option-2"),
                    UserQuizAnswerInput(questionId = "question-2", selectedOptionId = "option-3"),
                ),
            ),
            apiService.lastSubmissionRequest,
        )
        assertEquals("attempt-1", result.attempt.id)
        assertEquals(listOf("quiz_champion"), result.newlyAwardedBadges)
    }
}

private class EducationApiServiceStub : FakeApiService() {
    var lastSubmissionQuizId: String? = null
    var lastSubmissionRequest: QuizSubmissionRequestDto? = null

    override suspend fun getEducationCategories(): List<CategoryDto> = listOf(
        CategoryDto(id = "cat-1", title = "Sexual Health", articleCount = 1),
    )

    override suspend fun getEducationArticles(): List<ArticleSummaryDto> = listOf(
        ArticleSummaryDto(
            id = "article-1",
            title = "Contraception Basics",
            description = "What to know first.",
            thumbnailUrl = null,
            publisher = "Care",
            publishedDate = 1710000000000,
            categories = getEducationCategories(),
        ),
    )

    override suspend fun getEducationArticle(id: String): ArticleDetailDto = ArticleDetailDto(
        id = id,
        title = "Contraception Basics",
        description = "What to know first.",
        content = "Detailed article body",
        thumbnailUrl = null,
        publisher = "Care",
        publishedDate = 1710000000000,
        categories = getEducationCategories(),
        quiz = getEducationQuiz("quiz-1"),
    )

    override suspend fun getEducationQuiz(id: String): QuizDto = QuizDto(
        id = id,
        title = "Contraception Quiz",
        questions = listOf(
            QuestionDto(
                id = "question-1",
                title = "Question 1",
                explanation = "Explanation 1",
                options = listOf(
                    OptionDto(id = "option-1", text = "A"),
                    OptionDto(id = "option-2", text = "B"),
                ),
            ),
        ),
    )

    override suspend fun submitEducationQuiz(
        quizId: String,
        request: QuizSubmissionRequestDto,
    ): QuizSubmissionResultDto {
        lastSubmissionQuizId = quizId
        lastSubmissionRequest = request
        return QuizSubmissionResultDto(
            attempt = QuizAttemptDto(
                id = "attempt-1",
                userId = "user-1",
                quizId = quizId,
                score = 2,
                isFullScore = false,
                startedAt = request.startedAt,
                submittedAt = request.submittedAt,
            ),
            newlyAwardedBadges = listOf("quiz_champion"),
        )
    }

    override suspend fun getEducationQuizStats(): UserQuizStatsDto = UserQuizStatsDto(
        totalPerfectScores = 3,
        averageTimeSpentSeconds = 42.5,
        earnedBadges = listOf("Quiz Champion", "Quick Learner"),
    )

    override suspend fun getEducationEarnedBadges(): List<EarnedBadgeDto> = listOf(
        EarnedBadgeDto(
            id = "badge-1",
            name = "Quiz Champion",
            iconPath = "/badges/quiz-champion.png",
            earnedAt = 1710000001000,
        ),
        EarnedBadgeDto(
            id = "badge-2",
            name = "Quick Learner",
            iconPath = "/badges/quick-learner.png",
            earnedAt = 1710000002000,
        ),
    )
}
