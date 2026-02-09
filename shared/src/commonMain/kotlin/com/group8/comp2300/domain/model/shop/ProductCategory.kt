package com.group8.comp2300.domain.model.shop

import kotlinx.serialization.Serializable

@Serializable
enum class ProductCategory(val displayName: String) {
    ALL("All"),
    TESTING("Testing"),
    MEDICATION("Medication"),
    PREVENTION("Prevention")
}
