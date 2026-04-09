package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountHistory
import com.closet.promotion.domain.discount.DiscountPolicy
import com.closet.promotion.domain.discount.DiscountType
import com.closet.promotion.presentation.dto.ApplyDiscountRequest
import com.closet.promotion.presentation.dto.CreateDiscountPolicyRequest
import com.closet.promotion.repository.DiscountHistoryRepository
import com.closet.promotion.repository.DiscountPolicyRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.time.ZonedDateTime

class DiscountPolicyServiceTest : BehaviorSpec({

    val discountPolicyRepository = mockk<DiscountPolicyRepository>()
    val discountHistoryRepository = mockk<DiscountHistoryRepository>()
    val service = DiscountPolicyService(discountPolicyRepository, discountHistoryRepository)

    Given("할인 정책 생성") {
        val request =
            CreateDiscountPolicyRequest(
                name = "전체 상품 10% 할인",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("10"),
                maxDiscountAmount = BigDecimal("10000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        val saved = slot<DiscountPolicy>()
        every { discountPolicyRepository.save(capture(saved)) } answers { saved.captured }

        When("정상 생성") {
            val response = service.createPolicy(request)

            Then("정책이 저장된다") {
                verify(exactly = 1) { discountPolicyRepository.save(any()) }
                response.name shouldBe "전체 상품 10% 할인"
                response.discountType shouldBe DiscountType.PERCENT
            }
        }
    }

    Given("할인 적용 가능한 정책 조회") {
        val now = ZonedDateTime.now()
        val policy1 =
            DiscountPolicy.create(
                name = "전체 5000원 할인",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("5000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = now.minusDays(1),
                endedAt = now.plusDays(30),
            )
        val policy2 =
            DiscountPolicy.create(
                name = "카테고리 10% 할인",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("10"),
                conditionType = ConditionType.CATEGORY,
                conditionValue = "1",
                priority = 2,
                isStackable = true,
                startedAt = now.minusDays(1),
                endedAt = now.plusDays(30),
            )

        every { discountPolicyRepository.findActiveByConditions(any(), any(), any()) } returns listOf(policy1, policy2)

        When("카테고리ID=1 상품 주문 시 적용 가능 정책 조회") {
            val policies =
                service.findApplicablePolicies(
                    categoryId = 1L,
                    brandId = null,
                    orderAmount = BigDecimal("50000"),
                )

            Then("2개 정책이 조회된다") {
                policies shouldHaveSize 2
            }
        }
    }

    Given("할인 적용 (단일 정책)") {
        val now = ZonedDateTime.now()
        val policy =
            DiscountPolicy.create(
                name = "5000원 할인",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("5000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = now.minusDays(1),
                endedAt = now.plusDays(30),
            )

        every { discountPolicyRepository.findActiveByConditions(any(), any(), any()) } returns listOf(policy)
        val historySlot = slot<DiscountHistory>()
        every { discountHistoryRepository.save(capture(historySlot)) } answers { historySlot.captured }

        val request =
            ApplyDiscountRequest(
                orderId = 100L,
                memberId = 1L,
                originalAmount = BigDecimal("50000"),
                categoryId = null,
                brandId = null,
            )

        When("할인 적용") {
            val result = service.applyBestDiscount(request)

            Then("5000원 할인이 적용된다") {
                result.discountAmount.compareTo(BigDecimal("5000")) shouldBe 0
                result.finalAmount.compareTo(BigDecimal("45000")) shouldBe 0
            }

            Then("할인 이력이 저장된다") {
                verify(exactly = 1) { discountHistoryRepository.save(any()) }
            }
        }
    }

    Given("할인 적용 (중복 적용 가능 정책들)") {
        val now = ZonedDateTime.now()
        val policy1 =
            DiscountPolicy.create(
                name = "전체 3000원 할인",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("3000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = true,
                startedAt = now.minusDays(1),
                endedAt = now.plusDays(30),
            )
        val policy2 =
            DiscountPolicy.create(
                name = "카테고리 10% 할인",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("10"),
                conditionType = ConditionType.CATEGORY,
                conditionValue = "1",
                priority = 2,
                isStackable = true,
                startedAt = now.minusDays(1),
                endedAt = now.plusDays(30),
            )

        every { discountPolicyRepository.findActiveByConditions(any(), any(), any()) } returns listOf(policy1, policy2)
        every { discountHistoryRepository.save(any()) } answers { firstArg() }

        val request =
            ApplyDiscountRequest(
                orderId = 100L,
                memberId = 1L,
                originalAmount = BigDecimal("50000"),
                categoryId = 1L,
                brandId = null,
            )

        When("중복 할인 적용") {
            val result = service.applyStackedDiscounts(request)

            Then("두 할인이 모두 적용된다") {
                // 3000 (정액) + 50000*10% = 3000 + 5000 = 8000
                result.totalDiscountAmount.compareTo(BigDecimal("8000")) shouldBe 0
                result.finalAmount.compareTo(BigDecimal("42000")) shouldBe 0
            }

            Then("할인 이력이 2건 저장된다") {
                verify(exactly = 2) { discountHistoryRepository.save(any()) }
            }
        }
    }

    Given("적용 가능한 할인 정책이 없을 때") {
        every { discountPolicyRepository.findActiveByConditions(any(), any(), any()) } returns emptyList()

        val request =
            ApplyDiscountRequest(
                orderId = 100L,
                memberId = 1L,
                originalAmount = BigDecimal("50000"),
                categoryId = null,
                brandId = null,
            )

        When("할인 적용 시도") {
            val result = service.applyBestDiscount(request)

            Then("할인 없이 원래 금액 반환") {
                result.discountAmount.compareTo(BigDecimal.ZERO) shouldBe 0
                result.finalAmount.compareTo(BigDecimal("50000")) shouldBe 0
            }
        }
    }

    Given("할인 정책 비활성화") {
        val policy =
            DiscountPolicy.create(
                name = "비활성화 대상",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("1000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        every { discountPolicyRepository.findById(1L) } returns java.util.Optional.of(policy)
        every { discountPolicyRepository.save(any()) } answers { firstArg() }

        When("비활성화 처리") {
            val response = service.deactivatePolicy(1L)

            Then("비활성 상태가 된다") {
                response.isActive shouldBe false
            }
        }
    }

    Given("존재하지 않는 정책 비활성화") {
        every { discountPolicyRepository.findById(999L) } returns java.util.Optional.empty()

        When("비활성화 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    service.deactivatePolicy(999L)
                }
            }
        }
    }
})
