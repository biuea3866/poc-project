package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.NotificationPayload
import com.example.notification.legacy.LegacyOrderNotificationUseCase
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class LegacyBridgeTest : BehaviorSpec({

    Given("Feature Flag ON인 경우") {
        val orchestrator = mockk<NotificationOrchestrator>()
        val featureFlags = mockk<FeatureFlagService>()
        val legacyUseCase = mockk<LegacyOrderNotificationUseCase>()

        every { featureFlags.isEnabled(NotificationTriggerType.ORDER_PLACED, 1L) } returns true
        every { orchestrator.handle(any()) } just Runs

        val bridge = NotificationLegacyBridge(orchestrator, featureFlags)
        val useCaseV2 = OrderNotificationUseCaseV2(bridge, legacyUseCase)

        val order = LegacyOrderNotificationUseCase.Order(
            id = 100L, storeId = 1L, buyerName = "홍길동",
            storeName = "맛있는 빵집", amount = 35000L, buyerPhone = "010-1234-5678",
        )

        When("notifyOrderPlaced 호출") {
            useCaseV2.notifyOrderPlaced(order)

            Then("V2 오케스트레이터가 호출된다") {
                verify(exactly = 1) { orchestrator.handle(any()) }
            }

            Then("레거시 UseCase는 호출되지 않는다") {
                verify(exactly = 0) { legacyUseCase.notifyOrderPlaced(any()) }
            }
        }
    }

    Given("Feature Flag OFF인 경우") {
        val orchestrator = mockk<NotificationOrchestrator>()
        val featureFlags = mockk<FeatureFlagService>()
        val legacyUseCase = mockk<LegacyOrderNotificationUseCase>()

        every { featureFlags.isEnabled(NotificationTriggerType.ORDER_PLACED, 1L) } returns false
        every { legacyUseCase.notifyOrderPlaced(any()) } just Runs

        val bridge = NotificationLegacyBridge(orchestrator, featureFlags)
        val useCaseV2 = OrderNotificationUseCaseV2(bridge, legacyUseCase)

        val order = LegacyOrderNotificationUseCase.Order(
            id = 100L, storeId = 1L, buyerName = "홍길동",
            storeName = "맛있는 빵집", amount = 35000L, buyerPhone = "010-1234-5678",
        )

        When("notifyOrderPlaced 호출") {
            useCaseV2.notifyOrderPlaced(order)

            Then("V2 오케스트레이터는 호출되지 않는다") {
                verify(exactly = 0) { orchestrator.handle(any()) }
            }

            Then("레거시 UseCase가 호출된다") {
                verify(exactly = 1) { legacyUseCase.notifyOrderPlaced(any()) }
            }
        }
    }
})
