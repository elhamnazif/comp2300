package com.group8.comp2300

import com.group8.comp2300.data.repository.PasswordResetTokenRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.*
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.authRoutes
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.auth.AuthService
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
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

    // ============== Account Activation Tests ==============

    @Test
    fun loginWithInactiveAccountReturnsUnauthorized() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val email = "inactive.user@example.com"
        val password = "Password1"

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = email,
                    password = password,
                    firstName = "Inactive",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        // Try to login without activating account
        val loginResponse = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email = email, password = password))
        }

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

    // ============== Forgot Password Tests ==============

    @Test
    fun forgotPasswordReturnsSuccessEvenForNonExistentEmail() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(ForgotPasswordRequest(email = "nonexistent@example.com"))
        }

        // Returns success to prevent email enumeration
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "If an account with that email exists, a password reset link has been sent",
            response.body<MessageResponse>().message,
        )
    }

    @Test
    fun forgotPasswordReturnsSuccessForExistingEmail() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        // Register a user first
        client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "forgot.password@example.com",
                    password = "Password1",
                    firstName = "Forgot",
                    lastName = "Password",
                ),
            )
        }

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

    // ============== Reset Password Tests ==============

    @Test
    fun resetPasswordWithInvalidTokenReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(
                ResetPasswordRequest(
                    token = "invalid-token",
                    newPassword = "NewPassword1",
                ),
            )
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
            setBody(
                ResetPasswordRequest(
                    token = "some-token",
                    newPassword = "weak",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>().error
        assertTrue(
            error.contains("Password"),
            "Expected password validation error but got: $error",
        )
    }

    // ============== Preregister Tests ==============

    @Test
    fun preregisterReturnsSuccessWithValidData() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(
                PreregisterRequest(
                    email = "preregister@example.com",
                    password = "Password1",
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val payload = response.body<PreregisterResponse>()
        assertEquals("preregister@example.com", payload.email)
        assertTrue(payload.message.isNotBlank())
    }

    @Test
    fun preregisterWithInvalidEmailReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(
                PreregisterRequest(
                    email = "invalid-email",
                    password = "Password1",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid email format", response.body<ErrorResponse>().error)
    }

    @Test
    fun preregisterWithWeakPasswordReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(
                PreregisterRequest(
                    email = "weak.password@example.com",
                    password = "weak",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val error = response.body<ErrorResponse>().error
        assertTrue(error.contains("Password"))
    }

    @Test
    fun preregisterDuplicateEmailReturnsConflict() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val request = PreregisterRequest(
            email = "preregister.dupe@example.com",
            password = "Password1",
        )

        val firstResponse = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        assertEquals(HttpStatusCode.OK, firstResponse.status)

        val secondResponse = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Conflict, secondResponse.status)
        assertEquals(
            "An account with this email already exists",
            secondResponse.body<ErrorResponse>().error,
        )
    }

    // ============== Complete Profile Tests ==============

    @Test
    fun completeProfileRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            setBody(
                CompleteProfileRequest(
                    firstName = "Complete",
                    lastName = "Profile",
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun completeProfileWithValidDataReturnsUpdatedUser() = testApplication {
        val userRepo = configureAuthTestModule()
        val client = jsonClient()

        // First preregister
        val preregisterResponse = client.post("/api/auth/preregister") {
            contentType(ContentType.Application.Json)
            setBody(
                PreregisterRequest(
                    email = "complete.profile@example.com",
                    password = "Password1",
                ),
            )
        }
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)

        // Get tokens by logging in - but user is not activated, so we need to use the tokens from register
        // Actually, let's use register to get tokens for a complete profile test
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "complete.profile2@example.com",
                    password = "Password1",
                    firstName = "Initial",
                    lastName = "Name",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()

        // Activate the user account before completing profile
        userRepo.activateUser(authResponse.user.id)

        // Complete the profile
        val completeResponse = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(
                CompleteProfileRequest(
                    firstName = "Updated",
                    lastName = "ProfileName",
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, completeResponse.status, completeResponse.bodyAsText())
        val updatedUser = completeResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertEquals("Updated", updatedUser.firstName)
        assertEquals("ProfileName", updatedUser.lastName)
    }

    @Test
    fun completeProfileWithBlankNamesReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "blank.names@example.com",
                    password = "Password1",
                    firstName = "Test",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()

        val response = client.post("/api/auth/complete-profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(
                CompleteProfileRequest(
                    firstName = "",
                    lastName = "",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "First name and last name are required",
            response.body<ErrorResponse>().error,
        )
    }

    // ============== Logout Tests ==============

    @Test
    fun logoutRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/logout")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun logoutRevokesRefreshTokens() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        // Register and get tokens
        val registerResponse = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "logout.user@example.com",
                    password = "Password1",
                    firstName = "Logout",
                    lastName = "User",
                ),
            )
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()

        // Logout
        val logoutResponse = client.post("/api/auth/logout") {
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, logoutResponse.status)

        // Try to use the old refresh token - should fail
        val refreshResponse = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(authResponse.refreshToken))
        }
        assertEquals(HttpStatusCode.Unauthorized, refreshResponse.status)
    }

    // ============== Password Validation Tests ==============

    @Test
    fun registerWithPasswordMissingDigitReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "no.digit@example.com",
                    password = "PasswordWithoutDigit",
                    firstName = "No",
                    lastName = "Digit",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Password must contain at least one number", response.body<ErrorResponse>().error)
    }

    @Test
    fun registerWithPasswordMissingLetterReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "no.letter@example.com",
                    password = "12345678",
                    firstName = "No",
                    lastName = "Letter",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Password must contain at least one letter", response.body<ErrorResponse>().error)
    }

    @Test
    fun registerWithShortPasswordReturnsBadRequest() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                RegisterRequest(
                    email = "short.password@example.com",
                    password = "Short1",
                    firstName = "Short",
                    lastName = "Password",
                ),
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Password must be at least 8 characters", response.body<ErrorResponse>().error)
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
