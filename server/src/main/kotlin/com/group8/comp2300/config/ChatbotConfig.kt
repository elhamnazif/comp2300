package com.group8.comp2300.config

import org.slf4j.LoggerFactory

object ChatbotConfig {
    private val logger = LoggerFactory.getLogger("ChatbotConfig")

    private const val DEFAULT_CONNECT_TIMEOUT_SECONDS = 10L
    private const val DEFAULT_REQUEST_TIMEOUT_SECONDS = 90L

    val apiKey: String = Environment.value("CHATBOT_API_KEY") ?: ""
    val apiBaseUrl: String = (Environment.value("CHATBOT_API_BASE_URL") ?: "").removeSuffix("/")
    val model: String = Environment.value("CHATBOT_MODEL") ?: ""
    val connectTimeoutSeconds: Long =
        Environment.value("CHATBOT_CONNECT_TIMEOUT_SECONDS")?.toLongOrNull() ?: DEFAULT_CONNECT_TIMEOUT_SECONDS
    val requestTimeoutSeconds: Long =
        Environment.value("CHATBOT_REQUEST_TIMEOUT_SECONDS")?.toLongOrNull() ?: DEFAULT_REQUEST_TIMEOUT_SECONDS
    val isConfigured: Boolean = apiKey.isNotBlank() && apiBaseUrl.isNotBlank() && model.isNotBlank()

    init {
        if (!isConfigured) {
            logger.warn(
                "⚠️ Chatbot is disabled until CHATBOT_API_KEY, CHATBOT_API_BASE_URL, and CHATBOT_MODEL are configured.",
            )
        } else {
            logger.info(
                "✅ Chatbot provider configured with model={}, connectTimeout={}s, requestTimeout={}s",
                model,
                connectTimeoutSeconds,
                requestTimeoutSeconds,
            )
        }
    }
}
