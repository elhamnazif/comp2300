package com.group8.comp2300.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val price: Double,
    val category: String, // String category in API
    val insuranceCovered: Boolean = false,
    val imageUrl: String? = null,
)
