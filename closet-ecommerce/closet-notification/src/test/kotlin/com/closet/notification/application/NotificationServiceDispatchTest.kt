package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationRepository
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZonedDateTime

class NotificationServiceDispatchTest : BehaviorSpec({
    val notificationRepository = mockk<NotificationRepository>()
    val notificationDispatcher = mockk<NotificationDispatcher>()
    val notificationService = NotificationService(notificationRepository, notificationDispatcher)

    Given("알림 발송 요청이 주어졌을 때") {
        val memberId = 1L
        val type = NotificationType.ORDER
        val channel = NotificationChannel.EMAIL
        val title = "주문 완료"
        val content = "주문이 완료되었습니다."

        When("정상적으로 발송하면") {
            val notificationSlot = slot<Notification>()
            every { notificationRepository.save(capture(notificationSlot)) } answers {
                notificationSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }
            every { notificationDispatcher.dispatch(any()) } returns true

            val result = notificationService.send(memberId, type, channel, title, content)

            Then("알림이 저장되고 디스패처를 통해 발송된다") {
                result.memberId shouldBe memberId
                result.type shouldBe type
                result.channel shouldBe channel
                result.title shouldBe title
                result.content shouldBe content
                result.isRead shouldBe false
                verify(exactly = 1) { notificationDispatcher.dispatch(any()) }
            }
        }

        When("디스패처 발송이 실패해도") {
            val notificationSlot = slot<Notification>()
            every { notificationRepository.save(capture(notificationSlot)) } answers {
                notificationSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }
            every { notificationDispatcher.dispatch(any()) } returns false

            val result = notificationService.send(memberId, type, channel, title, content)

            Then("알림은 정상적으로 저장된다 (발송 실패는 별도 재시도로 처리)") {
                result.memberId shouldBe memberId
                result.channel shouldBe channel
            }
        }
    }
})
