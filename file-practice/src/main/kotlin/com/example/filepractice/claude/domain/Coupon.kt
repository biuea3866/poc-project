package com.example.filepractice.claude.domain

import java.math.BigDecimal

/**
 * 쿠폰 도메인 모델
 *
 * @property id 쿠폰 ID
 * @property code 쿠폰 코드
 * @property name 쿠폰명
 * @property discountRate 할인율 (0.0 ~ 1.0)
 * @property discountAmount 할인 금액
 */
data class Coupon(
    val id: Long,
    val code: String,
    val name: String,
    val discountRate: BigDecimal,
    val discountAmount: BigDecimal
)
