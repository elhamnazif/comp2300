package com.group8.comp2300.routes

import com.group8.comp2300.dto.*
import com.group8.comp2300.service.auth.AuthService
import com.group8.comp2300.service.auth.ProfileImageStorage
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import java.io.ByteArrayOutputStream

private const val DUPLICATE_ACCOUNT_MESSAGE = "An account with this email already exists"
private const val FORGOT_PASSWORD_RESPONSE_MESSAGE =
    "If an account with that email exists, a password reset link has been sent"

fun Route.authRoutes(authService: AuthService) {
    post("/api/auth/login") {
        val request = call.receive<LoginRequest>()
        call.respondResult(
            result = authService.login(request),
            onFailure = { error ->
                respondExpectedFailure(
                    error = error,
                    expectedStatus = HttpStatusCode.Unauthorized,
                    defaultMessage = "Login failed",
                )
            },
        )
    }

    post("/api/auth/refresh") {
        val request = call.receive<RefreshTokenRequest>()
        call.respondResult(
            result = authService.refreshToken(request.refreshToken),
            onFailure = { error ->
                respondExpectedFailure(
                    error = error,
                    expectedStatus = HttpStatusCode.Unauthorized,
                    defaultMessage = "Token refresh failed",
                )
            },
        )
    }

    post("/api/auth/activate") {
        val request = call.receive<ActivateRequest>()
        call.respondActivationResult(authService.activateAccount(request.token))
    }

    get("/api/auth/activate") {
        // GET endpoint for email link activation
        val token = call.request.queryParameters["token"]
        if (token.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing activation token"))
            return@get
        }

        call.respondActivationResult(authService.activateAccount(token))
    }

    post("/api/auth/forgot-password") {
        val request = call.receive<ForgotPasswordRequest>()
        authService.forgotPassword(request.email)
        // Return a generic success response to avoid email enumeration and rate-limit leakage.
        call.respond(HttpStatusCode.OK, MessageResponse(FORGOT_PASSWORD_RESPONSE_MESSAGE))
    }

    post("/api/auth/reset-password") {
        val request = call.receive<ResetPasswordRequest>()
        call.respondResult(
            result = authService.resetPassword(request.token, request.newPassword),
            successBody = MessageResponse("Password has been reset successfully"),
            onFailure = { error ->
                respondError(HttpStatusCode.BadRequest, error.message ?: "Password reset failed")
            },
        )
    }

    post("/api/auth/preregister") {
        val request = call.receive<PreregisterRequest>()
        call.respondResult(
            result = authService.preregister(request),
            onFailure = { error ->
                respondConflictAwareFailure(error, "Preregistration failed")
            },
        )
    }

    post("/api/auth/resend-verification") {
        val request = call.receive<ResendVerificationRequest>()
        call.respondResult(
            result = authService.resendVerificationEmail(request.email),
            successBody = MessageResponse("Verification email sent"),
            onFailure = { error ->
                respondExpectedFailure(
                    error = error,
                    expectedStatus = HttpStatusCode.BadRequest,
                    defaultMessage = "Failed to resend",
                )
            },
        )
    }

    authenticate("auth-jwt") {
        get("/api/auth/profile") {
            withUserId { userId ->
                val user = authService.getUserById(userId)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
                }
            }
        }

        post("/api/auth/logout") {
            withUserId { userId ->
                authService.logout(userId)
                call.respond(HttpStatusCode.OK, MessageResponse("Logged out successfully"))
            }
        }

        put("/api/auth/profile") {
            withUserId { userId ->
                val request = call.receive<UpdateProfileRequest>()
                call.respondResult(
                    result = authService.updateProfile(userId, request),
                    onFailure = { error ->
                        respondExpectedFailure(
                            error = error,
                            expectedStatus = HttpStatusCode.BadRequest,
                            defaultMessage = "Profile update failed",
                        )
                    },
                )
            }
        }

        put("/api/auth/profile/photo") {
            withUserId { userId ->
                val uploadPayload = runCatching {
                    val multipart = call.receiveMultipart()
                    var fileName: String? = null
                    var fileBytes: ByteArray? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> {
                                if (fileBytes == null) {
                                    fileName = part.originalFileName ?: "profile-photo"
                                    fileBytes = part.readBytesWithLimit(ProfileImageStorage.MAX_FILE_SIZE_BYTES)
                                }
                            }

                            else -> Unit
                        }
                        part.dispose()
                    }

                    fileBytes?.let { bytes ->
                        fileName.orEmpty() to bytes
                    } ?: throw IllegalArgumentException("No profile photo provided")
                }.getOrElse { error ->
                    return@withUserId call.respondExpectedFailure(
                        error = error,
                        expectedStatus = HttpStatusCode.BadRequest,
                        defaultMessage = "Profile photo upload failed",
                    )
                }

                call.respondResult(
                    result = authService.uploadProfilePhoto(
                        userId = userId,
                        fileName = uploadPayload.first,
                        fileBytes = uploadPayload.second,
                    ),
                    onFailure = { error ->
                        respondExpectedFailure(
                            error = error,
                            expectedStatus = HttpStatusCode.BadRequest,
                            defaultMessage = "Profile photo upload failed",
                        )
                    },
                )
            }
        }

        delete("/api/auth/profile/photo") {
            withUserId { userId ->
                call.respondResult(
                    result = authService.removeProfilePhoto(userId),
                    onFailure = { error ->
                        respondExpectedFailure(
                            error = error,
                            expectedStatus = HttpStatusCode.BadRequest,
                            defaultMessage = "Profile photo removal failed",
                        )
                    },
                )
            }
        }
    }
}

