package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class CartItem(
        val productId: String,
        val quantity: Int,
        val priceAtAdd: Double // Capture price at time of adding to cart
)