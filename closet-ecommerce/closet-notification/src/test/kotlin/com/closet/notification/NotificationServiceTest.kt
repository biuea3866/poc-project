package com.closet.notification

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.application.NotificationService
import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.ZonedDateTime

class NotificationServiceTest : BehaviorSpec({
    val notificationRepository = mockk<NotificationRepository>()
    val notificationService = NotificationService(notificationRepository)

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

            val result = notificationService.send(memberId, type, channel, title, content)

            Then("알림이 정상적으로 생성된다") {
                result.memberId shouldBe memberId
                result.type shouldBe type
                result.channel shouldBe channel
                result.title shouldBe title
                result.content shouldBe content
                result.isRead shouldBe false
            }
        }
    }

    Given("알림 읽음 처리 요청이 주어졌을 때") {
        val notification =
            Notification.create(
                memberId = 1L,
                channel = NotificationChannel.PUSH,
                type = NotificationType.SHIPPING,
                title = "배송 시작",
                content = "배송이 시작되었습니다.",
            ).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        When("존재하는 알림이면") {
            every { notificationRepository.findByIdAndDeletedAtIsNull(any()) } returns notification

            val result = notificationService.markAsRead(1L)

            Then("읽음 상태로 변경된다") {
                result.isRead shouldBe true
                notification.readAt shouldBe notification.readAt
            }
        }

        When("존재하지 않는 알림이면") {
            every { notificationRepository.findByIdAndDeletedAtIsNull(999L) } returns null

            Then("ENTITY_NOT_FOUND 예외가 발생한다") {
                val exception =
                    shouldThrow<BusinessException> {
                        notificationService.markAsRead(999L)
                    }
                exception.errorCode shouldBe ErrorCode.ENTITY_NOT_FOUND
            }
        }
    }

    Given("미읽음 카운트 조회 요청이 주어졌을 때") {
        val memberId = 1L

        When("미읽음 알림이 5개이면") {
            every { notificationRepository.countByMemberIdAndIsReadFalseAndDeletedAtIsNull(memberId) } returns 5L

            val result = notificationService.getUnreadCount(memberId)

            Then("카운트가 5로 반환된다") {
                result.count shouldBe 5L
            }
        }
    }
})
