package com.closet.shipping.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.ZonedDateTime

class ShipmentTest : BehaviorSpec({

    Given("Shipment 생성") {

        When("create()로 생성") {
            val shipment =
                Shipment.create(
                    orderId = 1L,
                    sellerId = 1L,
                    memberId = 1L,
                    receiverName = "홍길동",
                    receiverPhone = "010-1234-5678",
                    zipCode = "06000",
                    address = "서울시 강남구",
                    detailAddress = "101동 201호",
                )

            Then("초기 상태는 READY") {
                shipment.status shouldBe ShippingStatus.READY
            }

            Then("carrier와 trackingNumber는 null") {
                shipment.carrier shouldBe null
                shipment.trackingNumber shouldBe null
            }
        }
    }

    Given("송장 등록") {

        When("registerTracking() 호출") {
            val shipment = createShipment()
            shipment.registerTracking("CJ", "CJ1234567890")

            Then("carrier와 trackingNumber가 세팅됨") {
                shipment.carrier shouldBe "CJ"
                shipment.trackingNumber shouldBe "CJ1234567890"
            }

            Then("shippedAt이 세팅됨") {
                shipment.shippedAt shouldNotBe null
            }
        }
    }

    Given("배송 상태 전이") {

        When("READY -> IN_TRANSIT") {
            val shipment = createShipment()
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)

            Then("상태가 IN_TRANSIT로 변경") {
                shipment.status shouldBe ShippingStatus.IN_TRANSIT
            }
        }

        When("IN_TRANSIT -> DELIVERED") {
            val shipment = createShipment()
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            Then("상태가 DELIVERED로 변경") {
                shipment.status shouldBe ShippingStatus.DELIVERED
            }

            Then("deliveredAt이 세팅됨") {
                shipment.deliveredAt shouldNotBe null
            }
        }

        When("DELIVERED -> READY (잘못된 전이)") {
            val shipment = createShipment()
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            Then("IllegalArgumentException 발생") {
                shouldThrow<IllegalArgumentException> {
                    shipment.updateStatus(ShippingStatus.READY)
                }
            }
        }
    }

    Given("자동 구매확정 대상 확인") {

        When("DELIVERED 상태이고 168시간 경과") {
            val shipment = createShipment()
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            // deliveredAt을 169시간 전으로 조작
            val oldTime = ZonedDateTime.now().minusHours(169)
            val field = Shipment::class.java.getDeclaredField("deliveredAt")
            field.isAccessible = true
            field.set(shipment, oldTime)

            Then("자동 구매확정 대상") {
                shipment.isAutoConfirmEligible(ZonedDateTime.now()) shouldBe true
            }
        }

        When("DELIVERED 상태이고 168시간 미경과") {
            val shipment = createShipment()
            shipment.updateStatus(ShippingStatus.IN_TRANSIT)
            shipment.updateStatus(ShippingStatus.DELIVERED)

            Then("자동 구매확정 대상 아님") {
                shipment.isAutoConfirmEligible(ZonedDateTime.now()) shouldBe false
            }
        }
    }
})

private fun createShipment(): Shipment {
    return Shipment.create(
        orderId = 1L,
        sellerId = 1L,
        memberId = 1L,
        receiverName = "홍길동",
        receiverPhone = "010-1234-5678",
        zipCode = "06000",
        address = "서울시 강남구",
        detailAddress = "101동 201호",
    )
}
