package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class Cart(val userId: String, val items: List<CartItem> = emptyList()) {
    val itemCount: Int
        get() = items.sumOf { it.quantity }
    val subtotal: Double
        get() = items.sumOf { it.priceAtAdd * it.quantity }
}
