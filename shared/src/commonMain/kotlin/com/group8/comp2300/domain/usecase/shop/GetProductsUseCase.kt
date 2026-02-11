package com.group8.comp2300.domain.usecase.shop

import com.group8.comp2300.core.NetworkResult
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.model.shop.ProductCategory
import com.group8.comp2300.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetProductsUseCase(private val repository: ShopRepository) {
    operator fun invoke(category: ProductCategory): Flow<NetworkResult<List<Product>>> = flow {
        emit(NetworkResult.Loading)
        try {
            val products = if (category == ProductCategory.ALL) {
                repository.getAllProducts()
            } else {
                repository.getProductsByCategory(category)
            }
            emit(NetworkResult.Success(products))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e, e.message ?: "Unknown error"))
        }
    }
}
