package com.group8.comp2300.data.repository

import co.touchlab.kermit.Logger
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.data.remote.isTimeoutLike
import com.group8.comp2300.data.remote.toLogSummary
import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.repository.ChatbotRepository
import kotlin.time.Clock

class ChatbotRepositoryImpl(private val apiService: ApiService) : ChatbotRepository {
    private val logger = Logger.withTag("ChatbotRepository")

    override suspend fun send(messages: List<ChatbotMessage>): ChatbotMessage {
        val startedAtMs = Clock.System.now().toEpochMilliseconds()
        logger.d { "Sending chatbot request: messages=${messages.size}" }

        return runCatching {
            apiService.sendChatbotMessage(ChatbotRequest(messages)).message
        }.onSuccess { reply ->
            val elapsedMs = Clock.System.now().toEpochMilliseconds() - startedAtMs
            logger.i {
                "Chatbot request succeeded: messages=${messages.size}, replyLength=${reply.content.length}, elapsedMs=$elapsedMs"
            }
        }.onFailure { error ->
            val elapsedMs = Clock.System.now().toEpochMilliseconds() - startedAtMs
            val reason = if (error.isTimeoutLike()) "timeout" else "error"
            logger.w(error) {
                "Chatbot request failed: reason=$reason, messages=${messages.size}, elapsedMs=$elapsedMs, ${error.toLogSummary()}"
            }
        }.getOrThrow()
    }
}
