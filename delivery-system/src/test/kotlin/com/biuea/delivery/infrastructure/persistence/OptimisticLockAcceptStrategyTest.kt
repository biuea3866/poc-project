package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.AcceptOutcome
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.orm.ObjectOptimisticLockingFailureException

/**
 * 낙관적 락의 약점을 드러낸다. 경합이 심할수록 "일단 쓰고 충돌나면 다시"의 재시도 비용이 폭증한다.
 */
class OptimisticLockAcceptStrategyTest : BehaviorSpec({

    val concurrentRiderCount = 100

    Given("라이더 ${concurrentRiderCount}명이 같은 주문을 동시에 수락한다") {
        AcceptStrategyTestContext.clearOrders()
        val strategy = AcceptStrategyTestContext.optimisticLockAcceptStrategy
        strategy.resetTotalRetryCount()
        val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder()

        val outcomes = AcceptStrategyTestContext.runConcurrently(concurrentRiderCount) { riderIndex ->
            strategy.accept(orderId, riderId = riderIndex.toLong() + 1)
        }
        val totalRetryCount = strategy.totalRetryCount

        Then("승자는 1명이지만 버전 충돌로 인한 재시도가 발생한다") {
            outcomes.filterIsInstance<AcceptOutcome.Accepted>() shouldHaveSize 1
            totalRetryCount shouldBeGreaterThan 0L
            val winner = outcomes.filterIsInstance<AcceptOutcome.Accepted>().first()
            println(
                "[낙관적 락] 동시 라이더 ${concurrentRiderCount}명 → 총 재시도 ${totalRetryCount}회 " +
                    "(라이더 1명당 ${"%.2f".format(totalRetryCount.toDouble() / concurrentRiderCount)}회), " +
                    "승자 재시도 ${winner.retryCount}회, " +
                    "재시도 소진 ${outcomes.filterIsInstance<AcceptOutcome.RetryExhausted>().size}명, " +
                    "이미 배차 ${outcomes.filterIsInstance<AcceptOutcome.AlreadyAssigned>().size}명",
            )
        }

        Then("재시도 소진으로 끝난 라이더도 중복 배차를 만들지 않는다") {
            val storedOrder = requireNotNull(AcceptStrategyTestContext.findOrderBy(orderId))
            storedOrder.currentAssignedRiderId shouldBe
                outcomes.filterIsInstance<AcceptOutcome.Accepted>().first().riderId
        }
    }

    Given("버전 충돌이 재시도 상한만큼 반복되는 주문") {
        val assignmentTransaction = mockk<DeliveryOrderAssignmentTransaction>()
        every { assignmentTransaction.assignWithVersionCheck(any(), any()) } throws
            ObjectOptimisticLockingFailureException("delivery_orders", 1L)
        val strategy = OptimisticLockAcceptStrategy(assignmentTransaction)

        When("라이더가 수락하면") {
            val outcome = strategy.accept(orderId = 1L, riderId = 7L)

            Then("재시도 상한에서 멈추고 재시도 소진 결과를 돌려준다") {
                outcome shouldBe AcceptOutcome.RetryExhausted(1L, OptimisticLockAcceptStrategy.MAX_ATTEMPT_COUNT)
                verify(exactly = OptimisticLockAcceptStrategy.MAX_ATTEMPT_COUNT) {
                    assignmentTransaction.assignWithVersionCheck(1L, 7L)
                }
            }
        }
    }
})
