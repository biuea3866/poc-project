package com.closet.shipping.domain.cs.claim

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal

class ClaimRequestTest : BehaviorSpec({

    Given("클레임 요청 생성") {
        When("반품 요청 생성") {
            val claim =
                ClaimRequest.create(
                    orderId = 1L,
                    orderItemId = 10L,
                    memberId = 100L,
                    claimType = ClaimType.RETURN,
                    reasonCategory = ClaimReasonCategory.DEFECTIVE,
                    reasonDetail = "상품 하자 발견",
                )

            Then("REQUESTED 상태로 생성된다") {
                claim.orderId shouldBe 1L
                claim.orderItemId shouldBe 10L
                claim.memberId shouldBe 100L
                claim.claimType shouldBe ClaimType.RETURN
                claim.reasonCategory shouldBe ClaimReasonCategory.DEFECTIVE
                claim.reasonDetail shouldBe "상품 하자 발견"
                claim.status shouldBe ClaimStatus.REQUESTED
                claim.refundAmount shouldBe BigDecimal.ZERO
            }
        }

        When("교환 요청 생성") {
            val claim =
                ClaimRequest.create(
                    orderId = 2L,
                    orderItemId = 20L,
                    memberId = 200L,
                    claimType = ClaimType.EXCHANGE,
                    reasonCategory = ClaimReasonCategory.SIZE_MISMATCH,
                )

            Then("교환 타입으로 생성된다") {
                claim.claimType shouldBe ClaimType.EXCHANGE
                claim.reasonCategory shouldBe ClaimReasonCategory.SIZE_MISMATCH
                claim.status shouldBe ClaimStatus.REQUESTED
            }
        }
    }

    Given("REQUESTED 상태의 클레임") {
        val claim =
            ClaimRequest.create(
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.CHANGE_OF_MIND,
            )

        When("승인 처리") {
            claim.approve(BigDecimal("29900"))

            Then("APPROVED 상태로 변경되고 환불 금액이 설정된다") {
                claim.status shouldBe ClaimStatus.APPROVED
                claim.refundAmount shouldBe BigDecimal("29900")
                claim.approvedAt shouldBe claim.approvedAt // not null
            }
        }

        When("거부 처리") {
            val claim2 =
                ClaimRequest.create(
                    orderId = 3L,
                    orderItemId = 30L,
                    memberId = 300L,
                    claimType = ClaimType.RETURN,
                    reasonCategory = ClaimReasonCategory.CHANGE_OF_MIND,
                )
            claim2.reject()

            Then("REJECTED 상태로 변경된다") {
                claim2.status shouldBe ClaimStatus.REJECTED
            }
        }
    }

    Given("APPROVED 상태의 클레임") {
        val claim =
            ClaimRequest.create(
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.DEFECTIVE,
            )
        claim.approve(BigDecimal("29900"))

        When("완료 처리") {
            claim.complete()

            Then("COMPLETED 상태로 변경된다") {
                claim.status shouldBe ClaimStatus.COMPLETED
                claim.completedAt shouldBe claim.completedAt // not null
            }
        }
    }

    Given("ClaimStatus 상태 전이") {
        When("REQUESTED에서 APPROVED로 전이") {
            Then("전이 가능") {
                ClaimStatus.REQUESTED.canTransitionTo(ClaimStatus.APPROVED) shouldBe true
            }
        }

        When("REQUESTED에서 REJECTED로 전이") {
            Then("전이 가능") {
                ClaimStatus.REQUESTED.canTransitionTo(ClaimStatus.REJECTED) shouldBe true
            }
        }

        When("APPROVED에서 COMPLETED로 전이") {
            Then("전이 가능") {
                ClaimStatus.APPROVED.canTransitionTo(ClaimStatus.COMPLETED) shouldBe true
            }
        }

        When("COMPLETED에서 전이 시도") {
            Then("전이 불가") {
                ClaimStatus.entries.forEach { target ->
                    ClaimStatus.COMPLETED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("REJECTED에서 전이 시도") {
            Then("전이 불가") {
                ClaimStatus.entries.forEach { target ->
                    ClaimStatus.REJECTED.canTransitionTo(target) shouldBe false
                }
            }
        }

        When("터미널 상태 확인") {
            Then("COMPLETED는 터미널") {
                ClaimStatus.COMPLETED.isTerminal() shouldBe true
            }
            Then("REJECTED는 터미널") {
                ClaimStatus.REJECTED.isTerminal() shouldBe true
            }
            Then("REQUESTED는 터미널이 아님") {
                ClaimStatus.REQUESTED.isTerminal() shouldBe false
            }
        }
    }

    Given("잘못된 상태 전이") {
        val claim =
            ClaimRequest.create(
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.DEFECTIVE,
            )
        claim.approve(BigDecimal("29900"))
        claim.complete()

        When("COMPLETED에서 approve 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    claim.approve(BigDecimal("10000"))
                }
            }
        }

        When("COMPLETED에서 reject 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    claim.reject()
                }
            }
        }
    }
})
