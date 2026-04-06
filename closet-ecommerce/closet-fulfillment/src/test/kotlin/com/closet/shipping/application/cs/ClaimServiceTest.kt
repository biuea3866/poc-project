package com.closet.shipping.application.cs

import com.closet.common.exception.BusinessException
import com.closet.shipping.domain.cs.claim.ClaimReasonCategory
import com.closet.shipping.domain.cs.claim.ClaimRequest
import com.closet.shipping.domain.cs.claim.ClaimRequestRepository
import com.closet.shipping.domain.cs.claim.ClaimStatus
import com.closet.shipping.domain.cs.claim.ClaimType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.math.BigDecimal
import java.util.Optional

class ClaimServiceTest : BehaviorSpec({

    val claimRequestRepository = mockk<ClaimRequestRepository>()
    val claimService = ClaimService(claimRequestRepository = claimRequestRepository)

    Given("클레임 접수") {
        val command =
            CreateClaimCommand(
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.DEFECTIVE,
                reasonDetail = "상품 하자 발견",
            )

        val claimSlot = slot<ClaimRequest>()
        every { claimRequestRepository.save(capture(claimSlot)) } answers { claimSlot.captured }

        When("반품 접수 요청") {
            val response = claimService.createClaim(command)

            Then("REQUESTED 상태로 생성된다") {
                response.orderId shouldBe 1L
                response.claimType shouldBe ClaimType.RETURN
                response.status shouldBe ClaimStatus.REQUESTED
                response.reasonCategory shouldBe ClaimReasonCategory.DEFECTIVE
            }
        }
    }

    Given("클레임 승인") {
        val claim =
            ClaimRequest.create(
                orderId = 1L,
                orderItemId = 10L,
                memberId = 100L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.DEFECTIVE,
            )

        every { claimRequestRepository.findById(1L) } returns Optional.of(claim)

        When("승인 처리") {
            val response = claimService.approveClaim(1L, BigDecimal("29900"))

            Then("APPROVED 상태가 되고 환불 금액이 설정된다") {
                response.status shouldBe ClaimStatus.APPROVED
                response.refundAmount shouldBe BigDecimal("29900")
            }
        }
    }

    Given("클레임 거부") {
        val claim =
            ClaimRequest.create(
                orderId = 2L,
                orderItemId = 20L,
                memberId = 200L,
                claimType = ClaimType.EXCHANGE,
                reasonCategory = ClaimReasonCategory.CHANGE_OF_MIND,
            )

        every { claimRequestRepository.findById(2L) } returns Optional.of(claim)

        When("거부 처리") {
            val response = claimService.rejectClaim(2L)

            Then("REJECTED 상태가 된다") {
                response.status shouldBe ClaimStatus.REJECTED
            }
        }
    }

    Given("클레임 완료") {
        val claim =
            ClaimRequest.create(
                orderId = 3L,
                orderItemId = 30L,
                memberId = 300L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.WRONG_ITEM,
            )
        claim.approve(BigDecimal("15000"))

        every { claimRequestRepository.findById(3L) } returns Optional.of(claim)

        When("완료 처리") {
            val response = claimService.completeClaim(3L)

            Then("COMPLETED 상태가 된다") {
                response.status shouldBe ClaimStatus.COMPLETED
            }
        }
    }

    Given("잘못된 상태에서 승인 시도") {
        val claim =
            ClaimRequest.create(
                orderId = 4L,
                orderItemId = 40L,
                memberId = 400L,
                claimType = ClaimType.RETURN,
                reasonCategory = ClaimReasonCategory.DEFECTIVE,
            )
        claim.approve(BigDecimal("10000"))
        claim.complete()

        every { claimRequestRepository.findById(4L) } returns Optional.of(claim)

        When("COMPLETED 상태에서 승인 시도") {
            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    claimService.approveClaim(4L, BigDecimal("5000"))
                }
            }
        }
    }

    Given("존재하지 않는 클레임 조회") {
        every { claimRequestRepository.findById(999L) } returns Optional.empty()

        When("조회 시도") {
            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    claimService.getClaim(999L)
                }
            }
        }
    }

    Given("회원별 클레임 조회") {
        val claims =
            listOf(
                ClaimRequest.create(
                    orderId = 1L,
                    orderItemId = 10L,
                    memberId = 100L,
                    claimType = ClaimType.RETURN,
                    reasonCategory = ClaimReasonCategory.DEFECTIVE,
                ),
                ClaimRequest.create(
                    orderId = 2L,
                    orderItemId = 20L,
                    memberId = 100L,
                    claimType = ClaimType.EXCHANGE,
                    reasonCategory = ClaimReasonCategory.SIZE_MISMATCH,
                ),
            )

        every { claimRequestRepository.findByMemberId(100L) } returns claims

        When("회원 ID로 조회") {
            val response = claimService.getClaimsByMember(100L)

            Then("해당 회원의 클레임 목록이 반환된다") {
                response.size shouldBe 2
                response[0].claimType shouldBe ClaimType.RETURN
                response[1].claimType shouldBe ClaimType.EXCHANGE
            }
        }
    }
})
