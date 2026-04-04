package com.closet.search.presentation

import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.service.PopularKeywordResponse
import com.closet.search.application.service.PopularKeywordService
import com.closet.search.application.service.ProductSearchService
import com.closet.search.application.service.RecentKeywordService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * 상품 검색 API 컨트롤러.
 *
 * GET /api/v1/search/products                - 키워드 + 필터 + 정렬 검색
 * GET /api/v1/search/products/autocomplete   - 자동완성 검색 (CP-20)
 * GET /api/v1/search/keywords/popular        - 인기 검색어 (CP-21)
 * GET /api/v1/search/keywords/recent         - 최근 검색어 (CP-22)
 * DELETE /api/v1/search/keywords/recent      - 최근 검색어 삭제 (CP-22)
 */
@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val productSearchService: ProductSearchService,
    private val popularKeywordService: PopularKeywordService,
    private val recentKeywordService: RecentKeywordService,
) {

    /**
     * 상품 검색 API.
     *
     * nori 한글 분석기 기반 full-text 검색 + 다양한 필터 + 정렬을 지원한다.
     * 검색 시 인기 검색어 기록 + 최근 검색어 저장을 함께 수행한다.
     */
    @GetMapping("/products")
    fun searchProducts(
        @RequestHeader("X-Member-Id", required = false) memberId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) brand: String?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) sizes: List<String>?,
        @RequestParam(required = false) colors: List<String>?,
        @RequestParam(required = false) gender: String?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) fitType: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<SearchPageResponse<ProductSearchResponse>> {
        val filter = ProductSearchFilter(
            keyword = keyword,
            category = category,
            brand = brand,
            minPrice = minPrice,
            maxPrice = maxPrice,
            sizes = sizes,
            colors = colors,
            gender = gender,
            season = season,
            fitType = fitType,
            sort = sort,
        )

        val result: Page<ProductSearchResponse> = productSearchService.search(filter, PageRequest.of(page, size))

        // 인기 검색어 기록 + 최근 검색어 저장 (CP-21, CP-22)
        if (!keyword.isNullOrBlank()) {
            popularKeywordService.recordKeyword(keyword)
            if (memberId != null) {
                recentKeywordService.saveRecentKeyword(memberId, keyword)
            }
        }

        return ResponseEntity.ok(
            SearchPageResponse(
                content = result.content,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
                page = result.number,
                size = result.size,
            )
        )
    }

    /**
     * 자동완성 검색 API (CP-20).
     *
     * edge_ngram 분석기를 활용하여 입력 중인 키워드에 대한 자동완성 후보를 반환한다.
     */
    @GetMapping("/products/autocomplete")
    fun autocomplete(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<List<AutocompleteResponse>> {
        val results = productSearchService.autocomplete(keyword, size)
        return ResponseEntity.ok(results)
    }

    /**
     * 인기 검색어 조회 API (CP-21).
     *
     * Redis Sorted Set 기반 실시간 sliding window (PD-25).
     */
    @GetMapping("/keywords/popular")
    fun getPopularKeywords(
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<List<PopularKeywordResponse>> {
        val results = popularKeywordService.getPopularKeywords(size)
        return ResponseEntity.ok(results)
    }

    /**
     * 최근 검색어 조회 API (CP-22).
     *
     * Redis List 기반 멤버별 최근 검색어 (PD-48).
     */
    @GetMapping("/keywords/recent")
    fun getRecentKeywords(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<List<String>> {
        val results = recentKeywordService.getRecentKeywords(memberId, size)
        return ResponseEntity.ok(results)
    }

    /**
     * 최근 검색어 삭제 API (CP-22).
     *
     * keyword 파라미터가 있으면 해당 검색어만 삭제, 없으면 전체 삭제.
     */
    @DeleteMapping("/keywords/recent")
    fun deleteRecentKeywords(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(required = false) keyword: String?,
    ): ResponseEntity<Void> {
        if (keyword != null) {
            recentKeywordService.deleteRecentKeyword(memberId, keyword)
        } else {
            recentKeywordService.deleteAllRecentKeywords(memberId)
        }
        return ResponseEntity.noContent().build()
    }

    /**
     * 검색 페이지 응답 DTO.
     */
    data class SearchPageResponse<T>(
        val content: List<T>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
