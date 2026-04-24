package com.group8.comp2300.data.repository

import com.group8.comp2300.data.local.CartLocalDataSource
import com.group8.comp2300.data.local.ProductLocalDataSource
import com.group8.comp2300.domain.model.session.AuthSession
import com.group8.comp2300.domain.model.shop.*
import com.group8.comp2300.domain.model.user.User
import com.group8.comp2300.domain.repository.AuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ShopRepositoryImplTest {
    @Test
    fun addToCartPersistsQuantityAndPriceSnapshot() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)
        repository.addToCart(product)

        val cartItems = CartLocalDataSource(db).getItems("user-1")
        assertEquals(1, cartItems.size)
        assertEquals(2, cartItems.single().quantity)
        assertEquals(product.effectivePrice, cartItems.single().priceAtAdd)
    }

    @Test
    fun addToCartPersistsZeroSnapshotForInsuredProducts() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val insuredProduct = sampleProduct(price = 19.99, insuranceCovered = true)
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(insuredProduct)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(insuredProduct)

        val cartItems = CartLocalDataSource(db).getItems("user-1")
        assertEquals(0.0, cartItems.single().priceAtAdd)
    }

    @Test
    fun cartIsScopedToSignedInUser() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)
        authRepository.updateSession(signedInSession("user-2"))

        val secondUserCart = repository.observeCart().valueOrThrow()
        assertTrue(secondUserCart.items.isEmpty())
        assertEquals(1, CartLocalDataSource(db).getItems("user-1").size)
    }

    @Test
    fun guestCanAddToCartAndObserveIt() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(AuthSession.SignedOut)
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)

        val guestCart = repository.observeCart().valueOrThrow()
        assertEquals(1, guestCart.items.size)
        assertEquals(1, guestCart.items.single().quantity)
        assertEquals(1, CartLocalDataSource(db).getItems("__guest__").size)
    }

    @Test
    fun guestCartCarriesOverAfterSignIn() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(AuthSession.SignedOut)
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)
        authRepository.updateSession(signedInSession("user-1"))

        val signedInCart = repository.observeCart().valueOrThrow()
        assertEquals(1, signedInCart.items.size)
        assertEquals(1, signedInCart.items.single().quantity)
        assertTrue(CartLocalDataSource(db).getItems("__guest__").isEmpty())
        assertEquals(1, CartLocalDataSource(db).getItems("user-1").size)
    }

    @Test
    fun guestCartMergesOnlyOnceAcrossMultipleObservers() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(AuthSession.SignedOut)
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product)),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)

        val firstObserver = launch { repository.observeCart().first { it.userId == "user-1" } }
        val secondObserver = launch { repository.observeCart().first { it.userId == "user-1" } }

        authRepository.updateSession(signedInSession("user-1"))
        firstObserver.join()
        secondObserver.join()

        val signedInItems = CartLocalDataSource(db).getItems("user-1")
        assertEquals(1, signedInItems.size)
        assertEquals(1, signedInItems.single().quantity)
        assertTrue(CartLocalDataSource(db).getItems("__guest__").isEmpty())
    }

    @Test
    fun placeOrderClearsCartOnlyAfterSuccess() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val product = sampleProduct()
        val api = ShopApiStub(products = listOf(product))
        val repository = ShopRepositoryImpl(
            apiService = api,
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)
        val order = repository.placeOrder("123 Test Street")

        assertEquals("order-1", order.id)
        assertTrue(CartLocalDataSource(db).getItems("user-1").isEmpty())
        assertEquals("123 Test Street", api.lastPlacedOrderRequest?.shippingAddress)
    }

    @Test
    fun observeCartDetailsUsesLocallyCachedProducts() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val productLocal = ProductLocalDataSource(db)
        val cartLocal = CartLocalDataSource(db)
        val product = sampleProduct()
        productLocal.insert(product)
        cartLocal.upsertItem(
            userId = "user-1",
            item = com.group8.comp2300.domain.model.shop.CartItem(
                productId = product.id,
                quantity = 2,
                priceAtAdd = product.effectivePrice,
            ),
        )
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = emptyList()),
            productLocal = productLocal,
            cartLocal = cartLocal,
            authRepository = authRepository,
        )

        val details = repository.observeCartDetails().first()

        assertEquals(1, details.lines.size)
        assertNotNull(details.lines.single().product)
        assertEquals(product.id, details.lines.single().product?.id)
        assertEquals(2, details.lines.single().quantity)
    }

    @Test
    fun failedOrderLeavesCartIntact() = runTest {
        val db = newDatabase()
        val authRepository = MutableSessionAuthRepository(signedInSession("user-1"))
        val product = sampleProduct()
        val repository = ShopRepositoryImpl(
            apiService = ShopApiStub(products = listOf(product), failOnPlaceOrder = true),
            productLocal = ProductLocalDataSource(db),
            cartLocal = CartLocalDataSource(db),
            authRepository = authRepository,
        )

        repository.addToCart(product)

        assertFailsWith<IllegalStateException> {
            repository.placeOrder("123 Test Street")
        }
        assertEquals(1, CartLocalDataSource(db).getItems("user-1").size)
    }
}

