package com.group8.comp2300.service.order

import com.group8.comp2300.domain.model.shop.CartItem
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.PlaceOrderRequest
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.OrderRepository
import com.group8.comp2300.domain.repository.ProductRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderServiceTest {
    @Test
    fun placeOrderUsesTrustedCatalogPricingInsteadOfClientSubmittedPrice() {
        val repository = RecordingOrderRepository()
        val service = OrderService(
            orderRepository = repository,
            productRepository = FakeProductRepository(
                products = mapOf(
                    "4" to Product(
                        id = "4",
                        name = "Trusted Product",
                        description = "Uses catalog pricing",
                        price = 20.0,
                        category = ProductCategory.PREVENTION,
                    ),
                ),
            ),
        )

        val created = service.placeOrder(
            userId = "user-1",
            request = PlaceOrderRequest(
                items = listOf(CartItem(productId = "4", quantity = 2, priceAtAdd = 0.01)),
                shippingAddress = "123 Test Street",
            ),
        )

        assertEquals(40.0, created.subtotal)
        assertEquals(20.0, created.items.single().priceAtAdd)
        assertEquals(created, repository.inserted.single())
    }
}

private class RecordingOrderRepository : OrderRepository {
    val inserted = mutableListOf<Order>()

    override fun insert(order: Order) {
        inserted += order
    }

    override fun getByUserId(userId: String): List<Order> = inserted.filter { it.userId == userId }
}

private class FakeProductRepository(
    private val products: Map<String, Product>,
) : ProductRepository {
    override fun getAll(): List<Product> = products.values.toList()

    override fun getById(id: String): Product? = products[id]

    override fun insert(product: Product) = error("unused")

    override fun update(product: Product) = error("unused")

    override fun delete(id: String) = error("unused")
}
