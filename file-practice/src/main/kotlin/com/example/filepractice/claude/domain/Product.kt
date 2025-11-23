package com.example.filepractice.claude.domain

import java.math.BigDecimal

/**
 * 상품 도메인 모델
 *
 * @property id 상품 ID
 * @property name 상품명
 * @property price 상품 가격
 * @property quantity 주문 수량
 * @property category 상품 카테고리
 */
data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal,
    val quantity: Int,
    val category: String
)
