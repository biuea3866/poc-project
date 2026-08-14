package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryOrderStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * 전략 4종(비관적 락·낙관적 락·Redis 스핀 분산락·Redisson 분산락)이 지켜야 하는 동일한 계약.
 * 구현이 달라도 "동시에 눌러도 승자는 1명" 이라는 결과는 같아야 한다.
 */
class AcceptStrategyContractTest : BehaviorSpec({

    val concurrentRiderCount = 100
    val independentOrderCount = 50

    AcceptStrategyTestContext.strategies().forEach { (strategyName, strategy) ->

        Given("$strategyName — 라이더 ${concurrentRiderCount}명이 같은 주문을 동시에 수락한다") {
            AcceptStrategyTestContext.clearOrders()
            val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder()

            val outcomes = AcceptStrategyTestContext.runConcurrently(concurrentRiderCount) { riderIndex ->
                strategy.accept(orderId, riderId = riderIndex.toLong() + 1)
            }
            val acceptedOutcomes = outcomes.filterIsInstance<AcceptOutcome.Accepted>()

            Then("수락에 성공한 라이더는 정확히 1명이다") {
                acceptedOutcomes shouldHaveSize 1
            }

            Then("나머지 ${concurrentRiderCount - 1}명은 실패 결과를 받는다") {
                outcomes.count { it !is AcceptOutcome.Accepted } shouldBe concurrentRiderCount - 1
            }

            Then("DB 최종 상태의 배차 라이더가 성공한 라이더와 일치한다") {
                val storedOrder = AcceptStrategyTestContext.findOrderBy(orderId).shouldNotBeNull()
                storedOrder.currentStatus shouldBe DeliveryOrderStatus.ASSIGNED
                storedOrder.currentAssignedRiderId shouldBe acceptedOutcomes.first().riderId
                storedOrder.currentAssignedAt.shouldNotBeNull()
            }

            Then("배차 실패 사유는 실제 승자 라이더를 가리킨다") {
                val winnerRiderId = acceptedOutcomes.first().riderId
                outcomes.filterIsInstance<AcceptOutcome.AlreadyAssigned>()
                    .forEach { it.assignedRiderId shouldBe winnerRiderId }
            }
        }

        Given("$strategyName — 서로 다른 주문 ${independentOrderCount}건을 동시에 수락한다") {
            AcceptStrategyTestContext.clearOrders()
            val orderIds = AcceptStrategyTestContext.seedWaitingRiderOrders(independentOrderCount)

            val outcomes = AcceptStrategyTestContext.runConcurrently(independentOrderCount) { orderIndex ->
                strategy.accept(orderIds[orderIndex], riderId = orderIndex.toLong() + 1)
            }

            Then("주문이 다르면 불필요한 직렬화 없이 모두 성공한다") {
                outcomes.filterIsInstance<AcceptOutcome.Accepted>() shouldHaveSize independentOrderCount
            }

            Then("각 주문은 자기를 수락한 라이더에게 배차된다") {
                orderIds.forEachIndexed { orderIndex, orderId ->
                    val storedOrder = AcceptStrategyTestContext.findOrderBy(orderId).shouldNotBeNull()
                    storedOrder.currentAssignedRiderId shouldBe orderIndex.toLong() + 1
                }
            }
        }

        Given("$strategyName — 존재하지 않는 주문을 수락한다") {
            AcceptStrategyTestContext.clearOrders()

            val outcome = strategy.accept(orderId = 999_999_999L, riderId = 1L)

            Then("주문 없음 결과를 돌려주고 예외를 흘리지 않는다") {
                outcome shouldBe AcceptOutcome.OrderNotFound(999_999_999L)
            }
        }
    }
})
