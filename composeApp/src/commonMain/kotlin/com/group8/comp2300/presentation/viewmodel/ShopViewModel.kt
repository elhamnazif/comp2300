package com.group8.comp2300.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ShopUiState(
    val products: List<Product> = emptyList(),
    val selectedCategory: ProductCategory = ProductCategory.ALL,
    val cartItemCount: Int = 2
)

class ShopViewModel(
    private val repository: ShopRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShopUiState(products = repository.getAllProducts()))
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    fun selectCategory(category: ProductCategory) {
        _uiState.update { currentState ->
            val filteredProducts = repository.getProductsByCategory(category)
            currentState.copy(
                selectedCategory = category,
                products = filteredProducts
            )
        }
    }

    fun addToCart(product: Product) {
        _uiState.update { it.copy(cartItemCount = it.cartItemCount + 1) }
    }

    fun getProductById(id: String): Product? = repository.getProductById(id)
}
