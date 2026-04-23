package com.group8.comp2300.routes

import com.group8.comp2300.data.repository.OrderRepositoryImpl
import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.data.repository.UserRepositoryImpl
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.shop.CartItem
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.PlaceOrderRequest
import com.group8.comp2300.domain.repository.OrderRepository
import com.group8.comp2300.domain.repository.ProductRepository
import com.group8.comp2300.domain.repository.UserRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import com.group8.comp2300.security.JwtService
import com.group8.comp2300.security.JwtServiceImpl
import com.group8.comp2300.service.order.OrderService
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class OrderRoutesTest {
    @Test
    fun placeOrderCreatesPersistedOrderForAuthenticatedUser() = testApplication {
        val fixture = configureOrderTestModuleWithUsers()
        val client = jsonClient()

        val createResponse = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                PlaceOrderRequest(
                    items = listOf(CartItem(productId = "1", quantity = 2, priceAtAdd = 0.0)),
                    shippingAddress = "123 Test Street",
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = createResponse.body<Order>()
        assertEquals(fixture.userId, created.userId)
        assertEquals("123 Test Street", created.shippingAddress)
        assertEquals(1, fixture.orderRepository.getByUserId(fixture.userId).size)

        fixture.orderRepository.insert(
            created.copy(
                id = "other-order",
                userId = fixture.otherUserId,
            ),
        )

        val historyResponse = client.get("/api/orders/me") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, historyResponse.status)
        val history = historyResponse.body<List<Order>>()
        assertEquals(listOf(created.id), history.map(Order::id))
    }

    @Test
    fun placeOrderUsesTrustedCatalogPricingInsteadOfClientSubmittedPrice() = testApplication {
        val fixture = configureOrderTestModuleWithUsers()
        val client = jsonClient()

        val createResponse = client.post("/api/orders") {
            header(HttpHeaders.Authorization, "Bearer ${fixture.accessToken}")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                PlaceOrderRequest(
                    items = listOf(CartItem(productId = "4", quantity = 2, priceAtAdd = 0.01)),
                    shippingAddress = "123 Test Street",
                ),
            )
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val created = createResponse.body<Order>()
        assertEquals(40.0, created.subtotal)
        assertEquals(20.0, created.items.single().priceAtAdd)
    }

    @Test
    fun placeOrderRequiresAuthentication() = testApplication {
        configureOrderTestModuleWithUsers()
        val client = jsonClient()

        val response = client.post("/api/orders") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(
                PlaceOrderRequest(
                    items = listOf(CartItem(productId = "1", quantity = 1, priceAtAdd = 0.0)),
                    shippingAddress = "123 Test Street",
                ),
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

private data class OrderRouteFixture(
    val accessToken: String,
    val userId: String,
    val otherUserId: String,
    val orderRepository: OrderRepository,
)

private fun ApplicationTestBuilder.configureOrderTestModuleWithUsers(): OrderRouteFixture {
    val database = createServerDatabase("jdbc:sqlite::memory:")
    val jwtService = testOrderJwtService()
    val userRepository = UserRepositoryImpl(database)
    val productRepository = ProductRepositoryImpl(database)
    val orderRepository = OrderRepositoryImpl(database)

    val userId = "user_order_primary"
    val otherUserId = "user_order_other"
    listOf(userId to "primary@example.com", otherUserId to "other@example.com").forEach { (id, email) ->
        userRepository.insert(
            id = id,
            email = email,
            passwordHash = "hash",
            firstName = "Order",
            lastName = "User",
            phone = null,
            dateOfBirth = null,
            gender = null,
            sexualOrientation = null,
            preferredLanguage = "en",
        )
        userRepository.activateUser(id)
    }

    application {
        install(Koin) {
            modules(
                module {
                    single<ServerDatabase> { database }
                    single<UserRepository> { userRepository }
                    single<ProductRepository> { productRepository }
                    single<OrderRepository> { orderRepository }
                    single { OrderService(get(), get()) }
                },
            )
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(io.ktor.server.auth.Authentication) {
            jwt("auth-jwt") {
                realm = "comp2300-test"
                verifier(jwtService.verifier)
                validate { credential ->
                    credential.payload.subject?.let { JWTPrincipal(credential.payload) }
                }
            }
        }
        routing {
            authenticate("auth-jwt") {
                orderRoutes()
            }
        }
    }

    return OrderRouteFixture(
        accessToken = jwtService.generateAccessToken(userId),
        userId = userId,
        otherUserId = otherUserId,
        orderRepository = orderRepository,
    )
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

private fun testOrderJwtService(): JwtService = JwtServiceImpl(
    secret = "test-secret-for-order-routes",
    issuer = "http://localhost/test",
    audience = "http://localhost/test",
)
