package com.biuea.delivery.domain.order

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

/**
 * 단일 승자 보장의 마지막 방어선은 엔티티다. 락(비관적·낙관적·분산)이 뚫려 두 스레드가 동시에 들어와도
 * WAITING_RIDER 가 아닌 주문은 배차를 거부해야 중복 배차가 발생하지 않는다.
 */
class DeliveryOrderTest : BehaviorSpec({

    Given("라이더 배정을 기다리는 주문(WAITING_RIDER)") {
        When("라이더가 수락하면") {
            val order = DeliveryOrder.waitingRider(storeId = 100L)
            val acceptStartedAt = ZonedDateTime.now()
            order.assignTo(riderId = 7L)
            val acceptFinishedAt = ZonedDateTime.now()

            Then("ASSIGNED 로 전이되고 수락한 라이더가 기록된다") {
                order.currentStatus shouldBe DeliveryOrderStatus.ASSIGNED
                order.currentAssignedRiderId shouldBe 7L
            }

            Then("배차 시각이 수락 구간 안의 시각으로 기록된다") {
                val assignedAt = order.currentAssignedAt.shouldNotBeNull()
                assignedAt.isBefore(acceptStartedAt) shouldBe false
                assignedAt.isAfter(acceptFinishedAt) shouldBe false
            }
        }
    }

    Given("이미 다른 라이더에게 배차된 주문(ASSIGNED)") {
        val alreadyAssignedOrder = DeliveryOrder.reconstitute(
            id = 1L,
            storeId = 100L,
            status = DeliveryOrderStatus.ASSIGNED,
            assignedRiderId = 7L,
            version = 1L,
            createdAt = ZonedDateTime.now(),
            assignedAt = ZonedDateTime.now(),
        )

        When("다른 라이더가 수락하면") {
            val exception = shouldThrow<DeliveryOrderNotAssignableException> {
                alreadyAssignedOrder.assignTo(riderId = 8L)
            }

            Then("재배차를 거부하고 기존 배차를 유지한다") {
                alreadyAssignedOrder.currentAssignedRiderId shouldBe 7L
                alreadyAssignedOrder.currentStatus shouldBe DeliveryOrderStatus.ASSIGNED
            }

            Then("예외가 이미 배차된 라이더를 담아 패자에게 사유를 알린다") {
                exception.orderId shouldBe 1L
                exception.currentStatus shouldBe DeliveryOrderStatus.ASSIGNED
                exception.assignedRiderId shouldBe 7L
            }
        }
    }

    Given("취소된 주문(CANCELLED)") {
        val cancelledOrder = DeliveryOrder.reconstitute(
            id = 2L,
            storeId = 100L,
            status = DeliveryOrderStatus.CANCELLED,
            assignedRiderId = null,
            version = 3L,
            createdAt = ZonedDateTime.now(),
            assignedAt = null,
        )

        When("라이더가 수락하면") {
            val exception = shouldThrow<DeliveryOrderNotAssignableException> {
                cancelledOrder.assignTo(riderId = 9L)
            }

            Then("수락을 거부하고 배차 라이더가 남지 않는다") {
                cancelledOrder.currentAssignedRiderId shouldBe null
                cancelledOrder.currentAssignedAt shouldBe null
                exception.currentStatus shouldBe DeliveryOrderStatus.CANCELLED
                exception.assignedRiderId shouldBe null
            }
        }
    }

    Given("아직 조리 중이라 브로드캐스트되지 않은 주문(COOKING)") {
        val cookingOrder = DeliveryOrder.reconstitute(
            id = 3L,
            storeId = 100L,
            status = DeliveryOrderStatus.COOKING,
            assignedRiderId = null,
            version = 0L,
            createdAt = ZonedDateTime.now(),
            assignedAt = null,
        )

        When("라이더가 수락하면") {
            Then("수락을 거부한다") {
                shouldThrow<DeliveryOrderNotAssignableException> { cookingOrder.assignTo(riderId = 9L) }
            }
        }
    }

    Given("신규 생성된 주문") {
        val newOrder = DeliveryOrder.waitingRider(storeId = 100L)

        Then("라이더 배정 대기 상태로 시작하고 배차 정보가 비어 있다") {
            newOrder.currentStatus shouldBe DeliveryOrderStatus.WAITING_RIDER
            newOrder.currentAssignedRiderId shouldBe null
            newOrder.currentAssignedAt shouldBe null
            newOrder.id shouldBe null
        }
    }
})
