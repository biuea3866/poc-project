package com.biuea.wiki.presentation.search.response

import com.biuea.wiki.domain.search.VectorSearchResult

data class VectorSearchResultResponse(
    val results: List<VectorSearchResultItem>,
) {
    data class VectorSearchResultItem(
        val id: Long,
        val documentId: Long,
        val documentRevisionId: Long,
        val chunkContent: String,
        val tokenCount: Int,
        val similarity: Double,
    )

    companion object {
        fun from(results: List<VectorSearchResult>): VectorSearchResultResponse {
            return VectorSearchResultResponse(
                results = results.map {
                    VectorSearchResultItem(
                        id = it.id,
                        documentId = it.documentId,
                        documentRevisionId = it.documentRevisionId,
                        chunkContent = it.chunkContent,
                        tokenCount = it.tokenCount,
                        similarity = it.similarity,
                    )
                }
            )
        }
    }
}
