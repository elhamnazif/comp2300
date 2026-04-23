package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.shop.Cart
import com.group8.comp2300.domain.model.shop.CartDetails
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import kotlinx.coroutines.flow.Flow

interface ShopRepository {
    /** Return all available products. */
    suspend fun getAllProducts(): List<Product>

    /** Return products filtered by category. If category is [ProductCategory.ALL], returns all products. */
    suspend fun getProductsByCategory(category: ProductCategory): List<Product>

    /** Return a single product by its ID. */
    suspend fun getProductById(id: String): Product

    /** Observe the current signed-in user's local cart. */
    fun observeCart(): Flow<Cart>

    /** Observe the current signed-in user's local cart together with locally cached product details. */
    fun observeCartDetails(): Flow<CartDetails>

    /** Add a product to the current signed-in user's local cart. */
    suspend fun addToCart(product: Product, quantity: Int = 1)

    /** Update the quantity for a product in the current signed-in user's local cart. */
    suspend fun updateCartQuantity(productId: String, quantity: Int)

    /** Remove a product from the current signed-in user's local cart. */
    suspend fun removeFromCart(productId: String)

    /** Clear the current signed-in user's local cart. */
    suspend fun clearCart()

    /** Place an order using the current signed-in user's local cart. */
    suspend fun placeOrder(shippingAddress: String): Order

    /** Return all orders for the current signed-in user. */
    suspend fun getOrders(): List<Order>
}
