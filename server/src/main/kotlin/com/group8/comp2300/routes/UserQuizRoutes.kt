package com.group8.comp2300.routes

import com.group8.comp2300.dto.QuizSubmissionRequest
import com.group8.comp2300.service.content.UserQuizService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.userQuizRoutes() {
    val service by inject<UserQuizService>()

    route("/api/quizzes") {
        post("/{quizId}/submit") {
            val quizId = call.parameters["quizId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing quiz ID")

            val request = call.receive<QuizSubmissionRequest>()

            withUserId { userId ->
                try {
                    val result = service.submitQuizAttempt(
                        userId = userId,
                        quizId = quizId,
                        startedAt = request.startedAt,
                        submittedAt = request.submittedAt,
                        rawAnswers = request.answers,
                    )
                    call.respond(HttpStatusCode.Created, result)
                } catch (error: NoSuchElementException) {
                    call.respond(HttpStatusCode.NotFound, error.message ?: "Quiz not found")
                }
            }
        }

        get("/attempts/{attemptId}/review") {
            val attemptId = call.parameters["attemptId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing attempt ID")

            val review = service.getReviewForAttempt(attemptId)
            call.respond(HttpStatusCode.OK, review)
        }
    }

    route("/api/users") {
        get("/quiz-stats") {
            withUserId { userId ->
                val stats = service.getUserProfileStats(userId)
                call.respond(HttpStatusCode.OK, stats)
            }
        }

        get("/quizzes/{quizId}/status") {
            val quizId = call.parameters["quizId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            withUserId { userId ->
                val isCompleted = service.hasCompletedArticleQuiz(userId, quizId)
                call.respond(HttpStatusCode.OK, mapOf("completed" to isCompleted))
            }
        }

        delete("/quizzes/{quizId}") {
            val quizId = call.parameters["quizId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            withUserId { userId ->
                service.resetQuizProgress(userId, quizId)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
