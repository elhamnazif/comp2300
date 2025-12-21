package com.group8.comp2300.domain.usecase.shop

import com.group8.comp2300.core.NetworkResult
import com.group8.comp2300.domain.model.shop.Product
import com.group8.comp2300.domain.repository.ShopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GetProductUseCase(private val repository: ShopRepository) {
    operator fun invoke(id: String): Flow<NetworkResult<Product>> = flow {
        emit(NetworkResult.Loading)
        try {
            val product = repository.getProductById(id)
            emit(NetworkResult.Success(product))
        } catch (e: Exception) {
            emit(NetworkResult.Error(e, e.message ?: "Unknown error"))
        }
    }
}
