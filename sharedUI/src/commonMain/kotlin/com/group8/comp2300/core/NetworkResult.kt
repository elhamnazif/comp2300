package com.group8.comp2300.core

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val exception: Throwable, val message: String? = null) : NetworkResult<Nothing>
    data object Loading : NetworkResult<Nothing>
}
