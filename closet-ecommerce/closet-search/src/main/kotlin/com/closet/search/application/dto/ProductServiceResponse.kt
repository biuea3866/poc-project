package com.closet.search.application.dto

import java.math.BigDecimal

/**
 * closet-product 서비스에서 반환하는 상품 응답 DTO.
 * 벌크 인덱싱 시 Feign/RestTemplate 호출 결과를 매핑한다.
 */
data class ProductServiceResponse(
    val id: Long,
    val name: String,
    val description: String,
    val brandId: Long,
    val brandName: String?,
    val categoryId: Long,
    val categoryName: String?,
    val basePrice: BigDecimal,
    val salePrice: BigDecimal,
    val discountRate: Int,
    val status: String,
    val season: String?,
    val fitType: String?,
    val gender: String?,
    val sizes: List<String>,
    val colors: List<String>,
    val imageUrl: String?,
)

/**
 * closet-product 서비스의 페이지 응답 DTO.
 */
data class ProductServicePageResponse(
    val content: List<ProductServiceResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
    val last: Boolean,
)
