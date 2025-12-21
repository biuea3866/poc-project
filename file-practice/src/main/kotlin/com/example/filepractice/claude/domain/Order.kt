package com.example.filepractice.claude.domain

import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * 주문 도메인 모델
 *
 * @property id 주문 ID
 * @property orderNumber 주문 번호
 * @property userId 사용자 ID
 * @property products 주문한 상품 목록
 * @property coupon 사용한 쿠폰 (nullable)
 * @property totalAmount 총 주문 금액
 * @property discountedAmount 할인 후 금액
 * @property orderDate 주문 일시
 * @property status 주문 상태
 */
data class Order(
    val id: Long,
    val orderNumber: String,
    val userId: Long,
    val products: List<Product>,
    val coupon: Coupon?,
    val totalAmount: BigDecimal,
    val discountedAmount: BigDecimal,
    val orderDate: LocalDateTime,
    val status: OrderStatus
) {
    /**
     * 주문 상태
     */
    enum class OrderStatus {
        PENDING,      // 대기 중
        CONFIRMED,    // 확정됨
        SHIPPED,      // 배송 중
        DELIVERED,    // 배송 완료
        CANCELLED     // 취소됨
    }
}
