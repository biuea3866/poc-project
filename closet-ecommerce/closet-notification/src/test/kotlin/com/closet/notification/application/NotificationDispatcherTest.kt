package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.sender.NotificationSender
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class NotificationDispatcherTest : BehaviorSpec({

    Given("NotificationDispatcher에 EMAIL, SMS, PUSH 채널 Sender가 등록되어 있을 때") {
        val emailSender = mockk<NotificationSender>()
        val smsSender = mockk<NotificationSender>()
        val pushSender = mockk<NotificationSender>()
        val preferenceService = mockk<NotificationPreferenceService>()

        every { emailSender.supports(NotificationChannel.EMAIL) } returns true
        every { emailSender.supports(NotificationChannel.SMS) } returns false
        every { emailSender.supports(NotificationChannel.PUSH) } returns false

        every { smsSender.supports(NotificationChannel.EMAIL) } returns false
        every { smsSender.supports(NotificationChannel.SMS) } returns true
        every { smsSender.supports(NotificationChannel.PUSH) } returns false

        every { pushSender.supports(NotificationChannel.EMAIL) } returns false
        every { pushSender.supports(NotificationChannel.SMS) } returns false
        every { pushSender.supports(NotificationChannel.PUSH) } returns true

        // 기본 설정: 모든 채널 활성, DND 아님
        every { preferenceService.isChannelEnabled(any(), any()) } returns true
        every { preferenceService.isDndTime(any(), any()) } returns false

        val dispatcher = NotificationDispatcher(listOf(emailSender, smsSender, pushSender), preferenceService)

        When("EMAIL 채널 알림을 디스패치하면") {
            val notification = createNotification(NotificationChannel.EMAIL)
            every { emailSender.send(notification) } returns true

            val result = dispatcher.dispatch(notification)

            Then("EmailSender가 호출되고 성공을 반환한다") {
                result shouldBe true
                verify(exactly = 1) { emailSender.send(notification) }
                verify(exactly = 0) { smsSender.send(any()) }
                verify(exactly = 0) { pushSender.send(any()) }
            }
        }

        When("SMS 채널 알림을 디스패치하면") {
            val notification = createNotification(NotificationChannel.SMS)
            every { smsSender.send(notification) } returns true

            val result = dispatcher.dispatch(notification)

            Then("SmsSender가 호출되고 성공을 반환한다") {
                result shouldBe true
                verify(exactly = 1) { smsSender.send(notification) }
            }
        }

        When("PUSH 채널 알림을 디스패치하면") {
            val notification = createNotification(NotificationChannel.PUSH)
            every { pushSender.send(notification) } returns true

            val result = dispatcher.dispatch(notification)

            Then("PushSender가 호출되고 성공을 반환한다") {
                result shouldBe true
                verify(exactly = 1) { pushSender.send(notification) }
            }
        }

        When("Sender에서 발송 실패하면") {
            val notification = createNotification(NotificationChannel.EMAIL)
            every { emailSender.send(notification) } returns false

            val result = dispatcher.dispatch(notification)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }

    Given("지원하지 않는 채널 Sender가 없을 때") {
        val emailSender = mockk<NotificationSender>()
        val preferenceService = mockk<NotificationPreferenceService>()
        every { emailSender.supports(any()) } returns false
        every { preferenceService.isChannelEnabled(any(), any()) } returns true
        every { preferenceService.isDndTime(any(), any()) } returns false

        val dispatcher = NotificationDispatcher(listOf(emailSender), preferenceService)

        When("PUSH 채널 알림을 디스패치하면") {
            val notification = createNotification(NotificationChannel.PUSH)

            Then("IllegalStateException이 발생한다") {
                shouldThrow<IllegalStateException> {
                    dispatcher.dispatch(notification)
                }
            }
        }
    }
})

private fun createNotification(channel: NotificationChannel): Notification {
    return Notification.create(
        memberId = 1L,
        channel = channel,
        type = NotificationType.ORDER,
        title = "테스트 알림",
        content = "테스트 내용입니다.",
    ).apply {
        createdAt = ZonedDateTime.now()
        updatedAt = ZonedDateTime.now()
    }
}
