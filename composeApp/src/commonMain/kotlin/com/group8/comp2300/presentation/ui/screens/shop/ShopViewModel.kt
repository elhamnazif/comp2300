package com.group8.comp2300.presentation.ui.screens.shop

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.group8.comp2300.core.NetworkResult
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.AuthRepository
import com.group8.comp2300.domain.repository.ShopRepository
import com.group8.comp2300.domain.usecase.shop.GetProductsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val repository: ShopRepository, // Still needed for getProductById or other direct calls if not in usecases
    private val authRepository: AuthRepository
) : ViewModel() {

    // INPUT 1: Category Selection
    val selectedCategory: StateFlow<ProductCategory>
        field: MutableStateFlow<ProductCategory> = MutableStateFlow(ProductCategory.ALL)

    // INPUT 2: Cart Count (Local State)
    val cartItemCount: StateFlow<Int>
        field: MutableStateFlow<Int> = MutableStateFlow(0)

    // INPUT 3: Refresh Signal
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    // INTERNAL: The Data Loading Pipeline
    private val productsResultFlow = combine(
        selectedCategory,
        refreshTrigger.onStart { emit(Unit) }
    ) { category, _ ->
        category
    }.flatMapLatest { category ->
        getProductsUseCase(category)
    }

    // OUTPUT: Final State
    val state: StateFlow<State> = combine(
        productsResultFlow,
        selectedCategory,
        cartItemCount,
    ) { result, category, count ->
        when (result) {
            is NetworkResult.Loading -> State(
                isLoading = true,
                selectedCategory = category,
                cartItemCount = count,
            )

            is NetworkResult.Success -> State(
                isLoading = false,
                products = result.data,
                selectedCategory = category,
                cartItemCount = count,
            )

            is NetworkResult.Error -> State(
                isLoading = false,
                error = result.message,
                selectedCategory = category,
                cartItemCount = count,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = State(isLoading = true),
    )

    // --- Actions ---

    fun selectCategory(category: ProductCategory) {
        selectedCategory.value = category
    }

    fun addToCart(product: Product) {
        cartItemCount.update { it + 1 }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    suspend fun getProductById(id: String): Product? = try {
        repository.getProductById(id)
    } catch (e: Exception) {
        null
    }

    @Immutable
    data class State(
        val products: List<Product> = emptyList(),
        val selectedCategory: ProductCategory = ProductCategory.ALL,
        val cartItemCount: Int = 0,
        val isLoading: Boolean = false,
        val error: String? = null,
    )
}
