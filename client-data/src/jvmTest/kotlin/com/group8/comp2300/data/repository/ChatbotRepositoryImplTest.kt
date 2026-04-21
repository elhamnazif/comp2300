package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.model.chatbot.ChatbotResponse
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatbotRepositoryImplTest {
    private val apiService = ChatbotApiServiceStub()
    private val repository = ChatbotRepositoryImpl(apiService)

    @Test
    fun sendForwardsConversationAndReturnsAssistantMessage() = runTest {
        val messages = listOf(
            ChatbotMessage(role = ChatbotRole.USER, content = "How do I book a clinic visit?"),
            ChatbotMessage(role = ChatbotRole.ASSISTANT, content = "Open Care to view nearby clinics."),
        )

        val reply = repository.send(messages)

        assertEquals(messages, apiService.lastMessages)
        assertEquals(ChatbotRole.ASSISTANT, reply.role)
        assertEquals("Use Care to browse clinics and available slots.", reply.content)
    }
}

private class ChatbotApiServiceStub : FakeApiService() {
    var lastMessages: List<ChatbotMessage> = emptyList()

    override suspend fun sendChatbotMessage(request: ChatbotRequest): ChatbotResponse {
        lastMessages = request.messages
        return ChatbotResponse(
            message = ChatbotMessage(
                role = ChatbotRole.ASSISTANT,
                content = "Use Care to browse clinics and available slots.",
            ),
        )
    }
}
