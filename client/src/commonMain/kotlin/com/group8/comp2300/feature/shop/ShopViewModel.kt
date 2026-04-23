package com.group8.comp2300.feature.shop

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.domain.model.shop.Cart
import com.group8.comp2300.domain.model.shop.CartLine
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ShopViewModel(
    private val repository: ShopRepository,
) : ViewModel() {
    private val allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val selectedCategory = MutableStateFlow(ProductCategory.ALL)
    private val isLoadingProducts = MutableStateFlow(true)
    private val productsError = MutableStateFlow<String?>(null)
    private val cartStore = MutableStateFlow(CartStore())
    private val checkoutStore = MutableStateFlow(CheckoutStore())
    private val browseStore = MutableStateFlow(BrowseStore())
    private var browseFeedbackClearJob: Job? = null
    private val browseChrome = combine(cartStore, browseStore) { cart, browse ->
        BrowseChrome(
            cartItemCount = cart.cart.itemCount,
            feedback = browse.feedback,
        )
    }

    val browseState: StateFlow<BrowseScreenState> = combine(
        allProducts,
        selectedCategory,
        isLoadingProducts,
        productsError,
        browseChrome,
    ) { products, category, loadingProducts, productError, chrome ->
        BrowseScreenState(
            products = if (category == ProductCategory.ALL) {
                products
            } else {
                products.filter { it.category == category }
            },
            selectedCategory = category,
            cartItemCount = chrome.cartItemCount,
            isLoadingProducts = loadingProducts,
            productsError = productError,
            browseFeedback = chrome.feedback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BrowseScreenState(isLoadingProducts = true),
    )

    val cartState: StateFlow<CartScreenState> = cartStore.map { cart ->
        CartScreenState(
            cartLines = cart.lines,
            cartItemCount = cart.cart.itemCount,
            cartSubtotal = cart.cart.subtotal,
            isLoadingCart = cart.isLoading,
            cartError = cart.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CartScreenState(isLoadingCart = true),
    )

    val checkoutState: StateFlow<CheckoutScreenState> = combine(
        cartStore,
        checkoutStore,
    ) { cart, checkout ->
        CheckoutScreenState(
            cartLines = cart.lines,
            cartSubtotal = cart.cart.subtotal,
            shippingAddress = checkout.shippingAddress,
            shippingAddressError = checkout.shippingAddressError,
            isPlacingOrder = checkout.isPlacingOrder,
            orderError = checkout.orderError,
            isLoadingCart = cart.isLoading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CheckoutScreenState(isLoadingCart = true),
    )

    init {
        loadProducts()
        observeCart()
    }

    fun selectCategory(category: ProductCategory) {
        selectedCategory.value = category
    }

    fun addToCart(product: Product) {
        mutateAddToCart(product) { message ->
            cartStore.update { it.copy(error = message) }
        }
    }

    fun addToCartFromBrowse(product: Product) {
        mutateAddToCart(
            product = product,
            onSuccess = {
                showBrowseFeedback(
                    BrowseFeedback(
                        productId = product.id,
                        message = "Added to cart",
                    ),
                    autoDismiss = true,
                )
            },
            onFailure = { message ->
                showBrowseFeedback(
                    BrowseFeedback(
                        productId = product.id,
                        message = message,
                        isError = true,
                    ),
                    autoDismiss = false,
                )
            },
        )
    }

    fun dismissBrowseFeedback() {
        browseFeedbackClearJob?.cancel()
        browseStore.value = BrowseStore()
    }

    fun dismissCartError() {
        cartStore.update { it.copy(error = null) }
    }

    private fun mutateAddToCart(
        product: Product,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching { repository.addToCart(product) }
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    onFailure(error.userMessage("Couldn't add item right now"))
                }
        }
    }

    fun incrementCartItem(productId: String) {
        val line = cartStore.value.lines.firstOrNull { it.productId == productId } ?: return
        updateCartQuantity(productId, line.quantity + 1)
    }

    fun decrementCartItem(productId: String) {
        val line = cartStore.value.lines.firstOrNull { it.productId == productId } ?: return
        updateCartQuantity(productId, line.quantity - 1)
    }

    fun removeCartItem(productId: String) {
        viewModelScope.launch {
            runCatching { repository.removeFromCart(productId) }
                .onFailure { error ->
                    cartStore.update { it.copy(error = error.userMessage("Couldn't update cart right now")) }
                }
        }
    }

    fun refreshProducts() {
        loadProducts()
    }

    fun updateShippingAddress(value: String) {
        checkoutStore.update {
            it.copy(
                shippingAddress = value,
                shippingAddressError = null,
                orderError = null,
            )
        }
    }

    fun placeOrder(onSuccess: (Order) -> Unit) {
        val currentAddress = checkoutStore.value.shippingAddress.trim()
        if (currentAddress.isEmpty()) {
            checkoutStore.update { it.copy(shippingAddressError = "Enter a shipping address") }
            return
        }
        if (cartStore.value.lines.any { it.product == null }) {
            checkoutStore.update { it.copy(orderError = "Remove unavailable items to continue") }
            return
        }

        viewModelScope.launch {
            checkoutStore.update { it.copy(isPlacingOrder = true, orderError = null, shippingAddressError = null) }
            runCatching { repository.placeOrder(currentAddress) }
                .onSuccess { order ->
                    checkoutStore.value = CheckoutStore()
                    onSuccess(order)
                }
                .onFailure { error ->
                    checkoutStore.update {
                        it.copy(
                            isPlacingOrder = false,
                            orderError = error.userMessage("Couldn't place order right now"),
                        )
                    }
                }
        }
    }

    suspend fun getProductById(id: String): Product? = runCatching { repository.getProductById(id) }.getOrNull()

    suspend fun getOrders(): List<Order> = repository.getOrders()

    private fun loadProducts() {
        viewModelScope.launch {
            isLoadingProducts.value = true
            productsError.value = null
            runCatching { repository.getAllProducts() }
                .onSuccess { products ->
                    allProducts.value = products
                    isLoadingProducts.value = false
                }
                .onFailure { error ->
                    isLoadingProducts.value = false
                    productsError.value = error.userMessage("Couldn't load products")
                }
        }
    }

    private fun observeCart() {
        viewModelScope.launch {
            repository.observeCartDetails().collectLatest { details ->
                cartStore.value = CartStore(
                    cart = details.cart,
                    lines = details.lines,
                    isLoading = false,
                    error = if (details.lines.any { it.product == null }) "Some items are unavailable" else null,
                )
            }
        }
    }

    private fun updateCartQuantity(productId: String, quantity: Int) {
        viewModelScope.launch {
            runCatching { repository.updateCartQuantity(productId, quantity) }
                .onFailure { error ->
                    cartStore.update { it.copy(error = error.userMessage("Couldn't update cart right now")) }
                }
        }
    }

    private fun showBrowseFeedback(feedback: BrowseFeedback, autoDismiss: Boolean) {
        browseFeedbackClearJob?.cancel()
        browseStore.value = BrowseStore(feedback = feedback)
        if (autoDismiss) {
            browseFeedbackClearJob = viewModelScope.launch {
                delay(2_500)
                if (browseStore.value.feedback == feedback) {
                    browseStore.value = BrowseStore()
                }
            }
        }
    }
}

@Immutable
data class BrowseScreenState(
    val products: List<Product> = emptyList(),
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val cartItemCount: Int = 0,
    val isLoadingProducts: Boolean = false,
    val productsError: String? = null,
    val browseFeedback: BrowseFeedback? = null,
)

@Immutable
data class CartScreenState(
    val cartLines: List<CartLine> = emptyList(),
    val cartItemCount: Int = 0,
    val cartSubtotal: Double = 0.0,
    val isLoadingCart: Boolean = false,
    val cartError: String? = null,
) {
    val hasUnavailableItems: Boolean
        get() = cartLines.any { it.product == null }
}

@Immutable
data class CheckoutScreenState(
    val cartLines: List<CartLine> = emptyList(),
    val cartSubtotal: Double = 0.0,
    val shippingAddress: String = "",
    val shippingAddressError: String? = null,
    val isPlacingOrder: Boolean = false,
    val orderError: String? = null,
    val isLoadingCart: Boolean = false,
) {
    val hasUnavailableItems: Boolean
        get() = cartLines.any { it.product == null }
}

private data class CartStore(
    val cart: Cart = Cart(userId = "", items = emptyList()),
    val lines: List<CartLine> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

private data class CheckoutStore(
    val shippingAddress: String = "",
    val shippingAddressError: String? = null,
    val isPlacingOrder: Boolean = false,
    val orderError: String? = null,
)

private data class BrowseStore(
    val feedback: BrowseFeedback? = null,
)

private data class BrowseChrome(
    val cartItemCount: Int,
    val feedback: BrowseFeedback?,
)

@Immutable
data class BrowseFeedback(
    val productId: String? = null,
    val message: String,
    val isError: Boolean = false,
)

private fun Throwable.userMessage(fallback: String): String = fallback
