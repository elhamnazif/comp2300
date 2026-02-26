package com.group8.comp2300.routes

import com.group8.comp2300.data.repository.ProductRepositoryImpl
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ProductRepository
import com.group8.comp2300.infrastructure.database.createServerDatabase
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

class ProductRoutesTest {

    @Test
    fun `GET api-products returns list of products`() = testApplication {
        // Given: Setup database with test products
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val productRepository = ProductRepositoryImpl(database)

        val product1 = Product(
            id = "prod_1",
            name = "Vitamin C",
            description = "Daily supplement",
            price = 19.99,
            category = ProductCategory.MEDICATION,
            insuranceCovered = true,
            imageUrl = "http://example.com/vitaminc.jpg"
        )
        val product2 = Product(
            id = "prod_2",
            name = "Pain Relief",
            description = "Fast acting",
            price = 9.99,
            category = ProductCategory.MEDICATION,
            insuranceCovered = false,
            imageUrl = null
        )
        productRepository.insert(product1)
        productRepository.insert(product2)

        configureProductTestModule(productRepository)
        val client = jsonClient()

        // When
        val response = client.get("/api/products")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val products = response.body<List<Product>>()
        assertTrue(products.isNotEmpty())
    }

    @Test
    fun `GET api-products id returns product when exists`() = testApplication {
        // Given
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val productRepository = ProductRepositoryImpl(database)

        val product = Product(
            id = "prod_test",
            name = "Test Product",
            description = "A test product",
            price = 29.99,
            category = ProductCategory.TESTING,
            insuranceCovered = false,
            imageUrl = null
        )
        productRepository.insert(product)

        configureProductTestModule(productRepository)
        val client = jsonClient()

        // When
        val response = client.get("/api/products/prod_test")

        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val returnedProduct = response.body<Product>()
        assertEquals("prod_test", returnedProduct.id)
        assertEquals("Test Product", returnedProduct.name)
        assertEquals(29.99, returnedProduct.price)
    }

    @Test
    fun `GET api-products id returns 404 when not found`() = testApplication {
        val database = createServerDatabase("jdbc:sqlite::memory:")
        val productRepository = ProductRepositoryImpl(database)

        configureProductTestModule(productRepository)
        val client = jsonClient()

        // When
        val response = client.get("/api/products/nonexistent")

        // Then
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("Product not found", response.bodyAsText())
    }

    private fun ApplicationTestBuilder.configureProductTestModule(productRepository: ProductRepository) {
        application {
            install(Koin) {
                modules(
                    module {
                        single<ProductRepository> { productRepository }
                    }
                )
            }
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }

            routing {
                productRoutes()
            }
        }
    }

    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ClientContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }
}
