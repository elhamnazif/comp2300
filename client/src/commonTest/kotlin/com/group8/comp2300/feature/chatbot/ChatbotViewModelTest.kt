package com.group8.comp2300.feature.chatbot

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.repository.ChatbotRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChatbotViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        kotlinx.coroutines.Dispatchers.resetMain()
    }

    @Test
    fun clearConversationIgnoresInFlightReply() = runTest(dispatcher) {
        val pendingReply = CompletableDeferred<ChatbotMessage>()
        val repository = DeferredChatbotRepository(pendingReply)
        val viewModel = ChatbotViewModel(repository)

        viewModel.updateDraftMessage("How do I book a clinic visit?")
        viewModel.sendDraftMessage()
        advanceUntilIdle()

        viewModel.clearConversation()
        pendingReply.complete(
            ChatbotMessage(
                role = ChatbotRole.ASSISTANT,
                content = "Use Care to browse nearby clinics.",
            ),
        )
        advanceUntilIdle()

        assertEquals(emptyList(), viewModel.state.value.messages)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(false, viewModel.state.value.isSending)
    }

    @Test
    fun sendFailureUsesUnderlyingErrorMessage() = runTest(dispatcher) {
        val repository = FailingChatbotRepository("Request failed (500)")
        val viewModel = ChatbotViewModel(repository)

        viewModel.updateDraftMessage("Hello")
        viewModel.sendDraftMessage()
        advanceUntilIdle()

        assertEquals("Request failed (500)", viewModel.state.value.errorMessage)
        assertEquals(false, viewModel.state.value.isSending)
    }
}

private class DeferredChatbotRepository(private val reply: CompletableDeferred<ChatbotMessage>) : ChatbotRepository {
    override suspend fun send(messages: List<ChatbotMessage>): ChatbotMessage = reply.await()
}

private class FailingChatbotRepository(private val message: String) : ChatbotRepository {
    override suspend fun send(messages: List<ChatbotMessage>): ChatbotMessage = throw IllegalStateException(message)
}
