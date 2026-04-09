package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.sender.NotificationSender
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.ZonedDateTime

class NotificationDispatcherPreferenceTest : BehaviorSpec({

    Given("회원이 EMAIL 채널을 비활성화한 상태에서") {
        val emailSender = mockk<NotificationSender>()
        every { emailSender.supports(NotificationChannel.EMAIL) } returns true

        val preferenceService = mockk<NotificationPreferenceService>()
        every { preferenceService.isChannelEnabled(1L, NotificationChannel.EMAIL) } returns false
        every { preferenceService.isDndTime(1L, any()) } returns false

        val dispatcher = NotificationDispatcher(listOf(emailSender), preferenceService)

        When("EMAIL 알림을 디스패치하면") {
            val notification = createTestNotification(1L, NotificationChannel.EMAIL)
            val result = dispatcher.dispatch(notification)

            Then("발송이 차단되고 false를 반환한다") {
                result shouldBe false
                verify(exactly = 0) { emailSender.send(any()) }
            }
        }
    }

    Given("회원이 모든 채널을 활성화한 상태에서") {
        val pushSender = mockk<NotificationSender>()
        every { pushSender.supports(NotificationChannel.PUSH) } returns true
        every { pushSender.send(any()) } returns true

        val preferenceService = mockk<NotificationPreferenceService>()
        every { preferenceService.isChannelEnabled(1L, NotificationChannel.PUSH) } returns true
        every { preferenceService.isDndTime(1L, any()) } returns false

        val dispatcher = NotificationDispatcher(listOf(pushSender), preferenceService)

        When("PUSH 알림을 디스패치하면") {
            val notification = createTestNotification(1L, NotificationChannel.PUSH)
            val result = dispatcher.dispatch(notification)

            Then("정상적으로 발송된다") {
                result shouldBe true
                verify(exactly = 1) { pushSender.send(notification) }
            }
        }
    }

    Given("DND 시간인 경우") {
        val smsSender = mockk<NotificationSender>()
        every { smsSender.supports(NotificationChannel.SMS) } returns true

        val preferenceService = mockk<NotificationPreferenceService>()
        every { preferenceService.isChannelEnabled(1L, NotificationChannel.SMS) } returns true
        every { preferenceService.isDndTime(1L, any()) } returns true

        val dispatcher = NotificationDispatcher(listOf(smsSender), preferenceService)

        When("SMS 알림을 디스패치하면") {
            val notification = createTestNotification(1L, NotificationChannel.SMS)
            val result = dispatcher.dispatch(notification)

            Then("DND로 인해 발송이 차단된다") {
                result shouldBe false
                verify(exactly = 0) { smsSender.send(any()) }
            }
        }
    }

    Given("채널이 활성화되어 있고 DND 시간이 아닌 경우") {
        val emailSender = mockk<NotificationSender>()
        every { emailSender.supports(NotificationChannel.EMAIL) } returns true
        every { emailSender.send(any()) } returns true

        val preferenceService = mockk<NotificationPreferenceService>()
        every { preferenceService.isChannelEnabled(1L, NotificationChannel.EMAIL) } returns true
        every { preferenceService.isDndTime(1L, any()) } returns false

        val dispatcher = NotificationDispatcher(listOf(emailSender), preferenceService)

        When("EMAIL 알림을 디스패치하면") {
            val notification = createTestNotification(1L, NotificationChannel.EMAIL)
            val result = dispatcher.dispatch(notification)

            Then("정상적으로 발송된다") {
                result shouldBe true
                verify(exactly = 1) { emailSender.send(notification) }
            }
        }
    }
})

private fun createTestNotification(
    memberId: Long,
    channel: NotificationChannel,
): Notification {
    return Notification.create(
        memberId = memberId,
        channel = channel,
        type = NotificationType.ORDER,
        title = "테스트 알림",
        content = "테스트 내용입니다.",
    ).apply {
        createdAt = ZonedDateTime.now()
        updatedAt = ZonedDateTime.now()
    }
}
