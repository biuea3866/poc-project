package com.example.filepractice.domain

import java.time.LocalDateTime

/**
 * 주문 정보를 담는 데이터 클래스
 *
 * @property id 주문 고유 ID
 * @property orderAt 주문 시각
 * @property product 주문한 상품
 * @property coupon 적용된 쿠폰 (nullable)
 */
data class Order(
    val id: Long,
    val orderAt: LocalDateTime,
    val product: Product,
    val coupon: Coupon?
)
