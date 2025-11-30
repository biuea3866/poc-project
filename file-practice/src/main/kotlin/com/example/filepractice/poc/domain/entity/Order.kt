package com.example.filepractice.poc.domain.entity

import java.math.BigDecimal
import java.time.LocalDateTime

data class Order(
    val id: Int,
    val userId: Int,
    val userName: String,
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val totalPrice: BigDecimal,
    val status: OrderStatus,
    val orderDate: LocalDateTime,
    val shippingAddress: String?
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
