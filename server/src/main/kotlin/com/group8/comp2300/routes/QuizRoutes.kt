package com.group8.comp2300.routes

import com.group8.comp2300.service.content.QuizService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.quizRoutes() {
    val service by inject<QuizService>()

    route("/api/quizzes") {
        get {
            val quizzes = service.getAllQuizzes()
            call.respond(HttpStatusCode.OK, quizzes)
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing quiz ID")

            val quiz = service.getQuizById(id)
            if (quiz != null) {
                call.respond(HttpStatusCode.OK, quiz)
            } else {
                call.respond(HttpStatusCode.NotFound, "Quiz not found")
            }
        }

        get("/article/{articleId}") {
            val articleId = call.parameters["articleId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing article ID")

            val quiz = service.getQuizByArticleId(articleId)
            if (quiz != null) {
                call.respond(HttpStatusCode.OK, quiz)
            } else {
                call.respond(HttpStatusCode.NotFound, "No quiz found for this article")
            }
        }
    }
}
