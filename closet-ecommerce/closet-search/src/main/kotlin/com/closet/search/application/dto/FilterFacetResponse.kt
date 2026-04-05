package com.closet.search.application.dto

/**
 * 필터 검색 + facet 집계 응답 DTO (US-703).
 *
 * 검색 결과와 함께 카테고리/브랜드/사이즈/색상별 개수를 aggregation으로 반환한다.
 */
data class FilterFacetResponse(
    val products: List<ProductSearchResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
    val facets: FacetResult,
)

/**
 * facet 집계 결과.
 */
data class FacetResult(
    val categories: List<FacetBucket> = emptyList(),
    val brands: List<FacetBucket> = emptyList(),
    val sizes: List<FacetBucket> = emptyList(),
    val colors: List<FacetBucket> = emptyList(),
    val priceRanges: List<PriceRangeBucket> = emptyList(),
)

/**
 * facet 버킷 (키-카운트 쌍).
 */
data class FacetBucket(
    val key: String,
    val count: Long,
)

/**
 * 가격 범위 버킷.
 */
data class PriceRangeBucket(
    val from: Double?,
    val to: Double?,
    val count: Long,
)
