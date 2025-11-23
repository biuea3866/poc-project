package com.example.filepractice.domain

import java.math.BigDecimal

/**
 * 쿠폰 정보를 담는 데이터 클래스
 *
 * @property id 쿠폰 고유 ID
 * @property name 쿠폰명
 * @property discountRate 할인율
 */
data class Coupon(
    val id: Long,
    val name: String,
    val discountRate: BigDecimal
)
