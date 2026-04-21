package com.group8.comp2300.feature.chatbot

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.repository.ChatbotRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatbotViewModel(private val chatbotRepository: ChatbotRepository) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var activeReplyJob: Job? = null
    private var replyToken: Long = 0L

    fun updateDraftMessage(value: String) {
        _state.update { it.copy(draftMessage = value) }
    }

    fun sendDraftMessage() {
        submitMessage(state.value.draftMessage)
    }

    fun sendPrompt(prompt: String) {
        submitMessage(prompt)
    }

    fun retryLastMessage() {
        val currentState = state.value
        if (currentState.isSending || currentState.errorMessage == null) return
        if (currentState.messages.lastOrNull()?.role != ChatbotRole.USER) return

        _state.update { it.copy(isSending = true, errorMessage = null) }
        requestReply(currentState.messages)
    }

    fun clearConversation() {
        replyToken += 1
        activeReplyJob?.cancel()
        activeReplyJob = null
        _state.value = State()
    }

    private fun submitMessage(rawMessage: String) {
        val currentState = state.value
        val message = rawMessage.trim()
        if (currentState.isSending || message.isEmpty()) return

        val updatedMessages = currentState.messages + ChatbotMessage(
            role = ChatbotRole.USER,
            content = message,
        )

        _state.update {
            it.copy(
                messages = updatedMessages,
                draftMessage = "",
                isSending = true,
                errorMessage = null,
            )
        }

        requestReply(updatedMessages)
    }

    private fun requestReply(messages: List<ChatbotMessage>) {
        val token = ++replyToken
        activeReplyJob?.cancel()
        activeReplyJob = viewModelScope.launch {
            runCatching { chatbotRepository.send(messages) }
                .onSuccess { reply ->
                    if (token != replyToken) return@onSuccess
                    _state.update {
                        it.copy(
                            messages = messages + reply,
                            isSending = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException || token != replyToken) return@onFailure
                    _state.update {
                        it.copy(
                            messages = messages,
                            isSending = false,
                            errorMessage = throwable.userFacingMessage(),
                        )
                    }
                }
            if (token == replyToken) {
                activeReplyJob = null
            }
        }
    }

    @Immutable
    data class State(
        val draftMessage: String = "",
        val messages: List<ChatbotMessage> = emptyList(),
        val isSending: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val canSend: Boolean
            get() = !isSending && draftMessage.isNotBlank()

        val canRetry: Boolean
            get() = !isSending && errorMessage != null && messages.lastOrNull()?.role == ChatbotRole.USER
    }
}

private fun Throwable.userFacingMessage(): String = message
    ?.takeUnless { isTimeoutLikeMessage() }
    ?.trim()
    ?.takeIf(String::isNotEmpty)
    ?: if (isTimeoutLikeMessage()) {
        "The server took too long to respond. Try again."
    } else {
        "Couldn't get a reply right now."
    }

private fun Throwable.isTimeoutLikeMessage(): Boolean = generateSequence(this) { it.cause }
    .any { throwable ->
        val name = throwable::class.simpleName.orEmpty()
        val message = throwable.message.orEmpty()
        name.contains("Timeout", ignoreCase = true) ||
            message.contains("timeout", ignoreCase = true) ||
            message.contains("timed out", ignoreCase = true)
    }
