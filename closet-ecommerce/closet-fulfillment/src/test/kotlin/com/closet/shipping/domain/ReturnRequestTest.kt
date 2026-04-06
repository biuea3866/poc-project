package com.closet.shipping.domain

import com.closet.common.vo.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReturnRequestTest : BehaviorSpec({

    Given("ReturnRequest 생성") {

        When("단순변심 반품 생성") {
            val returnRequest =
                createReturnRequest(
                    reason = ReturnReason.CHANGE_OF_MIND,
                    shippingFee = Money.of(3000),
                    shippingFeePayer = "BUYER",
                    // 50000 - 3000
                    refundAmount = Money.of(47000),
                )

            Then("초기 상태는 REQUESTED") {
                returnRequest.status shouldBe ReturnStatus.REQUESTED
            }

            Then("배송비 3,000원 BUYER 부담") {
                returnRequest.shippingFee shouldBe Money.of(3000)
                returnRequest.shippingFeePayer shouldBe "BUYER"
            }

            Then("환불금액 47,000원 (50,000 - 3,000)") {
                returnRequest.refundAmount shouldBe Money.of(47000)
            }
        }

        When("불량 반품 생성") {
            val returnRequest =
                createReturnRequest(
                    reason = ReturnReason.DEFECTIVE,
                    shippingFee = Money.ZERO,
                    shippingFeePayer = "SELLER",
                    refundAmount = Money.of(50000),
                )

            Then("배송비 0원 SELLER 부담") {
                returnRequest.shippingFee shouldBe Money.ZERO
                returnRequest.shippingFeePayer shouldBe "SELLER"
            }

            Then("환불금액은 전액 50,000원") {
                returnRequest.refundAmount shouldBe Money.of(50000)
            }
        }
    }

    Given("반품 상태 전이 플로우") {

        When("정상 플로우: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> INSPECTING -> APPROVED -> COMPLETED") {
            val returnRequest = createReturnRequest()

            returnRequest.schedulePickup("CJ1234567890")
            Then("PICKUP_SCHEDULED") { returnRequest.status shouldBe ReturnStatus.PICKUP_SCHEDULED }
            Then("수거 송장번호 설정됨") { returnRequest.pickupTrackingNumber shouldBe "CJ1234567890" }

            returnRequest.completePickup()
            Then("PICKUP_COMPLETED") { returnRequest.status shouldBe ReturnStatus.PICKUP_COMPLETED }

            returnRequest.startInspection()
            Then("INSPECTING") { returnRequest.status shouldBe ReturnStatus.INSPECTING }
            Then("검수일시 설정됨") { returnRequest.inspectedAt shouldNotBe null }

            returnRequest.approve()
            Then("APPROVED") { returnRequest.status shouldBe ReturnStatus.APPROVED }

            returnRequest.complete()
            Then("COMPLETED") { returnRequest.status shouldBe ReturnStatus.COMPLETED }
            Then("완료일시 설정됨") { returnRequest.completedAt shouldNotBe null }
        }
    }

    Given("반품 거절") {

        When("REQUESTED 상태에서 거절") {
            val returnRequest = createReturnRequest()
            returnRequest.reject("수거 불가")

            Then("REJECTED") {
                returnRequest.status shouldBe ReturnStatus.REJECTED
            }
        }

        When("INSPECTING 상태에서 거절") {
            val returnRequest = createReturnRequest()
            returnRequest.schedulePickup(null)
            returnRequest.completePickup()
            returnRequest.startInspection()
            returnRequest.reject("사용 흔적 발견")

            Then("REJECTED") {
                returnRequest.status shouldBe ReturnStatus.REJECTED
            }
        }
    }

    Given("잘못된 상태 전이") {

        When("REQUESTED에서 바로 APPROVED") {
            val returnRequest = createReturnRequest()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    returnRequest.approve()
                }
            }
        }

        When("COMPLETED에서 APPROVED") {
            val returnRequest = createReturnRequest()
            returnRequest.schedulePickup(null)
            returnRequest.completePickup()
            returnRequest.startInspection()
            returnRequest.approve()
            returnRequest.complete()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    returnRequest.approve()
                }
            }
        }
    }
})

private fun createReturnRequest(
    reason: ReturnReason = ReturnReason.CHANGE_OF_MIND,
    shippingFee: Money = Money.of(3000),
    shippingFeePayer: String = "BUYER",
    refundAmount: Money = Money.of(47000),
): ReturnRequest {
    return ReturnRequest.create(
        orderId = 1L,
        orderItemId = 1L,
        memberId = 1L,
        sellerId = 1L,
        productOptionId = 1L,
        quantity = 1,
        reason = reason,
        reasonDetail = "사이즈가 맞지 않습니다",
        shippingFee = shippingFee,
        shippingFeePayer = shippingFeePayer,
        refundAmount = refundAmount,
    )
}
