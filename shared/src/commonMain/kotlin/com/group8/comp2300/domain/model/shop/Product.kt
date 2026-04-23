package com.group8.comp2300.domain.model.shop

import com.group8.comp2300.util.formatCurrency
import kotlinx.serialization.Serializable

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
    val effectivePrice: Double
        get() = if (insuranceCovered) 0.0 else price

    /** Formatted price for display */
    val formattedPrice: String
        get() = formatCurrency(price)
}
