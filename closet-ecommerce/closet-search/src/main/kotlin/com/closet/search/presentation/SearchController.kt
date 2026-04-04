package com.closet.search.presentation

import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.service.ProductSearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

/**
 * 상품 검색 API 컨트롤러.
 *
 * GET /api/v1/search/products       - 키워드 + 필터 + 정렬 검색
 * GET /api/v1/search/products/autocomplete - 자동완성 검색
 */
@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val productSearchService: ProductSearchService,
) {

    /**
     * 상품 검색 API.
     *
     * nori 한글 분석기 기반 full-text 검색 + 다양한 필터 + 정렬을 지원한다.
     */
    @GetMapping("/products")
    fun searchProducts(
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
     * 자동완성 검색 API.
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
