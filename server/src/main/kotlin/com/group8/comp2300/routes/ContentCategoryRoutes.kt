package com.group8.comp2300.routes

import com.group8.comp2300.service.content.ContentCategoryService
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.contentCategoryRoutes() {
    val categoryService: ContentCategoryService by inject()

    route("/api/categories") {

        // GET all categories (with article counts)
        get {
            val categories = categoryService.getAllCategories()
            call.respond(categories)
        }

        // all articles belonging to a specific category
        get("/{id}/articles") {
            val categoryId = call.parameters["id"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                "Missing category ID"
            )
            val articles = categoryService.getArticlesByCategory(categoryId)
            call.respond(articles)
        }
    }
}

