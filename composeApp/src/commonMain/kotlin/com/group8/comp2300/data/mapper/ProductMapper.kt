package com.group8.comp2300.data.mapper

import com.group8.comp2300.data.remote.dto.ProductDto
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory

fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        name = name,
        description = description,
        price = price,
        category = try {
            ProductCategory.valueOf(category.uppercase())
        } catch (e: Exception) {
            ProductCategory.ALL
        },
        insuranceCovered = insuranceCovered,
        imageUrl = imageUrl
    )
}
