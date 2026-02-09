package com.group8.comp2300

import com.group8.comp2300.di.serverModule
import com.group8.comp2300.routes.productRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun main() {
    embeddedServer(Netty, port = ServerConfig.PORT, host = "0.0.0.0", module = Application::module).start(wait = true)
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
            }
        )
    }

    routing {
        get("/") { call.respondText("Ktor: ${Greeting().greet()}") }

        get("/api/health") { call.respond(mapOf("status" to "OK")) }

        productRoutes()
    }
}
