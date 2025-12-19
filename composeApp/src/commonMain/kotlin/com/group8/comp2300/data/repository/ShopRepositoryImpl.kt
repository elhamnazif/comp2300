package com.group8.comp2300.data.repository

import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.mock.sampleProducts

class ShopRepositoryImpl : ShopRepository {
    override fun getAllProducts(): List<Product> = sampleProducts

    override fun getProductsByCategory(category: ProductCategory): List<Product> =
        if (category == ProductCategory.ALL) {
            sampleProducts
        } else {
            sampleProducts.filter { it.category == category }
        }

    override fun getProductById(id: String): Product? = sampleProducts.find { it.id == id }
}
