package com.biuea.wiki.domain.search

import com.biuea.wiki.infrastructure.document.DocumentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchService(
    private val documentRepository: DocumentRepository,
) {
    @Transactional(readOnly = true)
    fun searchIntegrated(keyword: String, page: Int, size: Int): IntegratedSearchResult {
        val pageResult = documentRepository.searchByKeyword(keyword, PageRequest.of(page, size))
        return IntegratedSearchResult(
            results = pageResult.content.map {
                IntegratedSearchResult.Item(
                    id = it.id,
                    title = it.title,
                    snippet = it.content?.take(200),
                )
            },
            page = pageResult.number,
            size = pageResult.size,
            totalElements = pageResult.totalElements,
            totalPages = pageResult.totalPages,
        )
    }

    fun searchWeb(keyword: String): List<WebSearchResult> {
        return listOf(
            WebSearchResult(
                title = "Web search for: $keyword",
                snippet = "외부 웹 검색 API 연동 예정 (MVP에서는 Mock 응답)",
                url = "https://example.com/search?q=$keyword",
            )
        )
    }
}

data class IntegratedSearchResult(
    val results: List<Item>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    data class Item(
        val id: Long,
        val title: String,
        val snippet: String?,
    )
}

data class WebSearchResult(
    val title: String,
    val snippet: String,
    val url: String,
)
