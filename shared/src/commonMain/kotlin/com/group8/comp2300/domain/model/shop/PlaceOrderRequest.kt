package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class PlaceOrderRequest(
    val items: List<CartItem>,
    val shippingAddress: String,
)
