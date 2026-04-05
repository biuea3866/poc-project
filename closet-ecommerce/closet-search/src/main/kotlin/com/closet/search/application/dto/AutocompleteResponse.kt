package com.closet.search.application.dto

/**
 * 자동완성 검색 응답 DTO (US-704).
 *
 * 상품명, 브랜드명, 카테고리명을 대상으로 자동완성 후보를 제공한다.
 */
data class AutocompleteResponse(
    val productId: Long,
    val name: String,
    val brandName: String?,
    val categoryName: String?,
    val imageUrl: String?,
)
