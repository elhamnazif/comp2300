package com.group8.comp2300.routes

import com.group8.comp2300.domain.repository.ProductRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.productRoutes() {
    val productRepository: ProductRepository by inject()

    get("/api/products") {
        call.respond(productRepository.getAll())
    }

    get("/api/products/{id}") {
        val id =
            call.parameters["id"]
                ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing product ID",
                )
        val product = productRepository.getById(id)
        if (product != null) {
            call.respond(product)
        } else {
            call.respond(HttpStatusCode.NotFound, "Product not found")
        }
    }
}
