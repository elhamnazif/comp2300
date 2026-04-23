package com.group8.comp2300.data.repository

import com.group8.comp2300.database.ServerDatabase
import com.group8.comp2300.domain.model.shop.CartItem
import com.group8.comp2300.domain.model.shop.Order
import com.group8.comp2300.domain.model.shop.OrderStatus
import com.group8.comp2300.domain.repository.OrderRepository

class OrderRepositoryImpl(private val database: ServerDatabase) : OrderRepository {
    override fun insert(order: Order) {
        database.transaction {
            database.orderQueries.insertShopOrder(
                id = order.id,
                user_id = order.userId,
                status = order.status.name,
                subtotal = order.subtotal,
                tax = order.tax,
                shipping = order.shipping,
                created_at = order.createdAt,
                updated_at = order.updatedAt,
                shipping_address = order.shippingAddress,
            )
            order.items.forEach { item ->
                database.orderQueries.insertShopOrderItem(
                    order_id = order.id,
                    product_id = item.productId,
                    quantity = item.quantity.toLong(),
                    price_at_add = item.priceAtAdd,
                )
            }
        }
    }

    override fun getByUserId(userId: String): List<Order> =
        database.orderQueries.selectShopOrdersByUserId(userId).executeAsList().map { row ->
            Order(
                id = row.id,
                userId = row.user_id,
                items = database.orderQueries.selectShopOrderItemsByOrderId(row.id).executeAsList().map { item ->
                    CartItem(
                        productId = item.product_id,
                        quantity = item.quantity.toInt(),
                        priceAtAdd = item.price_at_add,
                    )
                },
                status = OrderStatus.valueOf(row.status),
                subtotal = row.subtotal,
                tax = row.tax,
                shipping = row.shipping,
                createdAt = row.created_at,
                updatedAt = row.updated_at,
                shippingAddress = row.shipping_address,
            )
        }
}
