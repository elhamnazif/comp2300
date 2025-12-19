package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    val userId: String,
    val items: List<CartItem>,
    val status: OrderStatus = OrderStatus.PENDING,
    val subtotal: Double,
    val tax: Double = 0.0,
    val shipping: Double = 0.0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val shippingAddress: String? = null,
    val trackingNumber: String? = null,
) {
    val total: Double
        get() = subtotal + tax + shipping
}
