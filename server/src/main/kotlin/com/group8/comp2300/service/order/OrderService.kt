package com.group8.comp2300.service.order

import com.group8.comp2300.domain.model.shop.CartItem
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.OrderStatus
import com.group8.comp2300.domain.model.shop.PlaceOrderRequest
import com.group8.comp2300.domain.repository.OrderRepository
import com.group8.comp2300.domain.repository.ProductRepository
import java.util.*
import kotlin.time.Clock

class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
) {
    fun placeOrder(userId: String, request: PlaceOrderRequest): Order {
        val shippingAddress = request.shippingAddress.trim()
        require(shippingAddress.isNotEmpty()) { "Shipping address is required" }

        val items = request.items.normalizeWithCatalog(productRepository)
        require(items.isNotEmpty()) { "Cart is empty" }

        val now = Clock.System.now().toEpochMilliseconds()
        val order = Order(
            id = UUID.randomUUID().toString(),
            userId = userId,
            items = items,
            status = OrderStatus.CONFIRMED,
            subtotal = items.sumOf { it.priceAtAdd * it.quantity },
            createdAt = now,
            updatedAt = now,
            shippingAddress = shippingAddress,
        )
        orderRepository.insert(order)
        return order
    }

    fun getOrdersForUser(userId: String): List<Order> = orderRepository.getByUserId(userId)
}

private fun List<CartItem>.normalizeWithCatalog(productRepository: ProductRepository): List<CartItem> =
    groupBy(CartItem::productId).map { (productId, items) ->
        val quantity = items.sumOf(CartItem::quantity)
        require(quantity > 0) { "Quantity must be at least 1" }
        val product = productRepository.getById(productId)
            ?: throw IllegalArgumentException("Product $productId not found")
        CartItem(
            productId = productId,
            quantity = quantity,
            priceAtAdd = product.effectivePrice,
        )
    }
