package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.shop.PlaceOrderRequest
import com.group8.comp2300.service.order.OrderService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

fun Route.orderRoutes() {
    val orderService: OrderService by inject()

    post("/api/orders") {
        withUserId { userId ->
            runCatching {
                orderService.placeOrder(
                    userId = userId,
                    request = call.receive<PlaceOrderRequest>(),
                )
            }.onSuccess { order ->
                call.respond(HttpStatusCode.Created, order)
            }.onFailure { error ->
                val message = error.message ?: "Failed to place order"
                val status = if (error is IllegalArgumentException) {
                    HttpStatusCode.BadRequest
                } else {
                    HttpStatusCode.InternalServerError
                }
                call.respond(status, mapOf("error" to message))
            }
        }
    }

    get("/api/orders/me") {
        withUserId { userId ->
            call.respond(HttpStatusCode.OK, orderService.getOrdersForUser(userId))
        }
    }
}
