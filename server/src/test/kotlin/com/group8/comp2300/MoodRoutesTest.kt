package com.group8.comp2300

import com.group8.comp2300.data.repository.MoodRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.medical.Mood
import com.group8.comp2300.domain.model.medical.MoodEntryRequest
import com.group8.comp2300.domain.model.medical.MoodType
import com.group8.comp2300.domain.repository.MoodRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.moodRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class MoodRoutesTest {
    @Test
    fun getMoodHistoryRequiresAuthentication() = testApplication {
        withDevAuthBypassDisabled {
            configureMoodTestModule()
            val client = jsonClient()

            val response = client.get("/api/moods")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun getMoodHistoryReturnsUserHistory() = testApplication {
        val accessToken = configureMoodTestModuleWithUser()
        val client = jsonClient()

        val createResponse = client.post("/api/moods") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            header(HttpHeaders.ContentType, "application/json")
            setBody(MoodEntryRequest(4, listOf("calm"), emptyList(), "steady"))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        val historyResponse = client.get("/api/moods") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, historyResponse.status)
        val history = historyResponse.body<List<Mood>>()
        assertEquals(1, history.size)
        assertEquals(MoodType.GOOD, history.single().moodType)
    }
}

private inline fun withDevAuthBypassDisabled(block: () -> Unit) {
    val previous = System.getProperty("DEV_AUTH_BYPASS")
    System.setProperty("DEV_AUTH_BYPASS", "false")
    try {
        block()
    } finally {
        if (previous == null) {
            System.clearProperty("DEV_AUTH_BYPASS")
        } else {
            System.setProperty("DEV_AUTH_BYPASS", previous)
        }
    }
}

private fun ApplicationTestBuilder.configureMoodTestModule() {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val testModule = module {
        single<ServerDatabase> { database }
        single<MoodRepository> { MoodRepositoryImpl(get()) }
    }

    application {
        install(Koin) {
            slf4jLogger()
            modules(testModule)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        routing {
            moodRoutes()
        }
    }
}

private fun ApplicationTestBuilder.configureMoodTestModuleWithUser(): String {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testMoodJwtService()
    val userRepository = UserRepositoryImpl(database)

    val testModule = module {
        single<ServerDatabase> { database }
        single<MoodRepository> { MoodRepositoryImpl(get()) }
    }

    val userId = "user_mood_${java.util.UUID.randomUUID()}"
    userRepository.insert(
        id = userId,
        email = "mood.test@example.com",
        passwordHash = "hash",
        firstName = "Mood",
        lastName = "Test",
        phone = null,
        dateOfBirth = null,
        gender = null,
        sexualOrientation = null,
        preferredLanguage = "en",
    )
    userRepository.activateUser(userId)

    application {
        install(Koin) {
            slf4jLogger()
            modules(testModule)
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Authentication) {
            jwt("auth-jwt") {
                realm = "comp2300-test"
                verifier(jwtService.verifier)
                validate { credential ->
                    credential.payload.subject?.let { JWTPrincipal(credential.payload) }
                }
            }
        }
        routing {
            authenticate("auth-jwt") {
                moodRoutes()
            }
        }
    }

    return jwtService.generateAccessToken(userId)
}

private fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ClientContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            },
        )
    }
}

private fun testMoodJwtService(): JwtService = JwtServiceImpl(
    secret = "test-secret-for-mood-routes",
    issuer = "http://localhost/test",
    audience = "http://localhost/test",
)
