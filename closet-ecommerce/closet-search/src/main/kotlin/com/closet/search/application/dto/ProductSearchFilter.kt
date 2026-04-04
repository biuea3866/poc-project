package com.closet.search.application.dto

import java.math.BigDecimal

/**
 * 상품 검색 필터 DTO.
 */
data class ProductSearchFilter(
    val keyword: String? = null,
    val category: String? = null,
    val brand: String? = null,
    val minPrice: BigDecimal? = null,
    val maxPrice: BigDecimal? = null,
    val sizes: List<String>? = null,
    val colors: List<String>? = null,
    val gender: String? = null,
    val season: String? = null,
    val fitType: String? = null,
    val status: String? = "ACTIVE",
    val sort: String? = null,
)
