package com.closet.notification.infrastructure.sender

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

class NotificationSenderTest : BehaviorSpec({

    Given("EmailNotificationSender가 주어졌을 때") {
        val sender = EmailNotificationSender()

        When("EMAIL 채널을 지원하는지 확인하면") {
            Then("true를 반환한다") {
                sender.supports(NotificationChannel.EMAIL) shouldBe true
            }
        }

        When("SMS 채널을 지원하는지 확인하면") {
            Then("false를 반환한다") {
                sender.supports(NotificationChannel.SMS) shouldBe false
            }
        }

        When("EMAIL 알림을 발송하면") {
            val notification = createNotification(NotificationChannel.EMAIL)
            val result = sender.send(notification)

            Then("성공을 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("SmsNotificationSender가 주어졌을 때") {
        val sender = SmsNotificationSender()

        When("SMS 채널을 지원하는지 확인하면") {
            Then("true를 반환한다") {
                sender.supports(NotificationChannel.SMS) shouldBe true
            }
        }

        When("EMAIL 채널을 지원하는지 확인하면") {
            Then("false를 반환한다") {
                sender.supports(NotificationChannel.EMAIL) shouldBe false
            }
        }

        When("SMS 알림을 발송하면") {
            val notification = createNotification(NotificationChannel.SMS)
            val result = sender.send(notification)

            Then("성공을 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("PushNotificationSender가 주어졌을 때") {
        val sender = PushNotificationSender()

        When("PUSH 채널을 지원하는지 확인하면") {
            Then("true를 반환한다") {
                sender.supports(NotificationChannel.PUSH) shouldBe true
            }
        }

        When("EMAIL 채널을 지원하는지 확인하면") {
            Then("false를 반환한다") {
                sender.supports(NotificationChannel.EMAIL) shouldBe false
            }
        }

        When("PUSH 알림을 발송하면") {
            val notification = createNotification(NotificationChannel.PUSH)
            val result = sender.send(notification)

            Then("성공을 반환한다") {
                result shouldBe true
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
