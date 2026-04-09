package com.closet.notification.application

import com.closet.notification.consumer.event.InventoryEvent
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.presentation.dto.NotificationResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class NotificationFacadeTest : BehaviorSpec({
    val notificationService = mockk<NotificationService>(relaxed = true)
    val notificationTemplateService = mockk<NotificationTemplateService>(relaxed = true)
    val facade = NotificationFacade(notificationService, notificationTemplateService)

    Given("재입고 알림 이벤트가 주어졌을 때 - 템플릿 없음") {
        When("활성화된 템플릿이 없으면 기본 메시지로 발송한다") {
            Then("각 회원에게 기본 메시지로 PUSH 알림이 발송된다") {
                clearMocks(notificationService, notificationTemplateService)

                every {
                    notificationTemplateService.findActiveTemplate(
                        NotificationType.RESTOCK,
                        NotificationChannel.PUSH,
                    )
                } returns null

                every { notificationService.send(any(), any(), any(), any(), any()) } returns createResponse()

                val event =
                    InventoryEvent(
                        eventType = "RESTOCK_NOTIFICATION",
                        productOptionId = 100L,
                        sku = "SKU-001",
                        availableQuantity = 10,
                        memberIds = listOf(1L, 2L),
                    )

                facade.handleRestockNotification(event)

                verify(exactly = 2) {
                    notificationService.send(
                        any(),
                        NotificationType.RESTOCK,
                        NotificationChannel.PUSH,
                        any(),
                        any(),
                    )
                }
            }
        }
    }

    Given("재입고 알림 이벤트가 주어졌을 때 - 대상 없음") {
        When("memberIds가 비어 있으면") {
            Then("알림 발송을 하지 않는다") {
                clearMocks(notificationService, notificationTemplateService)

                val emptyEvent =
                    InventoryEvent(
                        eventType = "RESTOCK_NOTIFICATION",
                        productOptionId = 100L,
                        sku = "SKU-002",
                        availableQuantity = 5,
                        memberIds = emptyList(),
                    )

                facade.handleRestockNotification(emptyEvent)

                verify(exactly = 0) {
                    notificationService.send(any(), any(), any(), any(), any())
                }
            }
        }
    }

    Given("템플릿 변수 치환 테스트") {
        When("{{sku}}와 {{availableQuantity}} 변수가 포함된 템플릿이면") {
            Then("변수가 정상적으로 치환된다") {
                val templateContent = "구독하신 상품({{sku}})이 재입고되었습니다. 현재 {{availableQuantity}}개 남았습니다."
                val variables =
                    mapOf(
                        "sku" to "SKU-001",
                        "availableQuantity" to "10",
                    )

                var result = templateContent
                variables.forEach { (key, value) ->
                    result = result.replace("{{$key}}", value)
                }

                result shouldBe "구독하신 상품(SKU-001)이 재입고되었습니다. 현재 10개 남았습니다."
            }
        }
    }
})

private fun createResponse(): NotificationResponse {
    return NotificationResponse(
        id = 1L,
        memberId = 1L,
        channel = NotificationChannel.PUSH,
        type = NotificationType.RESTOCK,
        title = "재입고 알림",
        content = "구독하신 상품이 재입고되었습니다.",
        isRead = false,
        sentAt = ZonedDateTime.now(),
        readAt = null,
        createdAt = ZonedDateTime.now(),
    )
}