private class MutableSessionAuthRepository(initialSession: AuthSession) : AuthRepository {
    private val mutableSession = MutableStateFlow(initialSession)
    override val session: StateFlow<AuthSession> = mutableSession

    fun updateSession(next: AuthSession) {
        mutableSession.value = next
    }

    override suspend fun login(email: String, password: String) = error("unused")
    override suspend fun preregister(email: String, password: String) = error("unused")
    override suspend fun updateProfile(input: com.group8.comp2300.domain.model.user.UpdateProfileInput) =
        error("unused")
    override suspend fun uploadProfilePhoto(fileBytes: ByteArray, fileName: String) = error("unused")
    override suspend fun removeProfilePhoto() = error("unused")
    override suspend fun activateAccount(token: String) = error("unused")
    override suspend fun forgotPassword(email: String) = error("unused")
    override suspend fun resendVerificationEmail(email: String) = error("unused")
    override suspend fun resetPassword(token: String, newPassword: String) = error("unused")
    override suspend fun changePassword(currentPassword: String, newPassword: String) = error("unused")
    override suspend fun requestEmailChange(currentPassword: String, newEmail: String) = error("unused")
    override suspend fun confirmEmailChange(code: String) = error("unused")
    override suspend fun deactivateAccount(currentPassword: String) = error("unused")
    override suspend fun logout() = Unit
}

private class ShopApiStub(private val products: List<Product>, private val failOnPlaceOrder: Boolean = false) :
    FakeApiService() {
    var lastPlacedOrderRequest: PlaceOrderRequest? = null

    override suspend fun getProducts(): List<com.group8.comp2300.data.remote.dto.ProductDto> = products.map { product ->
        com.group8.comp2300.data.remote.dto.ProductDto(
            id = product.id,
            name = product.name,
            description = product.description,
            price = product.price,
            category = product.category.name,
            insuranceCovered = product.insuranceCovered,
            imageUrl = product.imageUrl,
        )
    }

    override suspend fun getProduct(id: String): com.group8.comp2300.data.remote.dto.ProductDto =
        getProducts().first { it.id == id }

    override suspend fun placeOrder(request: PlaceOrderRequest): Order {
        lastPlacedOrderRequest = request
        if (failOnPlaceOrder) {
            throw IllegalStateException("checkout failed")
        }
        return Order(
            id = "order-1",
            userId = "user-1",
            items = request.items,
            status = OrderStatus.CONFIRMED,
            subtotal = request.items.sumOf { it.priceAtAdd * it.quantity },
            createdAt = 123L,
            updatedAt = 123L,
            shippingAddress = request.shippingAddress,
        )
    }
}

private fun signedInSession(userId: String): AuthSession = AuthSession.SignedIn(
    User(
        id = userId,
        firstName = "Test",
        lastName = "User",
        email = "$userId@example.com",
    ),
)

private fun sampleProduct(id: String = "product-1", price: Double = 24.5, insuranceCovered: Boolean = false) = Product(
    id = id,
    name = "PrEP Refill",
    description = "3-month supply",
    price = price,
    category = ProductCategory.MEDICATION,
    insuranceCovered = insuranceCovered,
)

private suspend fun kotlinx.coroutines.flow.Flow<com.group8.comp2300.domain.model.shop.Cart>.valueOrThrow() = first()
