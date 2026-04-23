package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
data class CartDetails(val cart: Cart, val lines: List<CartLine> = emptyList())
