package com.group8.comp2300.data.repository

import com.group8.comp2300.data.local.ProductLocalDataSource
import com.group8.comp2300.data.mapper.toDomain
import com.group8.comp2300.data.offline.cacheFirstRead
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository

class ShopRepositoryImpl(
    private val apiService: ApiService,
    private val productLocal: ProductLocalDataSource,
) : ShopRepository {

    override suspend fun getAllProducts(): List<Product> = cacheFirstRead(
        cached = { productLocal.getAll() },
        fetch = { apiService.getProducts().map { it.toDomain() } },
        save = { productLocal.replaceAll(it) },
    )

    override suspend fun getProductsByCategory(category: ProductCategory): List<Product> {
        val allProducts = getAllProducts()
        return if (category == ProductCategory.ALL) {
            allProducts
        } else {
            allProducts.filter { it.category == category }
        }
    }

    override suspend fun getProductById(id: String): Product = cacheFirstRead(
        cached = { productLocal.getById(id) ?: throw NoSuchElementException("Product $id not cached") },
        fetch = { apiService.getProduct(id).toDomain() },
        save = { productLocal.insert(it) },
    )
}
