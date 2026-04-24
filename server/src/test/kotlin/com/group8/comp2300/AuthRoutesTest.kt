package com.group8.comp2300

import com.group8.comp2300.data.repository.EmailChangeTokenRepositoryImpl
import com.group8.comp2300.data.repository.PasswordResetTokenRepositoryImpl
import com.group8.comp2300.data.repository.RefreshTokenRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.dto.ActivateRequest
import com.group8.comp2300.dto.AuthResponse
import com.group8.comp2300.dto.ForgotPasswordRequest
import com.group8.comp2300.dto.LoginRequest
import com.group8.comp2300.dto.MessageResponse
import com.group8.comp2300.dto.PreregisterRequest
import com.group8.comp2300.dto.PreregisterResponse
import com.group8.comp2300.dto.RefreshTokenRequest
import com.group8.comp2300.dto.ResendVerificationRequest
import com.group8.comp2300.dto.ResetPasswordRequest
import com.group8.comp2300.dto.TokenResponse
import com.group8.comp2300.dto.UpdateProfileRequest
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.routes.authRoutes
import com.group8.comp2300.routes.withUserId
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.auth.AuthService
import com.group8.comp2300.service.auth.InMemoryVerificationRequestThrottle
import com.group8.comp2300.service.auth.ProfileImageStorage
import com.group8.comp2300.service.auth.VerificationRequestThrottle
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class AuthRoutesTest {
    @Test
    fun loginWithValidCredentialsReturnsSuccess() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "forgot.password@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        fixture.verificationThrottle.clearRequest("forgot.password@example.com")

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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val firstResponse = client.preregister(
            email = "preregister.dupe@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, firstResponse.status)

        val user = fixture.userRepository.findByEmail("preregister.dupe@example.com")
        assertNotNull(user)
        fixture.userRepository.activateUser(user.id)

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
    fun updateProfileRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.put("/api/auth/profile") {
            contentType(ContentType.Application.Json)
            setBody(UpdateProfileRequest(firstName = "Complete", lastName = "Profile"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun updateProfileWithValidDataReturnsUpdatedUser() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "complete.profile@example.com",
            password = "Password1",
        )

        val completeResponse = client.put("/api/auth/profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(
                UpdateProfileRequest(
                    firstName = "Updated",
                    lastName = "ProfileName",
                    phone = "12345678",
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, completeResponse.status, completeResponse.bodyAsText())
        val updatedUser = completeResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertEquals("Updated", updatedUser.firstName)
        assertEquals("ProfileName", updatedUser.lastName)
        assertEquals("12345678", updatedUser.phone)
    }

    @Test
    fun updateProfileWithBlankNamesReturnsBadRequest() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "blank.names@example.com",
            password = "Password1",
        )

        val response = client.put("/api/auth/profile") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
            setBody(UpdateProfileRequest(firstName = "", lastName = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            "First name and last name are required",
            response.body<ErrorResponse>().error,
        )
    }

    @Test
    fun profilePhotoRequiresAuthentication() = testApplication {
        configureAuthTestModule()
        val client = jsonClient()

        val response = client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    "image".encodeToByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.png\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun uploadProfilePhotoStoresUrlAndRemovesOldFile() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "photo.user@example.com",
            password = "Password1",
        )

        val firstResponse = client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    "first-image".encodeToByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.png\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, firstResponse.status, firstResponse.bodyAsText())
        val firstUser = firstResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertTrue(firstUser.profileImageUrl?.startsWith("/images/profile/") == true)
        val firstFiles = fixture.profileImageDirectory.listFiles().orEmpty()
        assertEquals(1, firstFiles.size)

        val secondResponse = client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    "second-image".encodeToByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.webp\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, secondResponse.status, secondResponse.bodyAsText())
        val secondUser = secondResponse.body<com.group8.comp2300.domain.model.user.User>()
        assertTrue(secondUser.profileImageUrl?.endsWith(".webp") == true)
        val secondFiles = fixture.profileImageDirectory.listFiles().orEmpty()
        assertEquals(1, secondFiles.size)
        assertFalse(secondFiles.single().name == firstFiles.single().name)
    }

    @Test
    fun deleteProfilePhotoClearsDbStateAndStoredFile() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "remove.photo@example.com",
            password = "Password1",
        )

        client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    "image".encodeToByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.png\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(1, fixture.profileImageDirectory.listFiles().orEmpty().size)

        val response = client.delete("/api/auth/profile/photo") {
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val updatedUser = response.body<com.group8.comp2300.domain.model.user.User>()
        assertNull(updatedUser.profileImageUrl)
        assertTrue(fixture.profileImageDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun invalidProfilePhotoTypeReturnsBadRequest() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "bad.photo@example.com",
            password = "Password1",
        )

        val response = client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    "image".encodeToByteArray(),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.gif\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Profile photo must be JPG, PNG, or WebP", response.body<ErrorResponse>().error)
        assertTrue(fixture.profileImageDirectory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun oversizedProfilePhotoReturnsBadRequest() = testApplication {
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
            email = "large.photo@example.com",
            password = "Password1",
        )

        val response = client.submitFormWithBinaryData(
            url = "/api/auth/profile/photo",
            formData = formData {
                append(
                    "file",
                    ByteArray(5 * 1024 * 1024 + 1),
                    Headers.build {
                        append(HttpHeaders.ContentDisposition, "filename=\"avatar.png\"")
                    },
                )
            },
        ) {
            method = io.ktor.http.HttpMethod.Put
            header(HttpHeaders.Authorization, "Bearer ${authResponse.accessToken}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Profile photo must be 5 MB or smaller", response.body<ErrorResponse>().error)
        assertTrue(fixture.profileImageDirectory.listFiles().orEmpty().isEmpty())
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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val authResponse = createActivatedSession(
            client = client,
            userRepo = fixture.userRepository,
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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "resend.inactive@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        fixture.verificationThrottle.clearRequest("resend.inactive@example.com")

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
        val fixture = configureAuthTestModule()
        val client = jsonClient()

        val preregisterResponse = client.preregister(
            email = "resend.activated@example.com",
            password = "Password1",
        )
        assertEquals(HttpStatusCode.OK, preregisterResponse.status)
        val user = fixture.userRepository.findByEmail("resend.activated@example.com")
        assertNotNull(user)
        fixture.userRepository.activateUser(user.id)
        fixture.verificationThrottle.clearRequest("resend.activated@example.com")

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

private data class AuthTestFixture(
    val userRepository: UserRepository,
    val verificationThrottle: VerificationRequestThrottle,
    val profileImageDirectory: java.io.File,
)

private fun ApplicationTestBuilder.configureAuthTestModule(): AuthTestFixture {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testJwtService()
    val userRepository = UserRepositoryImpl(database)
    val verificationThrottle = InMemoryVerificationRequestThrottle()
    val profileImageDirectory = createTempDirectory("auth-routes-profile-images").toFile()
    val profileImageStorage = ProfileImageStorage(profileImageDirectory.path)
    val refreshTokenRepository = RefreshTokenRepositoryImpl(
        database = database,
        refreshTokenExpiration = jwtService.refreshTokenExpiration,
    )
    val passwordResetTokenRepository = PasswordResetTokenRepositoryImpl(database)
    val emailChangeTokenRepository = EmailChangeTokenRepositoryImpl(database)
    val authService =
        AuthService(
            userRepository = userRepository,
            refreshTokenRepository = refreshTokenRepository,
            passwordResetTokenRepository = passwordResetTokenRepository,
            emailChangeTokenRepository = emailChangeTokenRepository,
            jwtService = jwtService,
            emailService = null,
            verificationRequestThrottle = verificationThrottle,
            profileImageStorage = profileImageStorage,
        )

    application {
        authTestModule(authService, jwtService, userRepository)
    }

    return AuthTestFixture(
        userRepository = userRepository,
        verificationThrottle = verificationThrottle,
        profileImageDirectory = profileImageDirectory,
    )
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

private fun Application.authTestModule(
    authService: AuthService,
    jwtService: JwtService,
    userRepository: UserRepository,
) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }

    install(Koin) {
        modules(
            module {
                single<UserRepository> { userRepository }
            },
        )
    }

    install(Authentication) {
        jwt("auth-jwt") {
            realm = "comp2300-test"
            verifier(jwtService.verifier)
            validate { credential ->
                val userId = credential.payload.subject
                if (userId != null && userRepository.isActive(userId)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authRoutes(authService)
        authenticate("auth-jwt") {
            get("/api/protected") {
                withUserId {
                    call.response.status(HttpStatusCode.OK)
                }
            }
        }
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

private suspend fun HttpClient.login(email: String, password: String): HttpResponse = post("/api/auth/login") {
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
