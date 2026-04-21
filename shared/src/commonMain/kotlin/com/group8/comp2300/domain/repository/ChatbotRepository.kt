package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage

interface ChatbotRepository {
    suspend fun send(messages: List<ChatbotMessage>): ChatbotMessage
}
