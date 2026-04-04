package com.closet.search.application.dto

/**
 * 자동완성 검색 응답 DTO.
 */
data class AutocompleteResponse(
    val productId: Long,
    val name: String,
    val brandName: String?,
    val imageUrl: String?,
)
