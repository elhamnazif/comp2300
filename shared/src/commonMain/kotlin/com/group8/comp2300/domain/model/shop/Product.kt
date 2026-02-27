package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable
import net.sergeych.sprintf.format

@Serializable
data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: ProductCategory,
    val insuranceCovered: Boolean = false,
    val imageUrl: String? = null,
) {
    /** Formatted price for display */
    val formattedPrice: String
        get() = "$%.2f".format(price)
}
