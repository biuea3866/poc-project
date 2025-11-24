package com.example.cachepractice.domain

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders", indexes = [
    Index(name = "idx_customer_id", columnList = "customerId"),
    Index(name = "idx_order_date", columnList = "orderDate")
])
data class Order(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val customerId: Long,

    @Column(nullable = false)
    val customerName: String,

    @Column(nullable = false)
    val totalAmount: BigDecimal,

    @Column(nullable = false)
    val orderDate: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: OrderStatus = OrderStatus.PENDING,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val items: MutableList<OrderItem> = mutableListOf()
) {
    fun addItem(item: OrderItem) {
        items.add(item)
    }
}

enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED
}
