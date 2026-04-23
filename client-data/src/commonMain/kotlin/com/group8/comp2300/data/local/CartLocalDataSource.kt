package com.group8.comp2300.data.local

import com.group8.comp2300.data.database.AppDatabase
import com.group8.comp2300.domain.model.shop.CartItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class CartLocalDataSource(private val database: AppDatabase) {
    private val cartSnapshots = MutableStateFlow<Map<String, List<CartItem>>>(emptyMap())

    fun getItems(userId: String): List<CartItem> =
        database.appDatabaseQueries.selectCartItemsByUserId(userId).executeAsList().map { entity ->
            CartItem(
                productId = entity.productId,
                quantity = entity.quantity.toInt(),
                priceAtAdd = entity.priceAtAdd,
            )
        }

    fun observeItems(userId: String): Flow<List<CartItem>> = cartSnapshots
        .map { snapshots -> snapshots[userId] ?: emptyList() }
        .onStart { emit(getItems(userId)) }
        .distinctUntilChanged()

    fun upsertItem(userId: String, item: CartItem) {
        database.appDatabaseQueries.insertCartItem(
            userId = userId,
            productId = item.productId,
            quantity = item.quantity.toLong(),
            priceAtAdd = item.priceAtAdd,
        )
        refreshUser(userId)
    }

    fun removeItem(userId: String, productId: String) {
        database.appDatabaseQueries.deleteCartItem(
            userId = userId,
            productId = productId,
        )
        refreshUser(userId)
    }

    fun clearCart(userId: String) {
        database.appDatabaseQueries.deleteCartByUserId(userId)
        refreshUser(userId)
    }

    private fun refreshUser(userId: String) {
        cartSnapshots.value = cartSnapshots.value + (userId to getItems(userId))
    }
}
