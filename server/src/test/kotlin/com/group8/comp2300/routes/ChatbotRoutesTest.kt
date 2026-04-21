package com.group8.comp2300.routes

import com.group8.comp2300.domain.model.chatbot.ChatbotMessage
import com.group8.comp2300.domain.model.chatbot.ChatbotRequest
import com.group8.comp2300.domain.model.chatbot.ChatbotResponse
import com.group8.comp2300.domain.model.chatbot.ChatbotRole
import com.group8.comp2300.domain.model.education.Article
import com.group8.comp2300.domain.repository.ArticleRepository
import com.group8.comp2300.dto.ErrorResponse
import com.group8.comp2300.service.chatbot.ChatbotProviderClient
import com.group8.comp2300.service.chatbot.ChatbotService
import com.group8.comp2300.service.chatbot.ProviderMessage
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class ChatbotRoutesTest {
    @Test
    fun postChatbotReturnsAssistantReply() = testApplication {
        configureChatbotRouteTestModule(
            chatbotService = ChatbotService(
                providerClient = FixedReplyProviderClient("Open Care to book an appointment."),
                articleRepository = EmptyArticleRepository(),
            ),
        )
        val client = jsonClient()

        val response = client.post("/api/chatbot") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody(
                ChatbotRequest(
                    messages = listOf(
                        ChatbotMessage(role = ChatbotRole.USER, content = "How do I book a clinic?"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "Open Care to book an appointment.",
            response.body<ChatbotResponse>().message.content,
        )
    }

    @Test
    fun postChatbotWithoutAuthReturnsUnauthorized() = testApplication {
        configureChatbotRouteTestModule(
            chatbotService = ChatbotService(
                providerClient = FixedReplyProviderClient("unused"),
                articleRepository = EmptyArticleRepository(),
            ),
        )
        val client = jsonClient()

        val response = client.post("/api/chatbot") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatbotRequest(
                    messages = listOf(
                        ChatbotMessage(role = ChatbotRole.USER, content = "Hello"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun postChatbotWithoutAuthAllowsRequestWhenDevBypassIsEnabled() = testApplication {
        configureChatbotRouteTestModule(
            chatbotService = ChatbotService(
                providerClient = FixedReplyProviderClient("Use Track to manage medications."),
                articleRepository = EmptyArticleRepository(),
            ),
            optionalAuth = true,
        )
        val client = jsonClient()

        val response = client.post("/api/chatbot") {
            contentType(ContentType.Application.Json)
            setBody(
                ChatbotRequest(
                    messages = listOf(
                        ChatbotMessage(role = ChatbotRole.USER, content = "Where do I manage medications?"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "Use Track to manage medications.",
            response.body<ChatbotResponse>().message.content,
        )
    }

    @Test
    fun postChatbotWithInvalidBodyReturnsBadRequest() = testApplication {
        configureChatbotRouteTestModule(
            chatbotService = ChatbotService(
                providerClient = FixedReplyProviderClient("unused"),
                articleRepository = EmptyArticleRepository(),
            ),
        )
        val client = jsonClient()

        val response = client.post("/api/chatbot") {
            header(HttpHeaders.Authorization, "Bearer valid-token")
            contentType(ContentType.Text.Plain)
            setBody("hello")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("Invalid chatbot request", response.body<ErrorResponse>().error)
    }

    @Test
    fun postChatbotMapsUnavailableErrorsToServiceUnavailable() = testApplication {
        configureChatbotRouteTestModule(
            chatbotService = ChatbotService(
                providerClient = FailingProviderClient(),
                articleRepository = EmptyArticleRepository(),
            ),
        )
        val client = jsonClient()

        val response = client.post("/api/chatbot") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody(
                ChatbotRequest(
                    messages = listOf(
                        ChatbotMessage(role = ChatbotRole.USER, content = "Hello"),
                    ),
                ),
            )
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("Chatbot is unavailable right now.", response.body<ErrorResponse>().error)
    }

    private fun ApplicationTestBuilder.configureChatbotRouteTestModule(
        chatbotService: ChatbotService,
        optionalAuth: Boolean = false,
    ) {
        application {
            this.install(Koin) {
                modules(
                    module {
                        single { chatbotService }
                    },
                )
            }
            this.install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    },
                )
            }

            this.install(Authentication) {
                bearer("auth-jwt") {
                    authenticate { credentials ->
                        if (credentials.token == "valid-token") {
                            io.ktor.server.auth.UserIdPrincipal("user-1")
                        } else {
                            null
                        }
                    }
                }
            }

            routing {
                authenticate("auth-jwt", optional = optionalAuth) {
                    chatbotRoutes()
                }
            }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ClientContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                },
            )
        }
    }
}

private class FixedReplyProviderClient(private val reply: String) : ChatbotProviderClient {
    override suspend fun reply(messages: List<ProviderMessage>): String = reply
}

private class FailingProviderClient : ChatbotProviderClient {
    override suspend fun reply(messages: List<ProviderMessage>): String =
        throw IllegalStateException("Provider offline")
}

private class EmptyArticleRepository : ArticleRepository {
    override fun getArticleById(id: String): Article? = null

    override fun getAllArticles(): List<Article> = emptyList()

    override fun getArticlesPaginated(page: Int, pageSize: Int): List<Article> = emptyList()

    override fun searchArticles(query: String): List<Article> = emptyList()

    override fun getArticlesByCategory(categoryId: String): List<Article> = emptyList()

    override fun upsertArticle(article: Article) = Unit

    override fun deleteArticle(id: String) = Unit

    override fun addCategoryToArticle(articleId: String, categoryId: String) = Unit

    override fun removeCategoryFromArticle(articleId: String, categoryId: String) = Unit
}
