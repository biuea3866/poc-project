package com.biuea.wiki.presentation.search.response

import com.biuea.wiki.domain.search.IntegratedSearchResult
import com.biuea.wiki.domain.search.WebSearchResult

data class SearchResultResponse(
    val results: List<SearchResultItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    data class SearchResultItem(
        val id: Long,
        val title: String,
        val snippet: String?,
    )

    companion object {
        fun from(result: IntegratedSearchResult): SearchResultResponse {
            return SearchResultResponse(
                results = result.results.map {
                    SearchResultItem(id = it.id, title = it.title, snippet = it.snippet)
                },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
        }
    }
}

data class WebSearchResultResponse(
    val results: List<WebSearchResultItem>,
) {
    data class WebSearchResultItem(
        val title: String,
        val snippet: String,
        val url: String,
    )

    companion object {
        fun from(results: List<WebSearchResult>): WebSearchResultResponse {
            return WebSearchResultResponse(
                results = results.map {
                    WebSearchResultItem(title = it.title, snippet = it.snippet, url = it.url)
                }
            )
        }
    }
}
