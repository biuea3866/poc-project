package com.closet.product.application.service

import com.closet.common.exception.BusinessException
import com.closet.common.vo.Money
import com.closet.product.application.dto.ProductCreateRequest
import com.closet.product.application.dto.ProductOptionCreateRequest
import com.closet.product.domain.entity.Product
import com.closet.product.domain.enums.FitType
import com.closet.product.domain.enums.Gender
import com.closet.product.domain.enums.ProductStatus
import com.closet.product.domain.enums.Season
import com.closet.product.domain.enums.Size
import com.closet.product.domain.repository.ProductRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.util.Optional

class ProductServiceTest : BehaviorSpec({

    val productRepository = mockk<ProductRepository>()
    val productService = ProductService(productRepository)

    Given("상품 생성 요청이 주어졌을 때") {
        val request = ProductCreateRequest(
            name = "오버핏 반팔 티셔츠",
            description = "시원한 여름 반팔 티셔츠",
            brandId = 1L,
            categoryId = 10L,
            basePrice = BigDecimal(39000),
            salePrice = BigDecimal(29000),
            discountRate = 25,
            season = Season.SS,
            fitType = FitType.OVERSIZED,
            gender = Gender.UNISEX
        )

        val productSlot = slot<Product>()
        every { productRepository.save(capture(productSlot)) } answers {
            productSlot.captured
        }

        When("create를 호출하면") {
            val response = productService.create(request)

            Then("상품이 DRAFT 상태로 생성된다") {
                response.name shouldBe "오버핏 반팔 티셔츠"
                response.status shouldBe ProductStatus.DRAFT
                response.basePrice shouldBe BigDecimal(39000)
                response.salePrice shouldBe BigDecimal(29000)
                response.discountRate shouldBe 25
                response.season shouldBe Season.SS
                response.fitType shouldBe FitType.OVERSIZED
                response.gender shouldBe Gender.UNISEX
            }
        }
    }

    Given("DRAFT 상태의 상품이 존재할 때") {
        val product = Product(
            name = "슬림 청바지",
            description = "스트레치 슬림핏 청바지",
            brandId = 1L,
            categoryId = 20L,
            basePrice = Money(BigDecimal(59000)),
            salePrice = Money(BigDecimal(49000)),
            discountRate = 16,
            status = ProductStatus.DRAFT,
            season = Season.ALL,
            fitType = FitType.SLIM,
            gender = Gender.MALE
        )

        every { productRepository.findById(1L) } returns Optional.of(product)

        When("ACTIVE로 상태 변경하면") {
            val response = productService.changeStatus(1L, ProductStatus.ACTIVE)

            Then("상태가 ACTIVE로 변경된다") {
                response.status shouldBe ProductStatus.ACTIVE
            }
        }
    }

    Given("ACTIVE 상태의 상품이 존재할 때") {
        val product = Product(
            name = "레귤러 셔츠",
            description = "베이직 셔츠",
            brandId = 1L,
            categoryId = 10L,
            basePrice = Money(BigDecimal(45000)),
            salePrice = Money(BigDecimal(35000)),
            discountRate = 22,
            status = ProductStatus.ACTIVE
        )

        every { productRepository.findById(2L) } returns Optional.of(product)

        When("SOLD_OUT으로 상태 변경하면") {
            val response = productService.changeStatus(2L, ProductStatus.SOLD_OUT)

            Then("상태가 SOLD_OUT으로 변경된다") {
                response.status shouldBe ProductStatus.SOLD_OUT
            }
        }
    }

    Given("INACTIVE 상태의 상품이 존재할 때") {
        val product = Product(
            name = "겨울 패딩",
            description = "따뜻한 패딩",
            brandId = 1L,
            categoryId = 30L,
            basePrice = Money(BigDecimal(199000)),
            salePrice = Money(BigDecimal(149000)),
            discountRate = 25,
            status = ProductStatus.INACTIVE
        )

        every { productRepository.findById(3L) } returns Optional.of(product)

        When("SOLD_OUT으로 상태 변경을 시도하면") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    productService.changeStatus(3L, ProductStatus.SOLD_OUT)
                }
            }
        }
    }

    Given("상품에 옵션을 추가할 때") {
        val product = Product(
            name = "기본 티셔츠",
            description = "기본 반팔",
            brandId = 1L,
            categoryId = 10L,
            basePrice = Money(BigDecimal(25000)),
            salePrice = Money(BigDecimal(19000)),
            discountRate = 24,
            status = ProductStatus.DRAFT
        )

        every { productRepository.findById(4L) } returns Optional.of(product)
        every { productRepository.flush() } returns Unit

        val optionRequest = ProductOptionCreateRequest(
            size = Size.M,
            colorName = "블랙",
            colorHex = "#000000",
            skuCode = "TSH-BLK-M-001",
            additionalPrice = BigDecimal.ZERO
        )

        When("addOption을 호출하면") {
            val response = productService.addOption(4L, optionRequest)

            Then("옵션이 추가된다") {
                response.size shouldBe Size.M
                response.colorName shouldBe "블랙"
                response.colorHex shouldBe "#000000"
                response.skuCode shouldBe "TSH-BLK-M-001"
                product.options.size shouldBe 1
            }
        }
    }

    Given("존재하지 않는 상품 ID로 조회할 때") {
        every { productRepository.findById(999L) } returns Optional.empty()

        When("findById를 호출하면") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    productService.findById(999L)
                }
            }
        }
    }
})
