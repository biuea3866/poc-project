package com.closet.search.infrastructure.repository

import com.closet.search.application.dto.FilterFacetResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.domain.ProductDocument
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Elasticsearch 커스텀 검색 인터페이스.
 * ElasticsearchOperations를 사용한 복합 검색 쿼리를 제공한다.
 */
interface ProductSearchRepositoryCustom {

    /**
     * 키워드 + 필터 + 정렬 복합 검색 (US-701, US-702).
     */
    fun search(filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument>

    /**
     * 필터 검색 + facet 집계 (US-703).
     *
     * 검색 결과와 함께 카테고리/브랜드/사이즈/색상별 aggregation을 반환한다.
     */
    fun searchWithFacets(filter: ProductSearchFilter, pageable: Pageable): FilterFacetResponse

    /**
     * 자동완성 검색 (edge_ngram 분석기 활용, US-704).
     */
    fun autocomplete(keyword: String, size: Int): List<ProductDocument>
}
