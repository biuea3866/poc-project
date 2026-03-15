package com.biuea.wiki.presentation.search.response

import com.biuea.wiki.domain.search.SearchResponse

data class SemanticSearchResponse(
    val items: List<SemanticSearchItem>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
) {
    data class SemanticSearchItem(
        val documentId: Long,
        val title: String,
        val snippet: String,
        val similarity: Double,
        val tags: List<String>,
    )

    companion object {
        fun from(result: SearchResponse): SemanticSearchResponse {
            return SemanticSearchResponse(
                items = result.items.map {
                    SemanticSearchItem(
                        documentId = it.documentId,
                        title = it.title,
                        snippet = it.snippet,
                        similarity = it.similarity,
                        tags = it.tags,
                    )
                },
                page = result.page,
                size = result.size,
                totalElements = result.totalElements,
            )
        }
    }
}
