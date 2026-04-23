package com.group8.comp2300.service.chatbot

import com.group8.comp2300.config.ChatbotConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

interface ChatbotProviderClient {
    suspend fun reply(messages: List<ProviderMessage>): String
}

class GoogleChatbotProviderClient(private val config: ChatbotConfig = ChatbotConfig) : ChatbotProviderClient {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds))
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun reply(messages: List<ProviderMessage>): String = withContext(Dispatchers.IO) {
        if (!config.isConfigured) {
            throw IllegalStateException("Chatbot is unavailable right now.")
        }

        logger.info(
            "Chatbot provider request: baseUrl={}, model={}, messages={}, connectTimeout={}s, requestTimeout={}s",
            config.apiBaseUrl,
            config.model,
            messages.size,
            config.connectTimeoutSeconds,
            config.requestTimeoutSeconds,
        )

        val requestBody = json.encodeToString(
            messages.toGoogleRequest(),
        )

        val request = HttpRequest.newBuilder()
            .uri(URI.create("${config.apiBaseUrl}/models/${config.model}:generateContent"))
            .header("X-goog-api-key", config.apiKey)
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val startedAt = System.currentTimeMillis()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (error: Exception) {
            logger.error("Chatbot provider request failed before response", error)
            throw IllegalStateException("Chatbot provider request failed", error)
        }

        val elapsedMs = System.currentTimeMillis() - startedAt
        logger.info("Chatbot provider response: status={}, elapsedMs={}", response.statusCode(), elapsedMs)
        if (response.statusCode() !in 200..299) {
            logger.warn(
                "Chatbot provider non-success response: status={}, body={}",
                response.statusCode(),
                response.body().truncateForLog(),
            )
            throw IllegalStateException("Chatbot provider request failed (${response.statusCode()})")
        }

        val payload = try {
            json.decodeFromString<GoogleGenerateContentResponse>(response.body())
        } catch (error: Exception) {
            logger.error(
                "Chatbot provider response could not be parsed: status={}, body={}",
                response.statusCode(),
                response.body().truncateForLog(),
                error,
            )
            throw IllegalStateException("Chatbot provider returned an invalid reply", error)
        }
        val reply = payload.candidates
            .firstOrNull()
            ?.content
            ?.parts
            ?.mapNotNull { it.text?.trim()?.takeIf(String::isNotEmpty) }
            ?.joinToString(separator = "\n")
            ?.takeIf(String::isNotEmpty)
            ?: throw IllegalStateException("Chatbot provider returned an empty reply")

        logger.info("Chatbot provider reply parsed: replyLength={}", reply.length)
        reply
    }

    private companion object {
        private val logger = LoggerFactory.getLogger("ChatbotProviderClient")
    }
}

private fun String.truncateForLog(maxLength: Int = 300): String =
    if (length <= maxLength) this else take(maxLength) + "..."

@Serializable
data class ProviderMessage(val role: String, val content: String)

@Serializable
private data class GoogleGenerateContentRequest(
    val contents: List<GoogleContent>,
    val systemInstruction: GoogleContent? = null,
    val generationConfig: GoogleGenerationConfig = GoogleGenerationConfig(temperature = 0.2),
)

@Serializable
private data class GoogleContent(val role: String? = null, val parts: List<GooglePart>)

@Serializable
private data class GooglePart(val text: String? = null)

@Serializable
private data class GoogleGenerationConfig(val temperature: Double)

@Serializable
private data class GoogleGenerateContentResponse(val candidates: List<GoogleCandidate> = emptyList())

@Serializable
private data class GoogleCandidate(val content: GoogleContent? = null)

private fun List<ProviderMessage>.toGoogleRequest(): GoogleGenerateContentRequest {
    val systemMessage = firstOrNull { it.role == "system" }?.content
    val contents = filterNot { it.role == "system" }
        .map { message ->
            GoogleContent(
                role = when (message.role) {
                    "assistant" -> "model"
                    else -> "user"
                },
                parts = listOf(GooglePart(text = message.content)),
            )
        }

    require(contents.isNotEmpty()) { "Chatbot provider request requires at least one non-system message" }

    return GoogleGenerateContentRequest(
        contents = contents,
        systemInstruction = systemMessage?.let { GoogleContent(parts = listOf(GooglePart(text = it))) },
    )
}
