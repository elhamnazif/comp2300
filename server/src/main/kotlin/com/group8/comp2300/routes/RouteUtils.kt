package com.group8.comp2300.routes

import com.group8.comp2300.config.Environment
import com.group8.comp2300.domain.repository.UserRepository
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

suspend fun RoutingContext.withUserId(block: suspend (String) -> Unit) {
    val principal = call.principal<JWTPrincipal>()
    val userId = principal?.payload?.subject
        ?: Environment.DEV_USER_ID.takeIf { Environment.devAuthBypass }

    if (userId == null) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return
    }

    val userRepository = runCatching { call.application.get<UserRepository>() }.getOrNull()
    if (userRepository != null && !userRepository.isActive(userId)) {
        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized"))
        return
    }

    block(userId)
}
