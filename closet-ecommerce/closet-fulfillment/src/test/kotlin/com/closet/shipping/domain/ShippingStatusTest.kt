package com.closet.shipping.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ShippingStatusTest : BehaviorSpec({

    Given("ShippingStatus 상태 전이") {

        When("READY -> IN_TRANSIT") {
            Then("전이 가능") {
                ShippingStatus.READY.canTransitionTo(ShippingStatus.IN_TRANSIT) shouldBe true
            }
        }

        When("IN_TRANSIT -> DELIVERED") {
            Then("전이 가능") {
                ShippingStatus.IN_TRANSIT.canTransitionTo(ShippingStatus.DELIVERED) shouldBe true
            }
        }

        When("DELIVERED -> READY") {
            Then("전이 불가") {
                ShippingStatus.DELIVERED.canTransitionTo(ShippingStatus.READY) shouldBe false
            }
        }

        When("DELIVERED -> IN_TRANSIT") {
            Then("전이 불가") {
                ShippingStatus.DELIVERED.canTransitionTo(ShippingStatus.IN_TRANSIT) shouldBe false
            }
        }

        When("READY -> DELIVERED 직접 전이") {
            Then("전이 불가") {
                ShippingStatus.READY.canTransitionTo(ShippingStatus.DELIVERED) shouldBe false
            }
        }
    }

    Given("ShippingStatus validateTransitionTo") {

        When("유효하지 않은 전이") {
            Then("IllegalArgumentException 발생") {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        ShippingStatus.DELIVERED.validateTransitionTo(ShippingStatus.READY)
                    }
                exception.message shouldContain "배송 상태를 DELIVERED에서 READY(으)로 변경할 수 없습니다"
            }
        }
    }

    Given("ShippingStatus.fromCarrierStatus 매핑") {

        When("ACCEPTED") {
            Then("READY로 매핑") {
                ShippingStatus.fromCarrierStatus("ACCEPTED") shouldBe ShippingStatus.READY
            }
        }

        When("IN_TRANSIT") {
            Then("IN_TRANSIT로 매핑") {
                ShippingStatus.fromCarrierStatus("IN_TRANSIT") shouldBe ShippingStatus.IN_TRANSIT
            }
        }

        When("OUT_FOR_DELIVERY") {
            Then("IN_TRANSIT로 매핑") {
                ShippingStatus.fromCarrierStatus("OUT_FOR_DELIVERY") shouldBe ShippingStatus.IN_TRANSIT
            }
        }

        When("DELIVERED") {
            Then("DELIVERED로 매핑") {
                ShippingStatus.fromCarrierStatus("DELIVERED") shouldBe ShippingStatus.DELIVERED
            }
        }

        When("알 수 없는 상태") {
            Then("IllegalArgumentException 발생") {
                val exception =
                    shouldThrow<IllegalArgumentException> {
                        ShippingStatus.fromCarrierStatus("UNKNOWN")
                    }
                exception.message shouldContain "Unknown carrier status: UNKNOWN"
            }
        }
    }
})
