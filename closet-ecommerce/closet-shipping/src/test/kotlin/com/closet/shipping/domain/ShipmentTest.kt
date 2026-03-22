package com.closet.shipping.domain

import com.closet.common.exception.BusinessException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ShipmentTest : BehaviorSpec({

    Given("배송 생성") {
        When("정상 생성") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )

            Then("PENDING 상태로 생성된다") {
                shipment.status shouldBe ShipmentStatus.PENDING
            }

            Then("송장 정보가 비어있다") {
                shipment.carrier shouldBe null
                shipment.trackingNumber shouldBe null
            }
        }
    }

    Given("송장 등록") {
        When("PENDING 상태에서 송장 등록") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )

            shipment.registerTracking(Carrier.CJ, "1234567890")

            Then("READY 상태로 전이된다") {
                shipment.status shouldBe ShipmentStatus.READY
            }

            Then("택배사와 송장번호가 설정된다") {
                shipment.carrier shouldBe Carrier.CJ
                shipment.trackingNumber shouldBe "1234567890"
            }
        }

        When("READY 상태에서 송장 등록 시도") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )
            shipment.registerTracking(Carrier.CJ, "1234567890")

            Then("BusinessException이 발생한다") {
                shouldThrow<BusinessException> {
                    shipment.registerTracking(Carrier.HANJIN, "9876543210")
                }
            }
        }
    }

    Given("배송 상태 변경") {
        When("PICKED_UP으로 변경 시 shippedAt이 설정된다") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )
            shipment.registerTracking(Carrier.CJ, "1234567890")
            shipment.updateStatus(ShipmentStatus.PICKED_UP)

            Then("PICKED_UP 상태") {
                shipment.status shouldBe ShipmentStatus.PICKED_UP
            }

            Then("shippedAt이 설정된다") {
                shipment.shippedAt shouldNotBe null
            }
        }
    }

    Given("배송 완료") {
        When("OUT_FOR_DELIVERY에서 배송 완료") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )
            shipment.registerTracking(Carrier.CJ, "1234567890")
            shipment.updateStatus(ShipmentStatus.PICKED_UP)
            shipment.updateStatus(ShipmentStatus.IN_TRANSIT)
            shipment.updateStatus(ShipmentStatus.OUT_FOR_DELIVERY)
            shipment.completeDelivery()

            Then("DELIVERED 상태") {
                shipment.status shouldBe ShipmentStatus.DELIVERED
            }

            Then("deliveredAt이 설정된다") {
                shipment.deliveredAt shouldNotBe null
            }
        }

        When("IN_TRANSIT에서 직접 배송 완료 시도") {
            val shipment = Shipment.create(
                orderId = 1L,
                sellerId = 10L,
                receiverName = "홍길동",
                receiverPhone = "010-1234-5678",
                address = "서울시 강남구 역삼동 123-4",
            )
            shipment.registerTracking(Carrier.CJ, "1234567890")
            shipment.updateStatus(ShipmentStatus.PICKED_UP)
            shipment.updateStatus(ShipmentStatus.IN_TRANSIT)

            Then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    shipment.completeDelivery()
                }
            }
        }
    }
})
