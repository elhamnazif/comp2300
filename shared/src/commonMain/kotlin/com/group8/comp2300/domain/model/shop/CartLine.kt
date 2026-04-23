package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class CartLine(val item: CartItem, val product: Product?) {
    val productId: String
        get() = item.productId

    val quantity: Int
        get() = item.quantity

    val priceAtAdd: Double
        get() = item.priceAtAdd

    val lineTotal: Double
        get() = priceAtAdd * quantity
}
