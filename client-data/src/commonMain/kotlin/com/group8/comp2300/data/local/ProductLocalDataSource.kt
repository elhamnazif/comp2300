package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory

class ProductLocalDataSource(private val database: AppDatabase) {

    fun getAll(): List<Product> =
        database.appDatabaseQueries.selectAllProducts().executeAsList().map { entity ->
            Product(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                price = entity.price,
                category = try {
                    ProductCategory.valueOf(entity.category)
                } catch (_: Exception) {
                    ProductCategory.ALL
                },
                insuranceCovered = entity.insuranceCovered != 0L,
                imageUrl = entity.imageUrl,
            )
        }

    fun getById(id: String): Product? =
        database.appDatabaseQueries.selectProductById(id).executeAsOneOrNull()?.let { entity ->
            Product(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                price = entity.price,
                category = try {
                    ProductCategory.valueOf(entity.category)
                } catch (_: Exception) {
                    ProductCategory.ALL
                },
                insuranceCovered = entity.insuranceCovered != 0L,
                imageUrl = entity.imageUrl,
            )
        }

    fun insert(product: Product) {
        database.appDatabaseQueries.insertProduct(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category.name,
            insuranceCovered = if (product.insuranceCovered) 1L else 0L,
            imageUrl = product.imageUrl,
        )
    }

    fun replaceAll(products: List<Product>) {
        database.appDatabaseQueries.transaction {
            database.appDatabaseQueries.deleteAllProducts()
            products.forEach { insert(it) }
        }
    }

    fun deleteAll() {
        database.appDatabaseQueries.deleteAllProducts()
    }
}
