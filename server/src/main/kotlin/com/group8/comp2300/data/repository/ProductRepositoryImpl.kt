package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ProductEntity
import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ProductRepository

class ProductRepositoryImpl(private val database: ServerDatabase) : ProductRepository {

    override fun getAll(): List<Product> =
        database.productQueries.selectAllProducts().executeAsList().map { it.toDomain() }

    override fun getById(id: String): Product? =
        database.productQueries.selectProductById(id).executeAsOneOrNull()?.toDomain()

    override fun insert(product: Product) {
        database.productQueries.insertProduct(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category.name,
            insuranceCovered = if (product.insuranceCovered) 1L else 0L,
            imageUrl = product.imageUrl
        )
    }

    override fun update(product: Product) {
        database.productQueries.updateProduct(
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category.name,
            insuranceCovered = if (product.insuranceCovered) 1L else 0L,
            imageUrl = product.imageUrl,
            id = product.id
        )
    }

    override fun delete(id: String) {
        database.productQueries.deleteProduct(id)
    }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        name = name,
        description = description,
        price = price,
        category = ProductCategory.valueOf(category),
        insuranceCovered = insuranceCovered == 1L,
        imageUrl = imageUrl
    )
}
