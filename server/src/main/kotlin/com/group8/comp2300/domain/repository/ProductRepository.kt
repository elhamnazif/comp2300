package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.shop.Product

interface ProductRepository {
    fun getAll(): List<Product>
    fun getById(id: String): Product?
    fun insert(product: Product)
    fun update(product: Product)
    fun delete(id: String)
}
