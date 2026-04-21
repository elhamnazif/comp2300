package com.group8.comp2300.domain.model.chatbot

import kotlinx.serialization.Serializable

@Serializable
enum class ChatbotRole {
    USER,
    ASSISTANT,
}

@Serializable
data class ChatbotMessage(val role: ChatbotRole, val content: String)

@Serializable
data class ChatbotRequest(val messages: List<ChatbotMessage>)

@Serializable
data class ChatbotResponse(val message: ChatbotMessage)
