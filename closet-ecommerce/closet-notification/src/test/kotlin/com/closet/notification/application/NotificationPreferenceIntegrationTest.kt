package com.closet.notification.application

import com.closet.notification.domain.Notification
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationPreference
import com.closet.notification.domain.NotificationType
import com.closet.notification.domain.repository.NotificationPreferenceRepository
import com.closet.notification.domain.sender.NotificationSender
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 통합 시나리오 테스트: 알림 수신 설정 opt-out 시 알림이 발송되지 않는 전체 흐름 검증.
 *
 * NotificationPreferenceService -> NotificationDispatcher -> NotificationSender 전 구간을
 * mock 기반으로 시뮬레이션한다.
 */
class NotificationPreferenceIntegrationTest : BehaviorSpec({

    Given("회원이 EMAIL 채널을 opt-out 한 상태에서 전체 발송 흐름 시") {
        val memberId = 1L

        // 1) preference setup: EMAIL 비활성화
        val preference =
            NotificationPreference.createDefault(memberId).apply {
                emailEnabled = false
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        val preferenceRepository = mockk<NotificationPreferenceRepository>()
        every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

        val topicSubscriptionRepository =
            mockk<com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository>(relaxed = true)

        val preferenceService = NotificationPreferenceService(preferenceRepository, topicSubscriptionRepository)

        // 2) sender setup
        val emailSender = mockk<NotificationSender>()
        every { emailSender.supports(NotificationChannel.EMAIL) } returns true

        // 3) dispatcher with real preferenceService
        val dispatcher = NotificationDispatcher(listOf(emailSender), preferenceService)

        When("EMAIL 알림을 발송하면") {
            val notification =
                Notification.create(
                    memberId = memberId,
                    channel = NotificationChannel.EMAIL,
                    type = NotificationType.MARKETING,
                    title = "마케팅 이벤트",
                    content = "놓치지 마세요!",
                ).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }

            val result = dispatcher.dispatch(notification)

            Then("EMAIL Sender가 호출되지 않고 false가 반환된다") {
                result shouldBe false
                verify(exactly = 0) { emailSender.send(any()) }
            }
        }
    }

    Given("회원이 야간 알림을 비동의하고 DND 시간에 발송 시") {
        val memberId = 2L

        val preference =
            NotificationPreference.createDefault(memberId).apply {
                nightEnabled = false // 야간 알림 비동의
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        val preferenceRepository = mockk<NotificationPreferenceRepository>()
        every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

        val topicSubscriptionRepository =
            mockk<com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository>(relaxed = true)

        val preferenceService = NotificationPreferenceService(preferenceRepository, topicSubscriptionRepository)

        When("야간(23시)에 DND 여부를 확인하면") {
            val isDnd =
                preferenceService.isDndTime(
                    memberId,
                    ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, ZoneId.of("Asia/Seoul")),
                )

            Then("DND 시간으로 판단된다") {
                isDnd shouldBe true
            }
        }
    }

    Given("회원이 모든 채널 활성화 + 야간 동의 상태에서") {
        val memberId = 3L

        val preference =
            NotificationPreference.createDefault(memberId).apply {
                nightEnabled = true // 야간 알림 동의
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        val preferenceRepository = mockk<NotificationPreferenceRepository>()
        every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

        val topicSubscriptionRepository =
            mockk<com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository>(relaxed = true)

        val preferenceService = NotificationPreferenceService(preferenceRepository, topicSubscriptionRepository)

        val smsSender = mockk<NotificationSender>()
        every { smsSender.supports(NotificationChannel.SMS) } returns true
        every { smsSender.send(any()) } returns true

        val dispatcher = NotificationDispatcher(listOf(smsSender), preferenceService)

        When("SMS 알림을 발송하면") {
            val notification =
                Notification.create(
                    memberId = memberId,
                    channel = NotificationChannel.SMS,
                    type = NotificationType.SHIPPING,
                    title = "배송 시작",
                    content = "배송이 시작되었습니다.",
                ).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }

            val result = dispatcher.dispatch(notification)

            Then("정상적으로 발송된다") {
                result shouldBe true
                verify(exactly = 1) { smsSender.send(notification) }
            }
        }
    }

    Given("토픽 구독/해제 전체 흐름 시") {
        val memberId = 1L

        val preferenceRepository = mockk<NotificationPreferenceRepository>()
        val topicSubscriptionRepository =
            mockk<com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository>()

        val preferenceService = NotificationPreferenceService(preferenceRepository, topicSubscriptionRepository)

        When("상품 토픽 구독 -> 해제 -> 재구독 흐름을 실행하면") {
            // 1) 신규 구독
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    com.closet.notification.domain.TopicType.PRODUCT,
                    100L,
                )
            } returns null

            val subscriptionSlot = slot<com.closet.notification.domain.NotificationTopicSubscription>()
            every { topicSubscriptionRepository.save(capture(subscriptionSlot)) } answers {
                subscriptionSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val subscribeResult =
                preferenceService.subscribe(
                    memberId,
                    com.closet.notification.domain.TopicType.PRODUCT,
                    100L,
                )

            Then("구독이 생성된다") {
                subscribeResult.isSubscribed shouldBe true
                subscribeResult.topicId shouldBe 100L
            }
        }
    }
})
