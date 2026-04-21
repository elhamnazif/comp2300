package com.group8.comp2300.service.chatbot

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.model.education.Article
import com.group8.comp2300.domain.model.education.Category
import com.group8.comp2300.domain.repository.ArticleRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatbotServiceTest {
    @Test
    fun refusesDiagnosticPromptWithoutCallingProvider() = runTest {
        val provider = FakeChatbotProviderClient()
        val service = ChatbotService(providerClient = provider, articleRepository = FakeArticleRepository())

        val response = service.reply(
            ChatbotRequest(
                messages = listOf(
                    ChatbotMessage(
                        role = ChatbotRole.USER,
                        content = "Can you diagnose this rash and tell me what medication to take?",
                    ),
                ),
            ),
        )

        assertFalse(provider.wasCalled)
        assertEquals(ChatbotRole.ASSISTANT, response.message.role)
        assertEquals(true, response.message.content.contains("I can help with app support and general education"))
    }

    @Test
    fun includesMatchedEducationContextBeforeCallingProvider() = runTest {
        val provider = FakeChatbotProviderClient(reply = "Use Education for articles and Care for appointments.")
        val service = ChatbotService(providerClient = provider, articleRepository = FakeArticleRepository())

        val response = service.reply(
            ChatbotRequest(
                messages = listOf(
                    ChatbotMessage(
                        role = ChatbotRole.USER,
                        content = "How can I learn about PrEP in the app?",
                    ),
                ),
            ),
        )

        assertEquals("Use Education for articles and Care for appointments.", response.message.content)
        assertEquals(true, provider.lastMessages.first().content.contains("PrEP Basics"))
    }

    @Test
    fun allowsPregnancyAndEmergencyContraceptionEducationQuestions() = runTest {
        val provider =
            FakeChatbotProviderClient(reply = "Emergency contraception can help prevent pregnancy after sex.")
        val service = ChatbotService(providerClient = provider, articleRepository = FakeArticleRepository())

        val response = service.reply(
            ChatbotRequest(
                messages = listOf(
                    ChatbotMessage(
                        role = ChatbotRole.USER,
                        content = "What is emergency contraception and how do pregnancy tests work?",
                    ),
                ),
            ),
        )

        assertTrue(provider.wasCalled)
        assertEquals(
            "Emergency contraception can help prevent pregnancy after sex.",
            response.message.content,
        )
    }

    @Test
    fun allowsMedicationAppNavigationQuestions() = runTest {
        val provider = FakeChatbotProviderClient(reply = "Open Track to add reminders from the medication screen.")
        val service = ChatbotService(providerClient = provider, articleRepository = FakeArticleRepository())

        val response = service.reply(
            ChatbotRequest(
                messages = listOf(
                    ChatbotMessage(
                        role = ChatbotRole.USER,
                        content = "Which medication screen lets me add reminders?",
                    ),
                ),
            ),
        )

        assertTrue(provider.wasCalled)
        assertEquals(
            "Open Track to add reminders from the medication screen.",
            response.message.content,
        )
    }

    @Test
    fun refusesEmergencyTriageIntent() = runTest {
        val provider = FakeChatbotProviderClient()
        val service = ChatbotService(providerClient = provider, articleRepository = FakeArticleRepository())

        val response = service.reply(
            ChatbotRequest(
                messages = listOf(
                    ChatbotMessage(
                        role = ChatbotRole.USER,
                        content = "Is this an emergency and should I go to the ER?",
                    ),
                ),
            ),
        )

        assertFalse(provider.wasCalled)
        assertTrue(response.message.content.contains("I can help with app support and general education"))
    }
}

private class FakeChatbotProviderClient(private val reply: String = "ok") : ChatbotProviderClient {
    var wasCalled: Boolean = false
    var lastMessages: List<ProviderMessage> = emptyList()

    override suspend fun reply(messages: List<ProviderMessage>): String {
        wasCalled = true
        lastMessages = messages
        return reply
    }
}

private class FakeArticleRepository : ArticleRepository {
    private val article = Article(
        id = "article-1",
        title = "PrEP Basics",
        description = "A quick guide to PrEP and HIV prevention.",
        content = "PrEP overview",
        thumbnailUrl = null,
        publisher = "Care",
        publishedDate = null,
        categories = listOf(Category(id = "cat-1", title = "HIV Prevention")),
    )

    override fun getArticleById(id: String): Article? = article.takeIf { it.id == id }

    override fun getAllArticles(): List<Article> = listOf(article)

    override fun getArticlesPaginated(page: Int, pageSize: Int): List<Article> = listOf(article)

    override fun searchArticles(query: String): List<Article> = listOf(article).filter { article ->
        query.contains("prep", ignoreCase = true) || article.title.contains(query, ignoreCase = true)
    }

    override fun getArticlesByCategory(categoryId: String): List<Article> = listOf(article)

    override fun upsertArticle(article: Article) = Unit

    override fun deleteArticle(id: String) = Unit

    override fun addCategoryToArticle(articleId: String, categoryId: String) = Unit

    override fun removeCategoryFromArticle(articleId: String, categoryId: String) = Unit
}
