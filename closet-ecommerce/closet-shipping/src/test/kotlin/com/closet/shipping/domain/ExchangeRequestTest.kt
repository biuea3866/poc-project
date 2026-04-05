package com.closet.shipping.domain

import com.closet.common.vo.Money
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ExchangeRequestTest : BehaviorSpec({

    Given("ExchangeRequest 생성") {

        When("단순변심 교환 생성") {
            val exchangeRequest = createExchangeRequest(
                reason = ReturnReason.CHANGE_OF_MIND,
                shippingFee = Money.of(6000),
                shippingFeePayer = "BUYER",
            )

            Then("초기 상태는 REQUESTED") {
                exchangeRequest.status shouldBe ExchangeStatus.REQUESTED
            }

            Then("배송비 6,000원 BUYER 부담 (왕복)") {
                exchangeRequest.shippingFee shouldBe Money.of(6000)
                exchangeRequest.shippingFeePayer shouldBe "BUYER"
            }
        }

        When("불량 교환 생성") {
            val exchangeRequest = createExchangeRequest(
                reason = ReturnReason.DEFECTIVE,
                shippingFee = Money.ZERO,
                shippingFeePayer = "SELLER",
            )

            Then("배송비 0원 SELLER 부담") {
                exchangeRequest.shippingFee shouldBe Money.ZERO
                exchangeRequest.shippingFeePayer shouldBe "SELLER"
            }
        }
    }

    Given("교환 상태 전이 플로우") {

        When("정상 플로우: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> RESHIPPING -> COMPLETED") {
            val exchangeRequest = createExchangeRequest()

            exchangeRequest.schedulePickup("CJ1234567890")
            Then("PICKUP_SCHEDULED") { exchangeRequest.status shouldBe ExchangeStatus.PICKUP_SCHEDULED }
            Then("수거 송장번호 설정됨") { exchangeRequest.pickupTrackingNumber shouldBe "CJ1234567890" }

            exchangeRequest.completePickup()
            Then("PICKUP_COMPLETED") { exchangeRequest.status shouldBe ExchangeStatus.PICKUP_COMPLETED }

            exchangeRequest.startReshipping("CJ9876543210")
            Then("RESHIPPING") { exchangeRequest.status shouldBe ExchangeStatus.RESHIPPING }
            Then("재배송 송장번호 설정됨") { exchangeRequest.newTrackingNumber shouldBe "CJ9876543210" }

            exchangeRequest.complete()
            Then("COMPLETED") { exchangeRequest.status shouldBe ExchangeStatus.COMPLETED }
            Then("완료일시 설정됨") { exchangeRequest.completedAt shouldNotBe null }
        }
    }

    Given("교환 거절") {

        When("REQUESTED 상태에서 거절") {
            val exchangeRequest = createExchangeRequest()
            exchangeRequest.reject()

            Then("REJECTED") {
                exchangeRequest.status shouldBe ExchangeStatus.REJECTED
            }
        }

        When("PICKUP_SCHEDULED 상태에서 거절") {
            val exchangeRequest = createExchangeRequest()
            exchangeRequest.schedulePickup(null)
            exchangeRequest.reject()

            Then("REJECTED") {
                exchangeRequest.status shouldBe ExchangeStatus.REJECTED
            }
        }
    }

    Given("잘못된 상태 전이") {

        When("REQUESTED에서 바로 RESHIPPING") {
            val exchangeRequest = createExchangeRequest()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    exchangeRequest.startReshipping(null)
                }
            }
        }

        When("REQUESTED에서 바로 COMPLETED") {
            val exchangeRequest = createExchangeRequest()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    exchangeRequest.complete()
                }
            }
        }

        When("COMPLETED에서 다시 RESHIPPING") {
            val exchangeRequest = createExchangeRequest()
            exchangeRequest.schedulePickup(null)
            exchangeRequest.completePickup()
            exchangeRequest.startReshipping(null)
            exchangeRequest.complete()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    exchangeRequest.startReshipping(null)
                }
            }
        }

        When("REJECTED에서 PICKUP_SCHEDULED") {
            val exchangeRequest = createExchangeRequest()
            exchangeRequest.reject()

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    exchangeRequest.schedulePickup(null)
                }
            }
        }
    }
})

private fun createExchangeRequest(
    reason: ReturnReason = ReturnReason.CHANGE_OF_MIND,
    shippingFee: Money = Money.of(6000),
    shippingFeePayer: String = "BUYER",
): ExchangeRequest {
    return ExchangeRequest.create(
        orderId = 1L,
        orderItemId = 1L,
        memberId = 1L,
        sellerId = 1L,
        originalProductOptionId = 100L,
        newProductOptionId = 200L,
        quantity = 1,
        reason = reason,
        reasonDetail = "사이즈가 맞지 않습니다",
        shippingFee = shippingFee,
        shippingFeePayer = shippingFeePayer,
    )
}
