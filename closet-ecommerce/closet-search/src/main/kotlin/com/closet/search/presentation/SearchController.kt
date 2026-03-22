package com.closet.search.presentation

import com.closet.common.response.ApiResponse
import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.service.ProductSearchService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchController(
    private val productSearchService: ProductSearchService,
) {

    @GetMapping("/products")
    fun searchProducts(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) gender: String?,
        @RequestParam(required = false) season: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<Page<ProductSearchResponse>> {
        val filter = ProductSearchFilter(
            categoryId = categoryId,
            brandId = brandId,
            minPrice = minPrice,
            maxPrice = maxPrice,
            gender = gender,
            season = season,
            sort = sort,
        )
        val pageable = PageRequest.of(page, size)
        return ApiResponse.ok(productSearchService.search(keyword, filter, pageable))
    }

    @GetMapping("/autocomplete")
    fun autocomplete(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "5") limit: Int,
    ): ApiResponse<AutocompleteResponse> {
        return ApiResponse.ok(productSearchService.autocomplete(query, limit))
    }

    @PostMapping("/index")
    fun reindex(): ApiResponse<IndexSyncResponse> {
        return ApiResponse.ok(productSearchService.syncFromProductService())
    }

    @PostMapping("/index/sync")
    fun syncFromProductService(): ApiResponse<IndexSyncResponse> {
        return ApiResponse.ok(productSearchService.syncFromProductService())
    }
}
