package com.group8.comp2300.service.content

import com.group8.comp2300.domain.model.education.Quiz
import com.group8.comp2300.domain.model.education.QuizOption
import com.group8.comp2300.domain.model.education.QuizQuestion
import com.group8.comp2300.domain.model.education.UserQuizAnswerInput
import com.group8.comp2300.domain.repository.QuizRepository
import com.group8.comp2300.domain.repository.UserQuizRepository
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals

class UserQuizServiceTest {

    private val repository = mockk<UserQuizRepository>()
    private val badgeService = mockk<UserBadgeService>()
    private val quizRepository = mockk<QuizRepository>()
    private val service = UserQuizService(repository, badgeService, quizRepository)

    @Test
    fun `submitQuizAttempt keeps best attempt and returns newly awarded badges`() {
        every { quizRepository.getQuizById("quiz-1") } returns sampleQuiz()
        every { repository.getBestScore("user-1", "quiz-1") } returns 1
        every { repository.saveBestAttempt(any(), any()) } returns Unit
        every { badgeService.checkForNewBadges("user-1") } returns listOf("The_Rookie")

        val result = service.submitQuizAttempt(
            userId = "user-1",
            quizId = "quiz-1",
            startedAt = 1_000L,
            submittedAt = 2_000L,
            rawAnswers = listOf(
                UserQuizAnswerInput(questionId = "q1", selectedOptionId = "opt-a"),
                UserQuizAnswerInput(questionId = "q2", selectedOptionId = "opt-d"),
            ),
        )

        assertEquals(listOf("The_Rookie"), result.newlyAwardedBadges)
        assertEquals(2, result.attempt.score)
        assertEquals(true, result.attempt.isFullScore)

        verify(exactly = 1) {
            repository.saveBestAttempt(
                withArg { attempt ->
                    assertEquals("user-1", attempt.userId)
                    assertEquals("quiz-1", attempt.quizId)
                    assertEquals(2, attempt.score)
                    assertEquals(true, attempt.isFullScore)
                },
                withArg { answers ->
                    assertEquals(2, answers.size)
                    assertEquals(listOf("q1", "q2"), answers.mapNotNull { it.questionId })
                    assertEquals(listOf("opt-a", "opt-d"), answers.mapNotNull { it.selectedOptionId })
                },
            )
        }
        verify(exactly = 1) { quizRepository.getQuizById("quiz-1") }
        verify(exactly = 1) { repository.getBestScore("user-1", "quiz-1") }
        verify(exactly = 1) { badgeService.checkForNewBadges("user-1") }
        confirmVerified(repository, badgeService, quizRepository)
    }

    @Test
    fun `submitQuizAttempt ignores forged higher client score and saves computed result`() {
        every { quizRepository.getQuizById("quiz-1") } returns sampleQuiz()
        every { repository.getBestScore("user-1", "quiz-1") } returns 5
        every { repository.saveBestAttempt(any(), any()) } returns Unit
        every { badgeService.checkForNewBadges("user-1") } returns emptyList()

        val result = service.submitQuizAttempt(
            userId = "user-1",
            quizId = "quiz-1",
            startedAt = 1_000L,
            submittedAt = 2_000L,
            rawAnswers = listOf(UserQuizAnswerInput(questionId = "q1", selectedOptionId = "opt-b")),
        )

        assertEquals(emptyList(), result.newlyAwardedBadges)
        assertEquals(0, result.attempt.score)
        assertEquals(false, result.attempt.isFullScore)

        verify(exactly = 1) { quizRepository.getQuizById("quiz-1") }
        verify(exactly = 1) { repository.getBestScore("user-1", "quiz-1") }
        verify(exactly = 0) { repository.saveBestAttempt(any(), any()) }
        verify(exactly = 0) { badgeService.checkForNewBadges(any()) }
        confirmVerified(repository, badgeService, quizRepository)
    }

    private fun sampleQuiz(): Quiz = Quiz(
        id = "quiz-1",
        articleId = "article-1",
        title = "Sample quiz",
        questions = listOf(
            QuizQuestion(
                id = "q1",
                title = "Question 1",
                explanation = "Explanation 1",
                options = listOf(
                    QuizOption(id = "opt-a", text = "A", isCorrect = true),
                    QuizOption(id = "opt-b", text = "B", isCorrect = false),
                ),
            ),
            QuizQuestion(
                id = "q2",
                title = "Question 2",
                explanation = "Explanation 2",
                options = listOf(
                    QuizOption(id = "opt-c", text = "C", isCorrect = false),
                    QuizOption(id = "opt-d", text = "D", isCorrect = true),
                ),
            ),
        ),
    )
}
