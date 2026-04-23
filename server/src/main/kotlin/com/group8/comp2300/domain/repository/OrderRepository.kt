package com.group8.comp2300.domain.repository

import com.group8.comp2300.domain.model.shop.Order

interface OrderRepository {
    fun insert(order: Order)
    fun getByUserId(userId: String): List<Order>
}
