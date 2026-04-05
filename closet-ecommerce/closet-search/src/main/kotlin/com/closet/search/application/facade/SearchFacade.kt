package com.closet.search.application.facade

import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.FilterFacetResponse
import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.service.PopularKeywordService
import com.closet.search.application.service.ProductSearchService
import com.closet.search.application.service.RecentKeywordService
import mu.KotlinLogging
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

/**
 * 검색 파사드.
 *
 * Controller/Consumer와 도메인 서비스 사이를 중재한다.
 * 여러 서비스를 조합하는 오케스트레이션 로직을 담당하며,
 * 각 서비스에 비즈니스 로직을 위임한다.
 */
@Component
class SearchFacade(
    private val productSearchService: ProductSearchService,
    private val popularKeywordService: PopularKeywordService,
    private val recentKeywordService: RecentKeywordService,
) {

    // ──────────────────────────── 검색 API ────────────────────────────

    /**
     * 키워드 + 필터 + 정렬 상품 검색 (US-701, US-702).
     *
     * 검색 수행 후 인기 검색어 기록 + 최근 검색어 저장을 함께 수행한다.
     */
    fun searchProducts(
        filter: ProductSearchFilter,
        pageable: Pageable,
        memberId: Long?,
    ): Page<ProductSearchResponse> {
        val result = productSearchService.search(filter, pageable)

        // 인기 검색어 기록 + 최근 검색어 저장
        if (!filter.keyword.isNullOrBlank()) {
            popularKeywordService.recordKeyword(filter.keyword)
            if (memberId != null) {
                recentKeywordService.saveRecentKeyword(memberId, filter.keyword)
            }
        }

        return result
    }

    /**
     * 필터 검색 + facet 집계 (US-703).
     */
    fun searchWithFacets(
        filter: ProductSearchFilter,
        pageable: Pageable,
    ): FilterFacetResponse {
        return productSearchService.searchWithFacets(filter, pageable)
    }

    /**
     * 자동완성 검색 (US-704).
     */
    fun autocomplete(keyword: String, size: Int): List<AutocompleteResponse> {
        return productSearchService.autocomplete(keyword, size)
    }

    // ──────────────────────────── 이벤트 처리 ────────────────────────────

    /**
     * 상품 생성 이벤트 처리 → ES 인덱싱.
     */
    fun handleProductCreated(
        productId: Long,
        name: String,
        description: String,
        brandId: Long,
        categoryId: Long,
        basePrice: BigDecimal,
        salePrice: BigDecimal,
        discountRate: Int,
        status: String,
        season: String?,
        fitType: String?,
        gender: String?,
        sizes: List<String>,
        colors: List<String>,
        imageUrl: String?,
    ) {
        productSearchService.indexProduct(
            productId = productId,
            name = name,
            description = description,
            brandId = brandId,
            categoryId = categoryId,
            basePrice = basePrice,
            salePrice = salePrice,
            discountRate = discountRate,
            status = status,
            season = season,
            fitType = fitType,
            gender = gender,
            sizes = sizes,
            colors = colors,
            imageUrl = imageUrl,
        )
    }

    /**
     * 상품 수정 이벤트 처리 → ES 문서 갱신.
     */
    fun handleProductUpdated(
        productId: Long,
        name: String,
        description: String,
        brandId: Long,
        categoryId: Long,
        basePrice: BigDecimal,
        salePrice: BigDecimal,
        discountRate: Int,
        status: String,
        season: String?,
        fitType: String?,
        gender: String?,
        sizes: List<String>,
        colors: List<String>,
        imageUrl: String?,
    ) {
        productSearchService.updateProduct(
            productId = productId,
            name = name,
            description = description,
            brandId = brandId,
            categoryId = categoryId,
            basePrice = basePrice,
            salePrice = salePrice,
            discountRate = discountRate,
            status = status,
            season = season,
            fitType = fitType,
            gender = gender,
            sizes = sizes,
            colors = colors,
            imageUrl = imageUrl,
        )
    }

    /**
     * 상품 삭제 이벤트 처리 → ES 문서 삭제.
     */
    fun handleProductDeleted(productId: Long) {
        productSearchService.deleteProduct(productId)
    }

    /**
     * 리뷰 집계 업데이트 이벤트 처리 → ES 문서 부분 갱신.
     */
    fun handleReviewSummaryUpdated(productId: Long, reviewCount: Int, avgRating: Double) {
        productSearchService.updateReviewSummary(productId, reviewCount, avgRating)
    }

    // ──────────────────────────── 어드민 ────────────────────────────

    /**
     * 벌크 리인덱싱 (US-708).
     */
    fun bulkReindex(): IndexSyncResponse {
        return productSearchService.bulkReindex()
    }
}
