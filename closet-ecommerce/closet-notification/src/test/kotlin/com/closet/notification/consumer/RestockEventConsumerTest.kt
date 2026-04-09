package com.closet.notification.consumer

import com.closet.notification.application.NotificationFacade
import com.closet.notification.consumer.event.InventoryEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class RestockEventConsumerTest : BehaviorSpec({
    val notificationFacade = mockk<NotificationFacade>(relaxed = true)
    val consumer = RestockEventConsumer(notificationFacade)

    Given("event.closet.inventory 토픽에서 RESTOCK_NOTIFICATION 이벤트가 수신되었을 때") {
        When("Consumer가 이벤트를 처리하면") {
            Then("Facade에 위임하여 재입고 알림을 발송한다") {
                clearMocks(notificationFacade)
                every { notificationFacade.handleRestockNotification(any()) } returns Unit

                val event =
                    InventoryEvent(
                        eventType = "RESTOCK_NOTIFICATION",
                        productOptionId = 100L,
                        sku = "SKU-001",
                        availableQuantity = 10,
                        memberIds = listOf(1L, 2L, 3L),
                    )

                consumer.handle(event)

                verify(exactly = 1) { notificationFacade.handleRestockNotification(event) }
            }
        }
    }

    Given("event.closet.inventory 토픽에서 처리하지 않는 eventType이 수신되었을 때") {
        When("Consumer가 이벤트를 처리하면") {
            Then("Facade를 호출하지 않고 무시한다") {
                clearMocks(notificationFacade)

                val event =
                    InventoryEvent(
                        eventType = "LOW_STOCK",
                        productOptionId = 100L,
                        sku = "SKU-001",
                        availableQuantity = 3,
                        memberIds = emptyList(),
                    )

                consumer.handle(event)

                verify(exactly = 0) { notificationFacade.handleRestockNotification(any()) }
            }
        }
    }
})
