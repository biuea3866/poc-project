package com.closet.bff.facade

import com.closet.bff.client.SearchServiceClient
import com.closet.bff.dto.AutocompleteBffResponse
import com.closet.bff.dto.PopularKeywordBffResponse
import com.closet.bff.dto.SearchPageBffResponse
import com.closet.bff.dto.SearchProductBffResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * 검색 BFF Facade (CP-30).
 *
 * search-service(8086)의 검색/자동완성/인기검색어/최근검색어를 통합한다.
 */
@Service
class SearchBffFacade(
    private val searchClient: SearchServiceClient,
) {
    data class SearchResultBffResponse(
        val products: SearchPageBffResponse<SearchProductBffResponse>?,
        val popularKeywords: List<PopularKeywordBffResponse>,
        val recentKeywords: List<String>,
    )

    /**
     * 통합 검색 (검색 결과 + 인기 검색어 + 최근 검색어).
     */
    fun search(
        memberId: Long?,
        keyword: String?,
        category: String?,
        brand: String?,
        sort: String?,
        page: Int,
        size: Int,
    ): SearchResultBffResponse {
        val products =
            runCatching {
                searchClient.searchProducts(
                    memberId = memberId,
                    keyword = keyword,
                    category = category,
                    brand = brand,
                    minPrice = null,
                    maxPrice = null,
                    sizes = null,
                    colors = null,
                    gender = null,
                    sort = sort,
                    page = page,
                    size = size,
                )
            }.getOrNull()

        val popularKeywords = runCatching { searchClient.getPopularKeywords(10) }.getOrDefault(emptyList())

        val recentKeywords =
            if (memberId != null) {
                runCatching { searchClient.getRecentKeywords(memberId, 10) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }

        return SearchResultBffResponse(
            products = products,
            popularKeywords = popularKeywords,
            recentKeywords = recentKeywords,
        )
    }

    /**
     * 자동완성 조회.
     */
    fun autocomplete(
        keyword: String,
        size: Int = 10,
    ): List<AutocompleteBffResponse> {
        return runCatching { searchClient.autocomplete(keyword, size) }.getOrDefault(emptyList())
    }
}
