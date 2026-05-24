package com.biuea.springai.domain

import org.springframework.stereotype.Repository

/**
 * 주문. PoC 라 영속화는 인메모리.
 *
 * trackingEvents: 결제 → 발송 → 배송중 → 배송완료 의 시간순 이력.
 * 취소·결제완료(미발송) 주문은 빈 리스트.
 */
data class Order(
    val orderId: String,
    val productId: String,
    val productName: String,
    val size: String,
    val quantity: Int,
    val status: String,
    val orderedAt: String,
    val trackingNo: String?,
    val trackingEvents: List<TrackingEvent> = emptyList(),
)

data class TrackingEvent(
    val timestamp: String,
    val status: String,
    val location: String,
)

@Repository
class OrderRepository {
    private val orders = mutableListOf<Order>()

    fun saveAll(items: List<Order>) {
        orders.clear()
        orders.addAll(items)
    }

    fun findAll(): List<Order> = orders.toList()

    fun findByStatus(status: String): List<Order> =
        orders.filter { it.status.equals(status, ignoreCase = true) }

    fun findById(orderId: String): Order? = orders.find { it.orderId.equals(orderId, ignoreCase = true) }

    /** 신규 등록 또는 갱신 (orderId 일치 시 덮어쓰기). */
    fun save(order: Order): Order {
        val idx = orders.indexOfFirst { it.orderId == order.orderId }
        if (idx >= 0) orders[idx] = order else orders += order
        return order
    }

    /** PoC 용 자동 발번: ORD-9001 부터 +1. */
    fun nextOrderId(): String {
        val maxNum = orders
            .mapNotNull { it.orderId.removePrefix("ORD-").toIntOrNull() }
            .maxOrNull() ?: 9000
        return "ORD-${maxOf(9000, maxNum) + 1}"
    }
}
