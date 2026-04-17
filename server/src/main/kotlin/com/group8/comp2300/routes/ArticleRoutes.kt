package com.group8.comp2300.routes

import com.group8.comp2300.service.content.ArticleService
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.articleRoutes() {
    val articleService: ArticleService by inject()

    route("/api/articles") {
        get {
            val articles = articleService.getAllArticles()
            call.respond(articles)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing article ID"
            )

            val article = articleService.getArticleById(id)

            if (article != null) {
                call.respond(article)
            } else {
                call.respond(HttpStatusCode.NotFound, "Article not found")
            }
        }
    }
}
