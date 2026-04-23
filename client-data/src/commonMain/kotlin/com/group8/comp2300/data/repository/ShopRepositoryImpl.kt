package com.group8.comp2300.data.repository

import com.group8.comp2300.data.local.CartLocalDataSource
import com.group8.comp2300.data.local.ProductLocalDataSource
import com.group8.comp2300.data.mapper.toDomain
import com.group8.comp2300.data.offline.cacheFirstRead
import com.group8.comp2300.data.remote.ApiService
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.shop.Cart
import com.group8.comp2300.domain.model.shop.CartDetails
import com.group8.comp2300.domain.model.shop.CartItem
import com.group8.comp2300.domain.model.shop.CartLine
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ShopRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ShopRepositoryImpl(
    private val apiService: ApiService,
    private val productLocal: ProductLocalDataSource,
    private val cartLocal: CartLocalDataSource,
    private val authRepository: AuthRepository,
) : ShopRepository {
    private companion object {
        const val GuestCartUserId = "__guest__"
    }

    private val guestCartMigrationMutex = Mutex()

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

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCart(): Flow<Cart> = authRepository.session.flatMapLatest { session ->
        flow {
            val signedIn = session as? AuthSession.SignedIn
            val userId = signedIn?.user?.id ?: GuestCartUserId
            if (signedIn != null) {
                migrateGuestCartIfNeeded(userId)
            }
            emitAll(
                cartLocal.observeItems(userId).map { items ->
                    Cart(userId = userId, items = items)
                },
            )
        }
    }

    override fun observeCartDetails(): Flow<CartDetails> = observeCart().map { cart ->
        CartDetails(
            cart = cart,
            lines = cart.items.map { item ->
                CartLine(
                    item = item,
                    product = productLocal.getById(item.productId),
                )
            },
        )
    }

    override suspend fun addToCart(product: Product, quantity: Int) {
        require(quantity > 0) { "Quantity must be at least 1" }
        val userId = currentCartUserId()
        val existing = cartLocal.getItems(userId).firstOrNull { it.productId == product.id }
        cartLocal.upsertItem(
            userId = userId,
            item = CartItem(
                productId = product.id,
                quantity = (existing?.quantity ?: 0) + quantity,
                priceAtAdd = existing?.priceAtAdd ?: product.effectivePrice,
            ),
        )
    }

    override suspend fun updateCartQuantity(productId: String, quantity: Int) {
        val userId = currentCartUserId()
        if (quantity <= 0) {
            cartLocal.removeItem(userId, productId)
        } else {
            val existing = cartLocal.getItems(userId).firstOrNull { it.productId == productId }
                ?: throw NoSuchElementException("Cart item $productId not found")
            cartLocal.upsertItem(
                userId = userId,
                item = existing.copy(quantity = quantity),
            )
        }
    }

    override suspend fun removeFromCart(productId: String) {
        val userId = currentCartUserId()
        cartLocal.removeItem(userId, productId)
    }

    override suspend fun clearCart() {
        val userId = currentCartUserId()
        cartLocal.clearCart(userId)
    }

    override suspend fun placeOrder(shippingAddress: String): Order {
        val userId = requireSignedInUserId()
        val cartItems = cartLocal.getItems(userId)
        require(cartItems.isNotEmpty()) { "Your cart is empty" }

        val order = apiService.placeOrder(
            com.group8.comp2300.domain.model.shop.PlaceOrderRequest(
                items = cartItems,
                shippingAddress = shippingAddress.trim(),
            ),
        )
        cartLocal.clearCart(userId)
        return order
    }

    override suspend fun getOrders(): List<Order> {
        requireSignedInUserId()
        return apiService.getOrders()
    }

    private fun currentCartUserId(): String =
        (authRepository.session.value as? AuthSession.SignedIn)?.user?.id ?: GuestCartUserId

    private suspend fun migrateGuestCartIfNeeded(userId: String) {
        val migrated = guestCartMigrationMutex.withLock {
            val guestItems = cartLocal.getItems(GuestCartUserId)
            if (guestItems.isEmpty()) {
                return@withLock false
            }

            val existingByProductId = cartLocal.getItems(userId).associateBy(CartItem::productId)
            guestItems.forEach { guestItem ->
                val existing = existingByProductId[guestItem.productId]
                cartLocal.upsertItem(
                    userId = userId,
                    item = guestItem.copy(
                        quantity = guestItem.quantity + (existing?.quantity ?: 0),
                        priceAtAdd = existing?.priceAtAdd ?: guestItem.priceAtAdd,
                    ),
                )
            }
            cartLocal.clearCart(GuestCartUserId)
            true
        }
        if (!migrated) return
    }

    private fun requireSignedInUserId(): String =
        (authRepository.session.value as? AuthSession.SignedIn)?.user?.id ?: error("Sign in required")
}
