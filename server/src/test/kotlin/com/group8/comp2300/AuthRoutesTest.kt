package com.group8.comp2300

import com.group8.comp2300.data.repository.PasswordResetTokenRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.RefreshTokenRequest
import com.group8.comp2300.dto.RegisterRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.authRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.auth.AuthService
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class AuthRoutesTest {
    @Test
    fun registerReturnsCreatedAndAuthPayload() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "new.user@example.com",
                    password = "Password1",
                    firstName = "New",
                    lastName = "User",
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, response.status, response.bodyAsText())
        val payload = response.body<AuthResponse>()
        assertEquals("new.user@example.com", payload.user.email)
        assertTrue(payload.accessToken.isNotBlank())
        assertTrue(payload.refreshToken.isNotBlank())
    }

    @Test
    fun registerWithInvalidEmailReturnsSpecificError() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "invalid-email",
                    password = "Password1",
                    firstName = "Invalid",
                    lastName = "Email",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid email format", response.body<ErrorResponse>().error)
    }

    @Test
    fun registerWithPasswordOverBcryptLimitReturnsSpecificError() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "long.password@example.com",
                    password = "${"a".repeat(79)}1",
                    firstName = "Long",
                    lastName = "Password",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Password must be 72 bytes or fewer", response.body<ErrorResponse>().error)
    }

    @Test
    fun registerDuplicateEmailReturnsConflictAndMessage() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val request =
            RegisterRequest(
                email = "dupe@example.com",
                password = "Password1",
                firstName = "Dupe",
                lastName = "User",
            )

        val firstResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.Created, firstResponse.status)

        val secondResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        assertEquals(
            "An account with this email already exists",
            secondResponse.body<ErrorResponse>().error,
        )
    }

    @Test
    fun loginWithValidCredentialsReturnsSuccess() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val email = "login.user@example.com"
        val password = "Password1"

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = email,
                    password = password,
                    firstName = "Login",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val registered = registerResponse.body<AuthResponse>()
        userRepo.activateUser(registered.user.id)

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }

        assertEquals(HttpStatusCode.OK, loginResponse.status, loginResponse.bodyAsText())
        val payload = loginResponse.body<AuthResponse>()
        assertEquals(email, payload.user.email)
        assertTrue(payload.accessToken.isNotBlank())
        assertTrue(payload.refreshToken.isNotBlank())
    }

    @Test
    fun loginWithWrongPasswordReturnsUnauthorized() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val email = "wrong.password@example.com"

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = email,
                    password = "Password1",
                    firstName = "Wrong",
                    lastName = "Password",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = "WrongPassword1"))
        }

        assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
        assertEquals("Invalid email or password", loginResponse.body<ErrorResponse>().error)
    }

    @Test
    fun refreshWithValidTokenReturnsNewTokenPair() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "refresh.user@example.com",
                    password = "Password1",
                    firstName = "Refresh",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()

        val refreshResponse = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(authResponse.refreshToken))
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status, refreshResponse.bodyAsText())
        val tokenResponse = refreshResponse.body<TokenResponse>()
        assertTrue(tokenResponse.accessToken.isNotBlank())
        assertTrue(tokenResponse.refreshToken.isNotBlank())
    }

    @Test
    fun profileEndpointReturnsUserForValidAccessToken() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "profile.user@example.com",
                    password = "Password1",
                    firstName = "Profile",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()
        assertNotNull(authResponse.accessToken)

        val profileResponse = client.get("/api/auth/profile") {
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, profileResponse.status)
        val profile = profileResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertEquals("profile.user@example.com", profile.email)
    }
}

private fun ApplicationTestBuilder.configureAuthTestModule(): UserRepository {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testJwtService()
    val userRepository = UserRepositoryImpl(database)
    val refreshTokenRepository = RefreshTokenRepositoryImpl(
        database = database,
        refreshTokenExpiration = jwtService.refreshTokenExpiration,
    )
    val passwordResetTokenRepository = PasswordResetTokenRepositoryImpl(database)
    val authService =
        AuthService(userRepository, refreshTokenRepository, passwordResetTokenRepository, jwtService, null)

    application {
        authTestModule(authService, jwtService)
    }

    return userRepository
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

private fun Application.authTestModule(authService: AuthService, jwtService: JwtService) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "comp2300-test"
            verifier(jwtService.verifier)
            validate { credential ->
                val userId = credential.payload.subject
                if (userId != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authRoutes(authService)
    }
}

private fun testJwtService(): JwtService = JwtServiceImpl(
    secret = "test-secret",
    issuer = "http://localhost/test",
    audience = "http://localhost/test",
)

@Serializable
private data class ErrorResponse(val error: String)