private suspend fun <T> io.ktor.server.application.ApplicationCall.respondResult(
    result: Result<T>,
    successStatus: HttpStatusCode = HttpStatusCode.OK,
    successBody: Any? = null,
    onFailure: suspend io.ktor.server.application.ApplicationCall.(Throwable) -> Unit,
) {
    result.fold(
        onSuccess = { value ->
            respond(successStatus, successBody ?: value as Any)
        },
        onFailure = { error ->
            onFailure(error)
        },
    )
}

private suspend fun io.ktor.server.application.ApplicationCall.respondActivationResult(result: Result<*>) {
    respondResult(
        result = result,
        onFailure = { error ->
            respondError(HttpStatusCode.BadRequest, error.message ?: "Activation failed")
        },
    )
}

private suspend fun io.ktor.server.application.ApplicationCall.respondConflictAwareFailure(
    error: Throwable,
    defaultMessage: String,
) {
    if (error is IllegalArgumentException) {
        val message = error.message ?: defaultMessage
        val status = if (message == DUPLICATE_ACCOUNT_MESSAGE) HttpStatusCode.Conflict else HttpStatusCode.BadRequest
        respondError(status, message)
        return
    }

    respondError(HttpStatusCode.InternalServerError, defaultMessage)
}

private suspend fun io.ktor.server.application.ApplicationCall.respondExpectedFailure(
    error: Throwable,
    expectedStatus: HttpStatusCode,
    defaultMessage: String,
) {
    if (error is IllegalArgumentException) {
        respondError(expectedStatus, error.message ?: defaultMessage)
        return
    }

    respondError(HttpStatusCode.InternalServerError, defaultMessage)
}

private suspend fun io.ktor.server.application.ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    respond(status, mapOf("error" to message))
}

private suspend fun PartData.FileItem.readBytesWithLimit(maxBytes: Int): ByteArray {
    val channel = provider()
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0

    while (true) {
        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead == -1) break
        if (bytesRead == 0) continue

        totalBytes += bytesRead
        require(totalBytes <= maxBytes) { ProfileImageStorage.FILE_TOO_LARGE_MESSAGE }
        output.write(buffer, 0, bytesRead)
    }

    return output.toByteArray()
}
