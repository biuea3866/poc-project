package com.biuea.wiki.presentation.search

import com.biuea.wiki.application.SearchDocumentFacade
import com.biuea.wiki.application.SearchDocumentInput
import com.biuea.wiki.presentation.document.response.SearchDocumentResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/search")
class SearchApiController(
    private val searchDocumentFacade: SearchDocumentFacade,
) {
    // GET /api/v1/search/integrated — 제목/내용 LIKE 검색 (MVP)
    @GetMapping("/integrated")
    fun search(
        @RequestParam q: String,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<SearchDocumentResponse> {
        return searchDocumentFacade.search(SearchDocumentInput(keyword = q, pageable = pageable))
            .map { SearchDocumentResponse.from(it) }
    }
}
