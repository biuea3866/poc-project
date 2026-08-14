package com.biuea.delivery.application

import com.biuea.delivery.domain.order.AcceptOutcome
import com.biuea.delivery.domain.order.DeliveryAcceptStrategy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class AcceptDeliveryUseCaseTest : BehaviorSpec({

    Given("라이더의 배달 수락 요청") {
        val deliveryAcceptStrategy = mockk<DeliveryAcceptStrategy>()
        val acceptDeliveryUseCase = AcceptDeliveryUseCase(deliveryAcceptStrategy)

        When("수락에 성공하면") {
            every { deliveryAcceptStrategy.accept(1L, 7L) } returns AcceptOutcome.Accepted(1L, 7L, retryCount = 0)
            val outcome = acceptDeliveryUseCase.execute(AcceptDeliveryCommand(orderId = 1L, riderId = 7L))

            Then("동시성 제어 전략의 결과를 그대로 돌려준다") {
                outcome shouldBe AcceptOutcome.Accepted(1L, 7L, retryCount = 0)
                verify(exactly = 1) { deliveryAcceptStrategy.accept(1L, 7L) }
            }
        }

        When("이미 다른 라이더가 가져간 주문이면") {
            every { deliveryAcceptStrategy.accept(2L, 8L) } returns AcceptOutcome.AlreadyAssigned(2L, 9L)
            val outcome = acceptDeliveryUseCase.execute(AcceptDeliveryCommand(orderId = 2L, riderId = 8L))

            Then("예외 대신 실패 결과를 돌려준다") {
                outcome shouldBe AcceptOutcome.AlreadyAssigned(2L, 9L)
            }
        }
    }
})
