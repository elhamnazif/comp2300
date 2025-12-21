package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory

interface ShopRepository {
    /** Return all available products. */
    suspend fun getAllProducts(): List<Product>

    /** Return products filtered by category. If category is [ProductCategory.ALL], returns all products. */
    suspend fun getProductsByCategory(category: ProductCategory): List<Product>

    /** Return a single product by its ID. */
    suspend fun getProductById(id: String): Product
}
