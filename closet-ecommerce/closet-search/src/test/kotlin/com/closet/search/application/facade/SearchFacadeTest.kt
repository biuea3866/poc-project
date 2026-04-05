package com.closet.search.application.facade

import com.closet.search.application.dto.AutocompleteResponse
import com.closet.search.application.dto.FacetResult
import com.closet.search.application.dto.FilterFacetResponse
import com.closet.search.application.dto.IndexSyncResponse
import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.service.PopularKeywordService
import com.closet.search.application.service.ProductSearchService
import com.closet.search.application.service.RecentKeywordService
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal

class SearchFacadeTest : BehaviorSpec({

    val productSearchService = mockk<ProductSearchService>(relaxed = true)
    val popularKeywordService = mockk<PopularKeywordService>(relaxed = true)
    val recentKeywordService = mockk<RecentKeywordService>(relaxed = true)

    val facade = SearchFacade(
        productSearchService = productSearchService,
        popularKeywordService = popularKeywordService,
        recentKeywordService = recentKeywordService,
    )

    Given("검색 API 호출 시 (US-701, US-702)") {

        val filter = ProductSearchFilter(keyword = "맨투맨")
        val pageable = PageRequest.of(0, 20)
        val response = ProductSearchResponse(
            productId = 1L,
            name = "오버핏 맨투맨",
            brandName = "무신사 스탠다드",
            categoryName = "상의",
            basePrice = BigDecimal("39000"),
            salePrice = BigDecimal("29000"),
            discountRate = 25,
            sizes = listOf("M", "L"),
            colors = listOf("블랙"),
            fitType = "OVERFIT",
            gender = "UNISEX",
            season = "FW",
            imageUrl = null,
            reviewCount = 10,
            avgRating = 4.5,
            popularityScore = 50.0,
            highlights = mapOf("name" to listOf("<em>맨투맨</em>")),
        )
        val page = PageImpl(listOf(response), pageable, 1)
        every { productSearchService.search(filter, pageable) } returns page

        When("로그인한 회원이 검색하면") {
            val result = facade.searchProducts(filter, pageable, memberId = 1L)

            Then("검색 결과가 반환되고 인기/최근 검색어가 기록된다") {
                result.content shouldHaveSize 1
                result.content[0].productId shouldBe 1L

                verify(exactly = 1) { popularKeywordService.recordKeyword("맨투맨") }
                verify(exactly = 1) { recentKeywordService.saveRecentKeyword(1L, "맨투맨") }
            }
        }

        When("비로그인 사용자가 검색하면") {
            val result = facade.searchProducts(filter, pageable, memberId = null)

            Then("검색 결과가 반환되고 인기 검색어만 기록된다 (최근 검색어 미기록)") {
                result.content shouldHaveSize 1
                verify(atLeast = 1) { popularKeywordService.recordKeyword("맨투맨") }
                verify(exactly = 0) { recentKeywordService.saveRecentKeyword(0L, any()) }
            }
        }
    }

    Given("필터 + facet 검색 시 (US-703)") {

        val filter = ProductSearchFilter(category = "상의", brand = "무신사 스탠다드")
        val pageable = PageRequest.of(0, 20)

        every { productSearchService.searchWithFacets(filter, pageable) } returns FilterFacetResponse(
            products = emptyList(),
            totalElements = 0,
            totalPages = 0,
            page = 0,
            size = 20,
            facets = FacetResult(),
        )

        When("facet 검색을 호출하면") {
            val result = facade.searchWithFacets(filter, pageable)

            Then("FilterFacetResponse가 반환된다") {
                result.size shouldBe 20
                result.page shouldBe 0
            }
        }
    }

    Given("자동완성 검색 시 (US-704)") {

        every { productSearchService.autocomplete("오버", 10) } returns listOf(
            AutocompleteResponse(1L, "오버핏 맨투맨", "무신사 스탠다드", "상의", null),
            AutocompleteResponse(2L, "오버핏 후드티", "무신사 스탠다드", "상의", null),
        )

        When("2자 이상 키워드로 자동완성 검색하면") {
            val result = facade.autocomplete("오버", 10)

            Then("자동완성 후보가 반환된다") {
                result shouldHaveSize 2
                result[0].name shouldBe "오버핏 맨투맨"
            }
        }
    }

    Given("상품 이벤트 처리 시") {

        When("ProductCreated 이벤트를 처리하면") {
            facade.handleProductCreated(
                productId = 1L,
                name = "테스트 상품",
                description = "설명",
                brandId = 1L,
                categoryId = 1L,
                basePrice = BigDecimal("10000"),
                salePrice = BigDecimal("8000"),
                discountRate = 20,
                status = "ACTIVE",
                season = null,
                fitType = null,
                gender = null,
                sizes = listOf("M"),
                colors = listOf("블랙"),
                imageUrl = null,
            )

            Then("ProductSearchService.indexProduct이 호출된다") {
                verify(exactly = 1) {
                    productSearchService.indexProduct(
                        productId = 1L,
                        name = "테스트 상품",
                        description = "설명",
                        brandId = 1L,
                        categoryId = 1L,
                        basePrice = BigDecimal("10000"),
                        salePrice = BigDecimal("8000"),
                        discountRate = 20,
                        status = "ACTIVE",
                        season = null,
                        fitType = null,
                        gender = null,
                        sizes = listOf("M"),
                        colors = listOf("블랙"),
                        imageUrl = null,
                    )
                }
            }
        }

        When("ProductDeleted 이벤트를 처리하면") {
            facade.handleProductDeleted(productId = 99L)

            Then("ProductSearchService.deleteProduct이 호출된다") {
                verify(exactly = 1) { productSearchService.deleteProduct(99L) }
            }
        }
    }

    Given("리뷰 집계 이벤트 처리 시") {

        When("ReviewSummaryUpdated 이벤트를 처리하면") {
            facade.handleReviewSummaryUpdated(productId = 5L, reviewCount = 100, avgRating = 4.8)

            Then("ProductSearchService.updateReviewSummary가 호출된다") {
                verify(exactly = 1) {
                    productSearchService.updateReviewSummary(5L, 100, 4.8)
                }
            }
        }
    }

    Given("벌크 리인덱싱 시 (US-708)") {

        every { productSearchService.bulkReindex() } returns IndexSyncResponse(
            totalRequested = 1000,
            totalIndexed = 995,
            totalFailed = 5,
            elapsedMillis = 3000,
        )

        When("벌크 리인덱싱을 호출하면") {
            val result = facade.bulkReindex()

            Then("인덱싱 결과가 반환된다") {
                result.totalRequested shouldBe 1000
                result.totalIndexed shouldBe 995
                result.totalFailed shouldBe 5
            }
        }
    }
})
