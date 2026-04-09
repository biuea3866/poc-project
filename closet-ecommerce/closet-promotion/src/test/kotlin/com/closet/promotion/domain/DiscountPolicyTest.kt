package com.closet.promotion.domain

import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountPolicy
import com.closet.promotion.domain.discount.DiscountType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal
import java.time.ZonedDateTime

class DiscountPolicyTest : BehaviorSpec({

    Given("정액 할인 정책") {
        val policy =
            DiscountPolicy.create(
                name = "5000원 할인",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("5000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("50000원 주문에 할인 계산") {
            val discount = policy.calculateDiscount(BigDecimal("50000"))

            Then("5000원이 할인된다") {
                discount.compareTo(BigDecimal("5000")) shouldBe 0
            }
        }

        When("주문 금액이 할인 금액보다 작을 때") {
            val discount = policy.calculateDiscount(BigDecimal("3000"))

            Then("주문 금액까지만 할인된다") {
                discount.compareTo(BigDecimal("3000")) shouldBe 0
            }
        }
    }

    Given("정률 할인 정책") {
        val policy =
            DiscountPolicy.create(
                name = "10% 할인",
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

        When("50000원 주문에 할인 계산") {
            val discount = policy.calculateDiscount(BigDecimal("50000"))

            Then("5000원 할인된다 (50000 * 10%)") {
                discount.compareTo(BigDecimal("5000")) shouldBe 0
            }
        }

        When("할인 금액이 최대 할인 한도 초과") {
            val discount = policy.calculateDiscount(BigDecimal("200000"))

            Then("최대 할인 금액(10000원)이 적용된다") {
                discount.compareTo(BigDecimal("10000")) shouldBe 0
            }
        }
    }

    Given("정률 할인 정책 (최대 할인 한도 없음)") {
        val policy =
            DiscountPolicy.create(
                name = "20% 할인 (한도 없음)",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("20"),
                maxDiscountAmount = null,
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("200000원 주문에 할인 계산") {
            val discount = policy.calculateDiscount(BigDecimal("200000"))

            Then("40000원 할인된다") {
                discount.compareTo(BigDecimal("40000")) shouldBe 0
            }
        }
    }

    Given("조건부 할인 정책 - 카테고리 기반") {
        val policy =
            DiscountPolicy.create(
                name = "상의 카테고리 10% 할인",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("10"),
                conditionType = ConditionType.CATEGORY,
                conditionValue = "1",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("매칭되는 카테고리 ID로 조건 확인") {
            val matches = policy.matchesCondition(categoryId = 1L)

            Then("조건에 부합한다") {
                matches shouldBe true
            }
        }

        When("매칭되지 않는 카테고리 ID로 조건 확인") {
            val matches = policy.matchesCondition(categoryId = 2L)

            Then("조건에 부합하지 않는다") {
                matches shouldBe false
            }
        }
    }

    Given("조건부 할인 정책 - 브랜드 기반") {
        val policy =
            DiscountPolicy.create(
                name = "나이키 브랜드 15% 할인",
                discountType = DiscountType.PERCENT,
                discountValue = BigDecimal("15"),
                conditionType = ConditionType.BRAND,
                conditionValue = "10",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("매칭되는 브랜드 ID로 조건 확인") {
            val matches = policy.matchesCondition(brandId = 10L)

            Then("조건에 부합한다") {
                matches shouldBe true
            }
        }

        When("매칭되지 않는 브랜드 ID로 조건 확인") {
            val matches = policy.matchesCondition(brandId = 20L)

            Then("조건에 부합하지 않는다") {
                matches shouldBe false
            }
        }
    }

    Given("조건부 할인 정책 - 금액 범위 기반") {
        val policy =
            DiscountPolicy.create(
                name = "5만원 이상 주문 시 3000원 할인",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("3000"),
                conditionType = ConditionType.AMOUNT_RANGE,
                conditionValue = "50000",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("주문 금액이 조건 이상일 때") {
            val matches = policy.matchesCondition(orderAmount = BigDecimal("60000"))

            Then("조건에 부합한다") {
                matches shouldBe true
            }
        }

        When("주문 금액이 조건 미만일 때") {
            val matches = policy.matchesCondition(orderAmount = BigDecimal("30000"))

            Then("조건에 부합하지 않는다") {
                matches shouldBe false
            }
        }
    }

    Given("할인 정책 활성 상태 검증") {
        When("기간이 만료된 정책") {
            val expired =
                DiscountPolicy.create(
                    name = "만료된 할인",
                    discountType = DiscountType.FIXED,
                    discountValue = BigDecimal("1000"),
                    conditionType = ConditionType.ALL,
                    conditionValue = "",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().minusDays(30),
                    endedAt = ZonedDateTime.now().minusDays(1),
                )

            Then("활성 상태가 아니다") {
                expired.isCurrentlyActive() shouldBe false
            }
        }

        When("아직 시작되지 않은 정책") {
            val future =
                DiscountPolicy.create(
                    name = "미래 할인",
                    discountType = DiscountType.FIXED,
                    discountValue = BigDecimal("1000"),
                    conditionType = ConditionType.ALL,
                    conditionValue = "",
                    priority = 1,
                    isStackable = false,
                    startedAt = ZonedDateTime.now().plusDays(1),
                    endedAt = ZonedDateTime.now().plusDays(30),
                )

            Then("활성 상태가 아니다") {
                future.isCurrentlyActive() shouldBe false
            }
        }
    }

    Given("잘못된 할인 정책 생성") {
        When("할인 값이 0 이하일 때") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    DiscountPolicy.create(
                        name = "잘못된 할인",
                        discountType = DiscountType.FIXED,
                        discountValue = BigDecimal.ZERO,
                        conditionType = ConditionType.ALL,
                        conditionValue = "",
                        priority = 1,
                        isStackable = false,
                        startedAt = ZonedDateTime.now().minusDays(1),
                        endedAt = ZonedDateTime.now().plusDays(30),
                    )
                }
            }
        }

        When("정률 할인이 100% 초과일 때") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    DiscountPolicy.create(
                        name = "잘못된 정률 할인",
                        discountType = DiscountType.PERCENT,
                        discountValue = BigDecimal("101"),
                        conditionType = ConditionType.ALL,
                        conditionValue = "",
                        priority = 1,
                        isStackable = false,
                        startedAt = ZonedDateTime.now().minusDays(1),
                        endedAt = ZonedDateTime.now().plusDays(30),
                    )
                }
            }
        }

        When("종료일이 시작일 이전일 때") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    DiscountPolicy.create(
                        name = "잘못된 기간",
                        discountType = DiscountType.FIXED,
                        discountValue = BigDecimal("1000"),
                        conditionType = ConditionType.ALL,
                        conditionValue = "",
                        priority = 1,
                        isStackable = false,
                        startedAt = ZonedDateTime.now().plusDays(30),
                        endedAt = ZonedDateTime.now().minusDays(1),
                    )
                }
            }
        }
    }

    Given("비활성화") {
        val policy =
            DiscountPolicy.create(
                name = "비활성화 테스트",
                discountType = DiscountType.FIXED,
                discountValue = BigDecimal("1000"),
                conditionType = ConditionType.ALL,
                conditionValue = "",
                priority = 1,
                isStackable = false,
                startedAt = ZonedDateTime.now().minusDays(1),
                endedAt = ZonedDateTime.now().plusDays(30),
            )

        When("비활성화 처리") {
            policy.deactivate()

            Then("활성 상태가 아니다") {
                policy.isCurrentlyActive() shouldBe false
            }
        }
    }
})
