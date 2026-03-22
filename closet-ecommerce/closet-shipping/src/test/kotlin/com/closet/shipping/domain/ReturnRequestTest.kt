package com.closet.shipping.domain

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReturnRequestTest : BehaviorSpec({

    Given("반품 요청 생성") {
        When("정상 반품 요청") {
            val returnRequest = ReturnRequest.create(
                orderId = 1L,
                orderItemId = 100L,
                type = ReturnType.RETURN,
                reasonType = ReturnReasonType.CHANGE_OF_MIND,
                reasonDetail = "단순 변심입니다",
                shippingFeeBearer = ShippingFeeBearer.BUYER,
                returnShippingFee = 3000,
            )

            Then("REQUESTED 상태로 생성된다") {
                returnRequest.status shouldBe ReturnStatus.REQUESTED
            }

            Then("요청 정보가 올바르게 설정된다") {
                returnRequest.orderId shouldBe 1L
                returnRequest.orderItemId shouldBe 100L
                returnRequest.type shouldBe ReturnType.RETURN
                returnRequest.reasonType shouldBe ReturnReasonType.CHANGE_OF_MIND
                returnRequest.shippingFeeBearer shouldBe ShippingFeeBearer.BUYER
                returnRequest.returnShippingFee shouldBe 3000
            }
        }
    }

    Given("반품 승인") {
        When("REQUESTED 상태에서 승인") {
            val returnRequest = ReturnRequest.create(
                orderId = 1L,
                orderItemId = 100L,
                type = ReturnType.RETURN,
                reasonType = ReturnReasonType.DEFECT,
                shippingFeeBearer = ShippingFeeBearer.SELLER,
            )

            returnRequest.approve()

            Then("APPROVED 상태로 변경된다") {
                returnRequest.status shouldBe ReturnStatus.APPROVED
            }

            Then("approvedAt이 설정된다") {
                returnRequest.approvedAt shouldNotBe null
            }
        }

        When("APPROVED 상태에서 다시 승인 시도") {
            val returnRequest = ReturnRequest.create(
                orderId = 1L,
                orderItemId = 100L,
                type = ReturnType.RETURN,
                reasonType = ReturnReasonType.DEFECT,
                shippingFeeBearer = ShippingFeeBearer.SELLER,
            )
            returnRequest.approve()

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    returnRequest.approve()
                }
            }
        }
    }

    Given("반품 거절") {
        When("REQUESTED 상태에서 거절") {
            val returnRequest = ReturnRequest.create(
                orderId = 1L,
                orderItemId = 100L,
                type = ReturnType.EXCHANGE,
                reasonType = ReturnReasonType.SIZE_MISMATCH,
                shippingFeeBearer = ShippingFeeBearer.BUYER,
            )

            returnRequest.reject()

            Then("REJECTED 상태로 변경된다") {
                returnRequest.status shouldBe ReturnStatus.REJECTED
            }

            Then("rejectedAt이 설정된다") {
                returnRequest.rejectedAt shouldNotBe null
            }
        }

        When("REJECTED 상태에서 다시 거절 시도") {
            val returnRequest = ReturnRequest.create(
                orderId = 1L,
                orderItemId = 100L,
                type = ReturnType.RETURN,
                reasonType = ReturnReasonType.WRONG_ITEM,
                shippingFeeBearer = ShippingFeeBearer.SELLER,
            )
            returnRequest.reject()

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    returnRequest.reject()
                }
            }
        }
    }
})
