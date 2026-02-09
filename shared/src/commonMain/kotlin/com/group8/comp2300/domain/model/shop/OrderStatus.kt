package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus(val displayName: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled")
}
