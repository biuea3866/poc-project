package com.closet.search.consumer.event

import java.math.BigDecimal

/**
 * event.closet.product 토픽의 통합 이벤트 엔벨로프.
 *
 * eventType 필드로 이벤트 유형을 구분한다.
 * - ProductCreated: ES 인덱싱
 * - ProductUpdated: ES 문서 갱신
 * - ProductDeleted: ES 문서 삭제
 */
data class ProductEvent(
    val eventType: String,
    val productId: Long,
    val name: String = "",
    val description: String = "",
    val brandId: Long = 0L,
    val categoryId: Long = 0L,
    val basePrice: BigDecimal = BigDecimal.ZERO,
    val salePrice: BigDecimal = BigDecimal.ZERO,
    val discountRate: Int = 0,
    val status: String = "",
    val season: String? = null,
    val fitType: String? = null,
    val gender: String? = null,
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val imageUrl: String? = null,
)
