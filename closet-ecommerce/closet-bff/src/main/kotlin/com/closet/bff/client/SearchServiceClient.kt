package com.closet.bff.client

import com.closet.bff.dto.AutocompleteBffResponse
import com.closet.bff.dto.PopularKeywordBffResponse
import com.closet.bff.dto.SearchPageBffResponse
import com.closet.bff.dto.SearchProductBffResponse
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

/**
 * Search Service Feign Client (CP-30).
 * Port: 8086 (PD-05)
 */
@FeignClient(name = "search-service", url = "\${service.search.url}")
interface SearchServiceClient {

    @GetMapping("/api/v1/search/products")
    fun searchProducts(
        @RequestHeader("X-Member-Id", required = false) memberId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) brand: String?,
        @RequestParam(required = false) minPrice: Long?,
        @RequestParam(required = false) maxPrice: Long?,
        @RequestParam(required = false) sizes: List<String>?,
        @RequestParam(required = false) colors: List<String>?,
        @RequestParam(required = false) gender: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): SearchPageBffResponse<SearchProductBffResponse>

    @GetMapping("/api/v1/search/products/autocomplete")
    fun autocomplete(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "10") size: Int,
    ): List<AutocompleteBffResponse>

    @GetMapping("/api/v1/search/keywords/popular")
    fun getPopularKeywords(@RequestParam(defaultValue = "10") size: Int): List<PopularKeywordBffResponse>

    @GetMapping("/api/v1/search/keywords/recent")
    fun getRecentKeywords(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<String>
}
