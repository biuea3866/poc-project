package com.biuea.wiki.presentation.search

import com.biuea.wiki.domain.search.SearchService
import com.biuea.wiki.domain.search.VectorSearchService
import com.biuea.wiki.presentation.search.request.VectorSearchRequest
import com.biuea.wiki.presentation.search.response.SearchResultResponse
import com.biuea.wiki.presentation.search.response.VectorSearchResultResponse
import com.biuea.wiki.presentation.search.response.WebSearchResultResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchApiController(
    private val searchService: SearchService,
    private val vectorSearchService: VectorSearchService,
) {
    @GetMapping("/integrated")
    fun searchIntegrated(
        @RequestParam query: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<SearchResultResponse> {
        val result = searchService.searchIntegrated(query, page, size)
        return ResponseEntity.ok(SearchResultResponse.from(result))
    }

    @GetMapping("/web")
    fun searchWeb(
        @RequestParam q: String,
    ): ResponseEntity<WebSearchResultResponse> {
        val result = searchService.searchWeb(q)
        return ResponseEntity.ok(WebSearchResultResponse.from(result))
    }

    @PostMapping("/vector")
    fun searchVector(
        @RequestBody @Valid request: VectorSearchRequest,
    ): ResponseEntity<VectorSearchResultResponse> {
        val results = if (request.embedding != null) {
            vectorSearchService.searchByVector(request.embedding, request.limit)
        } else {
            vectorSearchService.searchByText(request.query ?: "", request.limit)
        }
        return ResponseEntity.ok(VectorSearchResultResponse.from(results))
    }
}
