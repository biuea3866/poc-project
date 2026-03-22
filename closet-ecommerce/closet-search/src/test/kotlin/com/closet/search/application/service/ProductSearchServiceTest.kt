package com.closet.search.application.service

import com.closet.search.application.dto.ProductSearchFilter
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

class ProductSearchServiceTest : BehaviorSpec({

    val productSearchRepository = mockk<ProductSearchRepository>(relaxed = true)
    val productSearchRepositoryCustom = mockk<ProductSearchRepositoryCustom>()
    val productServiceClient = mockk<ProductServiceClient>()
    val productSearchService = ProductSearchService(
        productSearchRepository,
        productSearchRepositoryCustom,
        productServiceClient,
    )

    Given("검색 키워드와 필터가 주어졌을 때") {
        val keyword = "티셔츠"
        val filter = ProductSearchFilter(
            categoryId = 10L,
            gender = "UNISEX",
        )
        val pageable = PageRequest.of(0, 20)

        val documents = listOf(
            createProductDocument(1L, "오버핏 반팔 티셔츠", categoryId = 10L, gender = "UNISEX"),
            createProductDocument(2L, "베이직 티셔츠", categoryId = 10L, gender = "UNISEX"),
        )

        every { productSearchRepositoryCustom.search(keyword, filter, pageable) } returns PageImpl(documents, pageable, 2)

        When("search를 호출하면") {
            val result = productSearchService.search(keyword, filter, pageable)

            Then("검색 결과가 반환된다") {
                result.totalElements shouldBe 2
                result.content.size shouldBe 2
                result.content[0].name shouldBe "오버핏 반팔 티셔츠"
                result.content[1].name shouldBe "베이직 티셔츠"
            }
        }
    }

    Given("카테고리 필터만 주어졌을 때") {
        val filter = ProductSearchFilter(categoryId = 20L)
        val pageable = PageRequest.of(0, 20)

        val documents = listOf(
            createProductDocument(3L, "슬림 청바지", categoryId = 20L),
        )

        every { productSearchRepositoryCustom.search(null, filter, pageable) } returns PageImpl(documents, pageable, 1)

        When("키워드 없이 search를 호출하면") {
            val result = productSearchService.search(null, filter, pageable)

            Then("필터에 맞는 결과가 반환된다") {
                result.totalElements shouldBe 1
                result.content[0].name shouldBe "슬림 청바지"
                result.content[0].categoryId shouldBe 20L
            }
        }
    }

    Given("가격 범위 필터가 주어졌을 때") {
        val filter = ProductSearchFilter(minPrice = 10000, maxPrice = 50000)
        val pageable = PageRequest.of(0, 20)

        val documents = listOf(
            createProductDocument(4L, "기본 셔츠", salePrice = 35000),
            createProductDocument(5L, "반팔 폴로", salePrice = 29000),
        )

        every { productSearchRepositoryCustom.search(null, filter, pageable) } returns PageImpl(documents, pageable, 2)

        When("search를 호출하면") {
            val result = productSearchService.search(null, filter, pageable)

            Then("가격 범위 내의 결과가 반환된다") {
                result.totalElements shouldBe 2
                result.content.all { it.salePrice in 10000..50000 } shouldBe true
            }
        }
    }

    Given("자동완성 요청이 주어졌을 때") {
        val prefix = "티셔"
        val suggestions = listOf("티셔츠", "티셔츠 반팔", "티셔츠 긴팔")

        every { productSearchRepositoryCustom.autocomplete(prefix, 5) } returns suggestions

        When("autocomplete를 호출하면") {
            val result = productSearchService.autocomplete(prefix, 5)

            Then("자동완성 제안 목록이 반환된다") {
                result.suggestions.size shouldBe 3
                result.suggestions[0] shouldBe "티셔츠"
            }
        }
    }

    Given("인덱싱할 상품 정보가 주어졌을 때") {
        val product = ProductServiceResponse(
            id = 10L,
            name = "겨울 패딩",
            description = "따뜻한 겨울 패딩 자켓",
            brandId = 1L,
            categoryId = 30L,
            basePrice = 199000,
            salePrice = 149000,
            discountRate = 25,
            status = "ACTIVE",
            season = "FW",
            fitType = "OVERSIZED",
            gender = "UNISEX",
        )

        every { productSearchRepository.save(any()) } answers { firstArg() }

        When("indexProduct를 호출하면") {
            productSearchService.indexProduct(product)

            Then("ES에 인덱싱된다") {
                verify(exactly = 1) { productSearchRepository.save(any()) }
            }
        }
    }

    Given("벌크 인덱싱할 상품 목록이 주어졌을 때") {
        val products = listOf(
            ProductServiceResponse(1L, "상품1", null, 1L, 10L, 30000, 25000, 16, "ACTIVE", "SS", "REGULAR", "MALE"),
            ProductServiceResponse(2L, "상품2", null, 1L, 10L, 40000, 35000, 12, "ACTIVE", "FW", "SLIM", "FEMALE"),
            ProductServiceResponse(3L, "상품3", null, 2L, 20L, 50000, 45000, 10, "ACTIVE", "ALL", "OVERSIZED", "UNISEX"),
        )

        every { productSearchRepository.saveAll(any<List<ProductDocument>>()) } answers { firstArg() }

        When("bulkIndex를 호출하면") {
            productSearchService.bulkIndex(products)

            Then("3건이 벌크 인덱싱된다") {
                verify(exactly = 1) { productSearchRepository.saveAll(any<List<ProductDocument>>()) }
            }
        }
    }

    Given("삭제할 상품 ID가 주어졌을 때") {
        every { productSearchRepository.deleteById(99L) } returns Unit

        When("deleteProduct를 호출하면") {
            productSearchService.deleteProduct(99L)

            Then("ES에서 삭제된다") {
                verify(exactly = 1) { productSearchRepository.deleteById(99L) }
            }
        }
    }
})

private fun createProductDocument(
    id: Long,
    name: String,
    categoryId: Long = 10L,
    brandId: Long = 1L,
    salePrice: Long = 29000,
    gender: String = "UNISEX",
) = ProductDocument(
    id = id,
    name = name,
    description = "테스트 상품 설명",
    brandId = brandId,
    brandName = "테스트 브랜드",
    categoryId = categoryId,
    categoryName = "테스트 카테고리",
    basePrice = 39000,
    salePrice = salePrice,
    discountRate = 25,
    status = "ACTIVE",
    season = "SS",
    fitType = "OVERSIZED",
    gender = gender,
    createdAt = "2026-03-22T00:00:00",
)
