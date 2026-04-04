package com.closet.search.infrastructure.repository

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
     * 키워드 + 필터 + 정렬 복합 검색.
     */
    fun search(filter: ProductSearchFilter, pageable: Pageable): Page<ProductDocument>

    /**
     * 자동완성 검색 (edge_ngram 분석기 활용).
     */
    fun autocomplete(keyword: String, size: Int): List<ProductDocument>
}
