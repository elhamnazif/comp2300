package com.group8.comp2300

import com.group8.comp2300.config.Environment
import com.group8.comp2300.config.JwtConfig
import com.group8.comp2300.config.ResendConfig
import com.group8.comp2300.di.serverModule
import com.group8.comp2300.routes.*
import com.group8.comp2300.security.JwtService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.get
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = Environment.port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    install(Koin) {
        slf4jLogger()
        modules(serverModule)
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    val jwtService: JwtService = get()
    val devBypass = Environment.devAuthBypass

    install(Authentication) {
        jwt("auth-jwt") {
            realm = JwtConfig.realm
            verifier(jwtService.verifier)
            validate { credential ->
                val userId = credential.payload.subject
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token is not valid or has expired"))
            }
        }
    }

    if (devBypass) {
        log.warn("⚠️  DEV AUTH BYPASS is ENABLED — authenticated routes accept unauthenticated requests")
    }

    log.info(
        "Startup config: env={}, port={}, dbPath={}, authBypass={}, emailConfigured={}",
        Environment.environmentName,
        Environment.port,
        Environment.dbPath,
        devBypass,
        ResendConfig.isConfigured,
    )

    routing {
        get("/") { call.respondText("Ktor: ready") }

        get("/api/health") { call.respond(mapOf("status" to "OK")) }
        staticResources("/images", "images")

        authRoutes(get())
        productRoutes()
        clinicRoutes()
        articleRoutes()
        contentCategoryRoutes()
        quizRoutes()

        authenticate("auth-jwt", optional = devBypass) {
            chatbotRoutes()
            orderRoutes()
            appointmentRoutes()
            badgeRoutes()
            medicationRoutes()
            moodRoutes()
            medicalRecordRoutes()
            userQuizRoutes()
        }
    }
}
