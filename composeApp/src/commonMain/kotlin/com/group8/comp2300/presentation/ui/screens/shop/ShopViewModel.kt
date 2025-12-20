package com.group8.comp2300.presentation.ui.screens.shop

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class ShopViewModel(private val repository: ShopRepository) : ViewModel() {
    val state: StateFlow<State>
        field = MutableStateFlow(State(products = repository.getAllProducts()))

    fun selectCategory(category: ProductCategory) {
        state.update { currentState ->
            val filteredProducts = repository.getProductsByCategory(category)
            currentState.copy(selectedCategory = category, products = filteredProducts)
        }
    }

    fun addToCart(product: Product) {
        state.update { it.copy(cartItemCount = it.cartItemCount + 1) }
    }

    fun getProductById(id: String): Product? = repository.getProductById(id)

    @Immutable
    data class State(
        val products: List<Product> = emptyList(),
        val selectedCategory: ProductCategory = ProductCategory.ALL,
        val cartItemCount: Int = 2,
    )
}
