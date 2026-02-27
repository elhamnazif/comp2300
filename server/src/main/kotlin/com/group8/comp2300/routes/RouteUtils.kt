package com.group8.comp2300.routes

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

suspend fun RoutingContext.withUserId(block: suspend (String) -> Unit) {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.subject

    if (userId == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return
    }

    block(userId)
}
