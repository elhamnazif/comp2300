package com.group8.comp2300.data.repository

import com.group8.comp2300.data.mapper.toDomain
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository

class ShopRepositoryImpl(private val apiService: ApiService) : ShopRepository {
    override suspend fun getAllProducts(): List<Product> = 
        apiService.getProducts().map { it.toDomain() }

    override suspend fun getProductsByCategory(category: ProductCategory): List<Product> {
        val allProducts = getAllProducts()
        return if (category == ProductCategory.ALL) {
            allProducts
        } else {
            allProducts.filter { it.category == category }
        }
    }

    override suspend fun getProductById(id: String): Product {
        return apiService.getProduct(id).toDomain()
    }
}
