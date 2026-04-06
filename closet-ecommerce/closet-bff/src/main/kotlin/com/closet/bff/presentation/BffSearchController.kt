package com.closet.bff.presentation

import com.closet.bff.facade.SearchBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 검색 BFF 컨트롤러 (CP-30).
 */
@RestController
@RequestMapping("/api/v1/bff")
class BffSearchController(
    private val searchFacade: SearchBffFacade,
) {
    /**
     * 통합 검색 (검색 결과 + 인기 검색어 + 최근 검색어).
     */
    @GetMapping("/search")
    fun search(
        @RequestHeader("X-Member-Id", required = false) memberId: Long?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) brand: String?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ) = ApiResponse.ok(searchFacade.search(memberId, keyword, category, brand, sort, page, size))

    /**
     * 자동완성.
     */
    @GetMapping("/search/autocomplete")
    fun autocomplete(
        @RequestParam keyword: String,
        @RequestParam(defaultValue = "10") size: Int,
    ) = ApiResponse.ok(searchFacade.autocomplete(keyword, size))
}
