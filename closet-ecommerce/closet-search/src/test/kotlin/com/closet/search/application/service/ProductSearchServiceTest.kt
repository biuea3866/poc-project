package com.closet.search.application.service

import com.closet.search.application.dto.ProductSearchFilter
import com.closet.search.application.dto.ProductSearchResponse
import com.closet.search.application.dto.ProductServicePageResponse
import com.closet.search.application.dto.ProductServiceResponse
import com.closet.search.domain.ProductDocument
import com.closet.search.infrastructure.client.ProductServiceClient
import com.closet.search.infrastructure.repository.ProductSearchRepository
import com.closet.search.infrastructure.repository.ProductSearchRepositoryCustom
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.Optional

class ProductSearchServiceTest : BehaviorSpec({

    val productSearchRepository = mockk<ProductSearchRepository>()
    val productSearchRepositoryCustom = mockk<ProductSearchRepositoryCustom>()
    val productServiceClient = mockk<ProductServiceClient>()

    // save() 기본 stub: 입력값을 그대로 반환
    every { productSearchRepository.save(any<ProductDocument>()) } answers { firstArg() }
    every { productSearchRepository.saveAll(any<Iterable<ProductDocument>>()) } answers { firstArg() }
    every { productSearchRepository.existsById(any()) } returns false
    every { productSearchRepository.deleteById(any()) } returns Unit

    val service = ProductSearchService(
        productSearchRepository = productSearchRepository,
        productSearchRepositoryCustom = productSearchRepositoryCustom,
        productServiceClient = productServiceClient,
    )

    Given("상품 검색 시") {

        val searchResponse = ProductSearchResponse(
            productId = 1L,
            name = "오버핏 맨투맨",
            brandName = "무신사 스탠다드",
            categoryName = "상의",
            basePrice = BigDecimal("39000"),
            salePrice = BigDecimal("29000"),
            discountRate = 25,
            sizes = listOf("S", "M", "L", "XL"),
            colors = listOf("블랙", "화이트"),
            fitType = "OVERFIT",
            gender = "UNISEX",
            season = "FW",
            imageUrl = "https://cdn.closet.com/product/1.jpg",
            reviewCount = 350,
            avgRating = 4.5,
            popularityScore = 85.0,
            highlights = mapOf("name" to listOf("<em>맨투맨</em>")),
        )

        val filter = ProductSearchFilter(keyword = "맨투맨")
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(searchResponse), pageable, 1)

        every { productSearchRepositoryCustom.search(filter, pageable) } returns page

        When("키워드로 검색하면") {
            val result = service.search(filter, pageable)

            Then("검색 결과가 하이라이팅과 함께 반환된다") {
                result.content.size shouldBe 1
                result.content[0].productId shouldBe 1L
                result.content[0].name shouldBe "오버핏 맨투맨"
                result.content[0].brandName shouldBe "무신사 스탠다드"
                result.content[0].salePrice shouldBe BigDecimal("29000")
                result.content[0].highlights shouldNotBe emptyMap<String, List<String>>()
            }
        }
    }

    Given("자동완성 검색 시") {

        val documents = listOf(
            ProductDocument(
                productId = 1L,
                name = "오버핏 맨투맨",
                description = "",
                brandId = 1L,
                brandName = "무신사 스탠다드",
                categoryId = 1L,
                basePrice = BigDecimal("39000"),
                salePrice = BigDecimal("29000"),
                status = "ACTIVE",
                imageUrl = "https://cdn.closet.com/1.jpg",
            ),
            ProductDocument(
                productId = 2L,
                name = "오버핏 후드티",
                description = "",
                brandId = 1L,
                brandName = "무신사 스탠다드",
                categoryId = 1L,
                basePrice = BigDecimal("45000"),
                salePrice = BigDecimal("35000"),
                status = "ACTIVE",
                imageUrl = "https://cdn.closet.com/2.jpg",
            ),
        )

        every { productSearchRepositoryCustom.autocomplete("오버", 10) } returns documents

        When("키워드 '오버'로 자동완성 검색하면") {
            val result = service.autocomplete("오버", 10)

            Then("자동완성 후보가 반환된다") {
                result.size shouldBe 2
                result[0].name shouldBe "오버핏 맨투맨"
                result[1].name shouldBe "오버핏 후드티"
            }
        }
    }

    Given("상품 인덱싱 시") {

        When("ProductCreated 이벤트가 수신되면") {
            service.indexProduct(
                productId = 10L,
                name = "슬림핏 청바지",
                description = "스트레치 데님 소재",
                brandId = 2L,
                categoryId = 3L,
                basePrice = BigDecimal("59000"),
                salePrice = BigDecimal("49000"),
                discountRate = 16,
                status = "ACTIVE",
                season = "SS",
                fitType = "SLIM",
                gender = "MALE",
                sizes = listOf("28", "30", "32"),
                colors = listOf("인디고", "블랙"),
                imageUrl = "https://cdn.closet.com/10.jpg",
            )

            Then("ES에 문서가 인덱싱된다") {
                verify(exactly = 1) { productSearchRepository.save(any()) }
            }
        }
    }

    Given("상품 업데이트 시") {

        val existingDoc = ProductDocument(
            productId = 10L,
            name = "슬림핏 청바지",
            description = "스트레치 데님 소재",
            brandId = 2L,
            categoryId = 3L,
            basePrice = BigDecimal("59000"),
            salePrice = BigDecimal("49000"),
            status = "ACTIVE",
            popularityScore = 70.0,
            salesCount = 500,
            reviewCount = 100,
            avgRating = 4.2,
        )

        every { productSearchRepository.findById(10L) } returns Optional.of(existingDoc)

        When("ProductUpdated 이벤트가 수신되면") {
            service.updateProduct(
                productId = 10L,
                name = "슬림핏 청바지 (리뉴얼)",
                description = "개선된 스트레치 데님 소재",
                brandId = 2L,
                categoryId = 3L,
                basePrice = BigDecimal("59000"),
                salePrice = BigDecimal("45000"),
                discountRate = 23,
                status = "ACTIVE",
                season = "SS",
                fitType = "SLIM",
                gender = "MALE",
                sizes = listOf("28", "30", "32", "34"),
                colors = listOf("인디고", "블랙", "그레이"),
                imageUrl = "https://cdn.closet.com/10-v2.jpg",
            )

            Then("기존 popularityScore/reviewCount가 유지되며 업데이트된다") {
                verify(exactly = 1) {
                    productSearchRepository.save(match {
                        it.name == "슬림핏 청바지 (리뉴얼)" &&
                            it.popularityScore == 70.0 &&
                            it.reviewCount == 100
                    })
                }
            }
        }
    }

    Given("상품 삭제 시") {

        every { productSearchRepository.existsById(10L) } returns true

        When("ProductDeleted 이벤트가 수신되면") {
            service.deleteProduct(10L)

            Then("ES에서 문서가 삭제된다") {
                verify(exactly = 1) { productSearchRepository.deleteById(10L) }
            }
        }

        every { productSearchRepository.existsById(999L) } returns false

        When("존재하지 않는 상품 삭제 요청이 오면") {
            service.deleteProduct(999L)

            Then("삭제를 스킵한다") {
                verify(exactly = 0) { productSearchRepository.deleteById(999L) }
            }
        }
    }

    Given("리뷰 집계 업데이트 시") {

        val existingDoc = ProductDocument(
            productId = 5L,
            name = "니트 스웨터",
            description = "부드러운 울 소재",
            brandId = 1L,
            categoryId = 1L,
            basePrice = BigDecimal("49000"),
            salePrice = BigDecimal("39000"),
            status = "ACTIVE",
            reviewCount = 50,
            avgRating = 4.0,
        )

        every { productSearchRepository.findById(5L) } returns Optional.of(existingDoc)

        When("ReviewSummaryUpdated 이벤트가 수신되면") {
            service.updateReviewSummary(productId = 5L, reviewCount = 55, avgRating = 4.1)

            Then("reviewCount와 avgRating만 업데이트된다") {
                verify(exactly = 1) {
                    productSearchRepository.save(match {
                        it.productId == 5L &&
                            it.reviewCount == 55 &&
                            it.avgRating == 4.1 &&
                            it.name == "니트 스웨터"
                    })
                }
            }
        }
    }

    Given("벌크 리인덱싱 시") {

        val products = (1..3).map { i ->
            ProductServiceResponse(
                id = i.toLong(),
                name = "상품 $i",
                description = "설명 $i",
                brandId = 1L,
                brandName = "브랜드 A",
                categoryId = 1L,
                categoryName = "상의",
                basePrice = BigDecimal("30000"),
                salePrice = BigDecimal("25000"),
                discountRate = 16,
                status = "ACTIVE",
                season = null,
                fitType = null,
                gender = null,
                sizes = listOf("M", "L"),
                colors = listOf("블랙"),
                imageUrl = null,
            )
        }

        val pageResponse = ProductServicePageResponse(
            content = products,
            totalElements = 3,
            totalPages = 1,
            number = 0,
            size = 1000,
            last = true,
        )

        every { productServiceClient.fetchAllProducts(0, 1000) } returns pageResponse

        When("벌크 리인덱싱 API를 호출하면") {
            val result = service.bulkReindex()

            Then("전체 상품이 인덱싱되고 결과가 반환된다") {
                result.totalRequested shouldBe 3
                result.totalIndexed shouldBe 3
                result.totalFailed shouldBe 0
                result.elapsedMillis shouldNotBe 0
            }
        }
    }
})
