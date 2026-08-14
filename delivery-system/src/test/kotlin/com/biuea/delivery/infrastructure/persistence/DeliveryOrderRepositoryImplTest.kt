package com.biuea.delivery.infrastructure.persistence

import com.biuea.delivery.domain.order.DeliveryOrderStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class DeliveryOrderRepositoryImplTest : BehaviorSpec({

    Given("새로 브로드캐스트된 주문") {
        AcceptStrategyTestContext.clearOrders()
        val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder(storeId = 77L)

        Then("식별자가 부여되고 라이더 대기 상태로 조회된다") {
            val storedOrder = AcceptStrategyTestContext.findOrderBy(orderId).shouldNotBeNull()
            storedOrder.storeId shouldBe 77L
            storedOrder.currentStatus shouldBe DeliveryOrderStatus.WAITING_RIDER
            storedOrder.version shouldBe 0L
        }
    }

    Given("배차를 마친 주문") {
        AcceptStrategyTestContext.clearOrders()
        val orderId = AcceptStrategyTestContext.seedWaitingRiderOrder()
        AcceptStrategyTestContext.pessimisticLockAcceptStrategy.accept(orderId, riderId = 5L)

        Then("낙관적 락 판정 근거인 version 이 증가한다") {
            val storedOrder = AcceptStrategyTestContext.findOrderBy(orderId).shouldNotBeNull()
            storedOrder.version shouldBe 1L
            storedOrder.currentAssignedRiderId shouldBe 5L
        }
    }

    Given("존재하지 않는 주문 식별자") {
        Then("조회 결과는 null 이다") {
            AcceptStrategyTestContext.findOrderBy(888_888_888L) shouldBe null
        }
    }
})
