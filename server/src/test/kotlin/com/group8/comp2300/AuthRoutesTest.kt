package com.group8.comp2300

import com.group8.comp2300.data.repository.PasswordResetTokenRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.ActivateRequest
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.CompleteProfileRequest
import com.group8.comp2300.dto.ForgotPasswordRequest
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.MessageResponse
import com.group8.comp2300.dto.PreregisterRequest
import com.group8.comp2300.dto.PreregisterResponse
import com.group8.comp2300.dto.RefreshTokenRequest
import com.group8.comp2300.dto.ResendVerificationRequest
import com.group8.comp2300.dto.ResetPasswordRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.authRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.auth.AuthService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
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

class AuthRoutesTest {
    @Test
    fun loginWithValidCredentialsReturnsSuccess() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "login.user@example.com",
            password = "Password1",
        )

        assertEquals("login.user@example.com", authResponse.user.email)
        assertTrue(authResponse.accessToken.isNotBlank())
        assertTrue(authResponse.refreshToken.isNotBlank())
    }

    @Test
    fun loginWithWrongPasswordReturnsUnauthorized() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "wrong.password@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)

        val loginResponse = client.login(
            email = "wrong.password@example.com",
            password = "WrongPassword1",
        )

        assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
        assertEquals("Invalid email or password", loginResponse.body<ErrorResponse>().error)
    }

    @Test
    fun refreshWithValidTokenReturnsNewTokenPair() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "refresh.user@example.com",
            password = "Password1",
        )

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
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "profile.user@example.com",
            password = "Password1",
        )

        val profileResponse = client.get("/api/auth/profile") {
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, profileResponse.status)
        val profile = profileResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertEquals("profile.user@example.com", profile.email)
    }

    @Test
    fun loginWithInactiveAccountReturnsUnauthorized() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "inactive.user@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)

        val loginResponse = client.login(
            email = "inactive.user@example.com",
            password = "Password1",
        )

        assertEquals(HttpStatusCode.Unauthorized, loginResponse.status)
        assertEquals(
            "Please activate your account before logging in",
            loginResponse.body<ErrorResponse>().error,
        )
    }

    @Test
    fun activateAccountWithMissingTokenReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.get("/api/auth/activate")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Missing activation token", response.body<ErrorResponse>().error)
    }

    @Test
    fun activateAccountWithInvalidTokenReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/activate") {
            contentType(ContentType.Application.Json)
            setBody(ActivateRequest(token = "invalid-token"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid or expired activation token", response.body<ErrorResponse>().error)
    }

    @Test
    fun forgotPasswordReturnsSuccessEvenForNonExistentEmail() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email = "nonexistent@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "If an account with that email exists, a password reset link has been sent",
            response.body<MessageResponse>().message,
        )
    }

    @Test
    fun forgotPasswordReturnsSuccessForExistingEmail() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "forgot.password@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        userRepo.clearVerificationRequest("forgot.password@example.com")

        val response = client.post("/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email = "forgot.password@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "If an account with that email exists, a password reset link has been sent",
            response.body<MessageResponse>().message,
        )
    }

    @Test
    fun resetPasswordWithInvalidTokenReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(token = "invalid-token", newPassword = "NewPassword1"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid or expired reset token", response.body<ErrorResponse>().error)
    }

    @Test
    fun resetPasswordWithWeakPasswordReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordRequest(token = "some-token", newPassword = "weak"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.body<ErrorResponse>().error.contains("Password"))
    }

    @Test
    fun preregisterReturnsSuccessWithValidData() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.preregister(
            email = "preregister@example.com",
            password = "Password1",
        )

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val payload = response.body<PreregisterResponse>()
        assertEquals("preregister@example.com", payload.email)
        assertTrue(payload.message.isNotBlank())
    }

    @Test
    fun preregisterWithInvalidEmailReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.preregister(
            email = "invalid-email",
            password = "Password1",
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid email format", response.body<ErrorResponse>().error)
    }

    @Test
    fun preregisterWithWeakPasswordReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.preregister(
            email = "weak.password@example.com",
            password = "weak",
        )

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.body<ErrorResponse>().error.contains("Password"))
    }

    @Test
    fun preregisterDuplicateEmailReturnsConflict() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val firstResponse = client.preregister(
            email = "preregister.dupe@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, firstResponse.status)

        val user = userRepo.findByEmail("preregister.dupe@example.com")
        assertNotNull(user)
        userRepo.activateUser(user.id)

        val secondResponse = client.preregister(
            email = "preregister.dupe@example.com",
            password = "Password1",
        )

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        assertEquals(
            "An account with this email already exists",
            secondResponse.body<ErrorResponse>().error,
        )
    }

    @Test
    fun preregisterWithUnverifiedEmailSucceeds() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val firstResponse = client.preregister(
            email = "preregister.unverified@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, firstResponse.status)

        val secondResponse = client.preregister(
            email = "preregister.unverified@example.com",
            password = "NewPassword1",
        )

        assertEquals(HttpStatusCode.OK, secondResponse.status, secondResponse.bodyAsText())
        val response = secondResponse.body<PreregisterResponse>()
        assertEquals("preregister.unverified@example.com", response.email)
    }

    @Test
    fun completeProfileRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            setBody(CompleteProfileRequest(firstName = "Complete", lastName = "Profile"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun completeProfileWithValidDataReturnsUpdatedUser() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "complete.profile@example.com",
            password = "Password1",
        )

        val completeResponse = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(CompleteProfileRequest(firstName = "Updated", lastName = "ProfileName"))
        }

        assertEquals(HttpStatusCode.OK, completeResponse.status, completeResponse.bodyAsText())
        val updatedUser = completeResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertEquals("Updated", updatedUser.firstName)
        assertEquals("ProfileName", updatedUser.lastName)
    }

    @Test
    fun completeProfileWithBlankNamesReturnsBadRequest() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "blank.names@example.com",
            password = "Password1",
        )

        val response = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(CompleteProfileRequest(firstName = "", lastName = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "First name and last name are required",
            response.body<ErrorResponse>().error,
        )
    }

    @Test
    fun logoutRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/logout")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun logoutRevokesRefreshTokens() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = userRepo,
            email = "logout.user@example.com",
            password = "Password1",
        )

        val logoutResponse = client.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, logoutResponse.status)

        val refreshResponse = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(authResponse.refreshToken))
        }
        assertEquals(HttpStatusCode.Unauthorized, refreshResponse.status)
    }

    @Test
    fun resendVerificationReturnsSuccessForExistingInactiveUser() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "resend.inactive@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        userRepo.clearVerificationRequest("resend.inactive@example.com")

        val response = client.post("/api/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationRequest(email = "resend.inactive@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        assertEquals("Verification email sent", response.body<MessageResponse>().message)
    }

    @Test
    fun resendVerificationReturnsSuccessForNonExistentEmail() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationRequest(email = "nonexistent.resend@example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Verification email sent", response.body<MessageResponse>().message)
    }

    @Test
    fun resendVerificationReturnsBadRequestForActivatedAccount() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "resend.activated@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        val user = userRepo.findByEmail("resend.activated@example.com")
        assertNotNull(user)
        userRepo.activateUser(user.id)
        userRepo.clearVerificationRequest("resend.activated@example.com")

        val response = client.post("/api/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationRequest(email = "resend.activated@example.com"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Account is already activated", response.body<ErrorResponse>().error)
    }

    @Test
    fun resendVerificationRateLimitedAfterPreregister() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "resend.ratelimited@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)

        val response = client.post("/api/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(ResendVerificationRequest(email = "resend.ratelimited@example.com"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "Please wait before requesting another verification email",
            response.body<ErrorResponse>().error,
        )
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

private suspend fun HttpClient.preregister(email: String, password: String): HttpResponse =
    post("/api/auth/preregister") {
        contentType(ContentType.Application.Json)
        setBody(PreregisterRequest(email = email, password = password))
    }

private suspend fun HttpClient.login(email: String, password: String): HttpResponse =
    post("/api/auth/login") {
        contentType(ContentType.Application.Json)
        setBody(LoginRequest(email = email, password = password))
    }

private suspend fun createActivatedSession(
    client: HttpClient,
    userRepo: UserRepository,
    email: String,
    password: String,
): AuthResponse {
    val preregisterResponse = client.preregister(email = email, password = password)
    assertEquals(HttpStatusCode.OK, preregisterResponse.status, preregisterResponse.bodyAsText())

    val user = userRepo.findByEmail(email)
    assertNotNull(user)
    userRepo.activateUser(user.id)

    val loginResponse = client.login(email = email, password = password)
    assertEquals(HttpStatusCode.OK, loginResponse.status, loginResponse.bodyAsText())
    return loginResponse.body()
}

@Serializable
private data class ErrorResponse(val error: String)
