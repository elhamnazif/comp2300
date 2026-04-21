package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.dto.ErrorResponse
import com.group8.comp2300.service.chatbot.ChatbotService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ChatbotRoutes")

fun Route.chatbotRoutes() {
    val chatbotService: ChatbotService by inject()

    route("/api/chatbot") {
        post {
            logger.info(
                "Chatbot request received: contentType={}, contentLength={}",
                call.request.headers[HttpHeaders.ContentType] ?: "unknown",
                call.request.headers[HttpHeaders.ContentLength] ?: "unknown",
            )

            val request = try {
                call.receive<ChatbotRequest>()
            } catch (_: ContentTransformationException) {
                logger.warn("Chatbot request body could not be transformed to ChatbotRequest")
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid chatbot request"))
                return@post
            } catch (_: BadRequestException) {
                logger.warn("Chatbot request body was malformed")
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid chatbot request"))
                return@post
            }

            logger.info(
                "Chatbot request parsed: messages={}, lastRole={}",
                request.messages.size,
                request.messages.lastOrNull()?.role ?: "none",
            )

            runCatching { chatbotService.reply(request) }
                .onSuccess { response ->
                    logger.info("Chatbot request succeeded: replyLength={}", response.message.content.length)
                    call.respond(response)
                }
                .onFailure { error ->
                    when (error) {
                        is IllegalArgumentException -> {
                            logger.warn("Chatbot request rejected: {}", error.message)
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ErrorResponse(error.message ?: "Invalid chatbot request"),
                            )
                        }

                        is IllegalStateException -> {
                            logger.warn("Chatbot unavailable: {}", error.message)
                            call.respond(
                                HttpStatusCode.ServiceUnavailable,
                                ErrorResponse("Chatbot is unavailable right now."),
                            )
                        }

                        else -> {
                            logger.error("Chatbot request failed", error)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                ErrorResponse("Chatbot is unavailable right now."),
                            )
                        }
                    }
                }
        }
    }
}
