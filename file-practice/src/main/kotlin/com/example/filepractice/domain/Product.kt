package com.example.filepractice.domain

import java.math.BigDecimal

/**
 * 상품 정보를 담는 데이터 클래스
 *
 * @property id 상품 고유 ID
 * @property name 상품명
 * @property price 상품 가격
 */
data class Product(
    val id: Long,
    val name: String,
    val price: BigDecimal
)
