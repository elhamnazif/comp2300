package com.group8.comp2300.service.chatbot

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.model.chatbot.ChatbotResponse
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.repository.ArticleRepository

class ChatbotService(
    private val providerClient: ChatbotProviderClient,
    private val articleRepository: ArticleRepository,
) {
    suspend fun reply(request: ChatbotRequest): ChatbotResponse {
        val messages = request.messages
            .map { it.copy(content = it.content.trim()) }
            .filter { it.content.isNotEmpty() }

        require(messages.isNotEmpty()) { "Message history is required" }

        val lastUserMessage = messages.lastOrNull { it.role == ChatbotRole.USER }
            ?: throw IllegalArgumentException("A user message is required")

        if (shouldRefuse(lastUserMessage.content)) {
            return ChatbotResponse(
                message = ChatbotMessage(
                    role = ChatbotRole.ASSISTANT,
                    content = REFUSAL_MESSAGE,
                ),
            )
        }

        val providerMessages = buildProviderMessages(messages, lastUserMessage.content)
        val providerReply = providerClient.reply(providerMessages)

        return ChatbotResponse(
            message = ChatbotMessage(
                role = ChatbotRole.ASSISTANT,
                content = providerReply,
            ),
        )
    }

    private fun buildProviderMessages(messages: List<ChatbotMessage>, lastUserMessage: String): List<ProviderMessage> {
        val educationContext = articleRepository.searchArticles(lastUserMessage)
            .take(3)
            .joinToString(separator = "\n") { article ->
                "- ${article.title}: ${article.description}"
            }
            .ifBlank { "- No closely matching article snippets found." }

        val prompt = """
            You are Vita, the in-app assistant for the Vita health app.
            You can help with:
            - app navigation and support
            - clinic booking and feature guidance
            - general sexual health education

            You must not provide:
            - diagnosis
            - medication changes or prescribing advice
            - personalized treatment instructions
            - emergency triage beyond directing the user to urgent help

            When a question crosses those limits, refuse briefly and redirect to a clinician, urgent care, or emergency services when appropriate.
            Keep answers concise and practical.

            Vita app areas:
            - Home: quick actions and daily insight
            - Care: clinics, appointments, and booking
            - Track: calendar, medications, routines, and logs
            - Education: articles and quizzes
            - Me: profile, privacy, notifications, and support

            Relevant education context:
            $educationContext
        """.trimIndent()

        return buildList {
            add(ProviderMessage(role = "system", content = prompt))
            messages.takeLast(12).forEach { message ->
                add(
                    ProviderMessage(
                        role = when (message.role) {
                            ChatbotRole.USER -> "user"
                            ChatbotRole.ASSISTANT -> "assistant"
                        },
                        content = message.content,
                    ),
                )
            }
        }
    }

    private fun shouldRefuse(content: String): Boolean {
        val normalized = content.lowercase()
        return refusalPatterns.any { pattern -> pattern.containsMatchIn(normalized) }
    }

    private companion object {
        val refusalPatterns = listOf(
            Regex("""\bdiagnos\w*"""),
            Regex("""\bwhat\s+medication\s+(should\s+i\s+take|do\s+i\s+need|is\s+best)\b"""),
            Regex("""\bwhich\s+medication\s+should\s+i\s+take\b"""),
            Regex("""\b(change|stop|start)\s+my\s+medication\b"""),
            Regex("""\bshould\s+i\s+take\b"""),
            Regex("""\bwhat\s+dose\b"""),
            Regex("""\bdosage\b"""),
            Regex("""\bprescrib\w*"""),
            Regex("""\bam\s+i\s+pregnan\w*"""),
            Regex("""\bcould\s+i\s+be\s+pregnan\w*"""),
            Regex("""\bis\s+this\s+an?\s+emergency\b"""),
            Regex("""\bdo\s+i\s+need\s+emergency\s+care\b"""),
            Regex("""\bshould\s+i\s+go\s+to\s+(the\s+)?(er|ed|emergency\s+room)\b"""),
            Regex("""\bchest\s+pain\b"""),
            Regex("""\bshortness\s+of\s+breath\b"""),
            Regex("""\bsuicid\w*"""),
        )

        const val REFUSAL_MESSAGE =
            "I can help with app support and general education, but I can’t diagnose symptoms or tell you what medication to take. Please contact a clinician or urgent care for medical advice. If this feels urgent or severe, seek emergency help right away."
    }
}
