package com.closet.notification.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.domain.NotificationChannel
import com.closet.notification.domain.NotificationPreference
import com.closet.notification.domain.NotificationTopicSubscription
import com.closet.notification.domain.TopicType
import com.closet.notification.domain.repository.NotificationPreferenceRepository
import com.closet.notification.domain.repository.NotificationTopicSubscriptionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationPreferenceServiceTest : BehaviorSpec({
    val preferenceRepository = mockk<NotificationPreferenceRepository>()
    val topicSubscriptionRepository = mockk<NotificationTopicSubscriptionRepository>()
    val service = NotificationPreferenceService(preferenceRepository, topicSubscriptionRepository)

    Given("getOrCreatePreference - 기존 설정이 있는 경우") {
        val memberId = 1L
        val existingPreference =
            NotificationPreference.createDefault(memberId).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        When("회원 설정을 조회하면") {
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns existingPreference

            val result = service.getOrCreatePreference(memberId)

            Then("기존 설정을 반환한다") {
                result.memberId shouldBe memberId
                result.emailEnabled shouldBe true
                result.smsEnabled shouldBe true
                result.pushEnabled shouldBe true
            }
        }
    }

    Given("getOrCreatePreference - 기존 설정이 없는 경우") {
        val memberId = 2L

        When("회원 설정을 조회하면") {
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns null

            val preferenceSlot = slot<NotificationPreference>()
            every { preferenceRepository.save(capture(preferenceSlot)) } answers {
                preferenceSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val result = service.getOrCreatePreference(memberId)

            Then("기본 설정이 생성되어 반환된다") {
                result.memberId shouldBe memberId
                result.emailEnabled shouldBe true
                result.smsEnabled shouldBe true
                result.pushEnabled shouldBe true
                result.marketingEnabled shouldBe false
                result.nightEnabled shouldBe false
                verify(exactly = 1) { preferenceRepository.save(any()) }
            }
        }
    }

    Given("updatePreference - 설정 업데이트 시") {
        val memberId = 1L
        val existingPreference =
            NotificationPreference.createDefault(memberId).apply {
                createdAt = ZonedDateTime.now()
                updatedAt = ZonedDateTime.now()
            }

        When("이메일을 비활성화하면") {
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns existingPreference

            val result =
                service.updatePreference(
                    memberId = memberId,
                    emailEnabled = false,
                )

            Then("이메일만 비활성화된다") {
                result.emailEnabled shouldBe false
                result.smsEnabled shouldBe true
                result.pushEnabled shouldBe true
            }
        }

        When("마케팅 알림을 활성화하면") {
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns existingPreference

            val result =
                service.updatePreference(
                    memberId = memberId,
                    marketingEnabled = true,
                )

            Then("마케팅 알림이 활성화된다") {
                result.marketingEnabled shouldBe true
            }
        }
    }

    Given("isChannelEnabled - 채널 활성화 여부 확인 시") {
        val memberId = 1L

        When("기존 설정에서 EMAIL이 활성화 상태이면") {
            val preference =
                NotificationPreference.createDefault(memberId).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

            val result = service.isChannelEnabled(memberId, NotificationChannel.EMAIL)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("설정이 없는 회원이면") {
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(99L) } returns null

            val preferenceSlot = slot<NotificationPreference>()
            every { preferenceRepository.save(capture(preferenceSlot)) } answers {
                preferenceSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val result = service.isChannelEnabled(99L, NotificationChannel.EMAIL)

            Then("기본 설정 생성 후 true를 반환한다") {
                result shouldBe true
            }
        }
    }

    Given("isDndTime - DND 시간 확인 시") {
        val memberId = 1L

        When("야간 알림이 비허용이고 야간 시간이면") {
            val preference =
                NotificationPreference.createDefault(memberId).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

            val nightTime = ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = service.isDndTime(memberId, nightTime)

            Then("DND로 판단된다") {
                result shouldBe true
            }
        }

        When("야간 알림이 허용이면") {
            val preference =
                NotificationPreference.createDefault(memberId).apply {
                    nightEnabled = true
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every { preferenceRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns preference

            val nightTime = ZonedDateTime.of(2026, 4, 6, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val result = service.isDndTime(memberId, nightTime)

            Then("DND가 아니다") {
                result shouldBe false
            }
        }
    }

    Given("subscribe - 토픽 구독 시") {
        val memberId = 1L
        val topicType = TopicType.PRODUCT
        val topicId = 100L

        When("기존 구독이 없으면") {
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    topicType,
                    topicId,
                )
            } returns null

            val subscriptionSlot = slot<NotificationTopicSubscription>()
            every { topicSubscriptionRepository.save(capture(subscriptionSlot)) } answers {
                subscriptionSlot.captured.apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            }

            val result = service.subscribe(memberId, topicType, topicId)

            Then("새 구독이 생성된다") {
                result.memberId shouldBe memberId
                result.topicType shouldBe topicType
                result.topicId shouldBe topicId
                result.isSubscribed shouldBe true
                verify(exactly = 1) { topicSubscriptionRepository.save(any()) }
            }
        }

        When("이미 활성 구독이 있으면") {
            val existing =
                NotificationTopicSubscription.create(memberId, topicType, topicId).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    topicType,
                    topicId,
                )
            } returns existing

            Then("DUPLICATE_ENTITY 예외가 발생한다") {
                val exception =
                    shouldThrow<BusinessException> {
                        service.subscribe(memberId, topicType, topicId)
                    }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_ENTITY
            }
        }

        When("비활성 구독이 있으면 재활성화한다") {
            val inactiveSub =
                NotificationTopicSubscription.create(memberId, topicType, topicId).apply {
                    unsubscribe()
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    topicType,
                    topicId,
                )
            } returns inactiveSub

            val result = service.subscribe(memberId, topicType, topicId)

            Then("구독이 재활성화된다") {
                result.isSubscribed shouldBe true
                result.unsubscribedAt shouldBe null
            }
        }
    }

    Given("unsubscribe - 토픽 구독 해제 시") {
        val memberId = 1L
        val topicType = TopicType.PRODUCT
        val topicId = 100L

        When("활성 구독이 있으면") {
            val existing =
                NotificationTopicSubscription.create(memberId, topicType, topicId).apply {
                    createdAt = ZonedDateTime.now()
                    updatedAt = ZonedDateTime.now()
                }
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    topicType,
                    topicId,
                )
            } returns existing

            service.unsubscribe(memberId, topicType, topicId)

            Then("구독이 비활성화된다") {
                existing.isSubscribed shouldBe false
                existing.unsubscribedAt shouldNotBe null
            }
        }

        When("구독이 없으면") {
            every {
                topicSubscriptionRepository.findByMemberIdAndTopicTypeAndTopicIdAndDeletedAtIsNull(
                    memberId,
                    topicType,
                    topicId,
                )
            } returns null

            Then("ENTITY_NOT_FOUND 예외가 발생한다") {
                val exception =
                    shouldThrow<BusinessException> {
                        service.unsubscribe(memberId, topicType, topicId)
                    }
                exception.errorCode shouldBe ErrorCode.ENTITY_NOT_FOUND
            }
        }
    }

    Given("getSubscriptions - 구독 목록 조회 시") {
        val memberId = 1L

        When("구독이 2건 있으면") {
            val subscriptions =
                listOf(
                    NotificationTopicSubscription.create(memberId, TopicType.PRODUCT, 100L).apply {
                        createdAt = ZonedDateTime.now()
                        updatedAt = ZonedDateTime.now()
                    },
                    NotificationTopicSubscription.create(memberId, TopicType.BRAND, 50L).apply {
                        createdAt = ZonedDateTime.now()
                        updatedAt = ZonedDateTime.now()
                    },
                )
            every { topicSubscriptionRepository.findByMemberIdAndDeletedAtIsNull(memberId) } returns subscriptions

            val result = service.getSubscriptions(memberId)

            Then("2건의 구독이 반환된다") {
                result.size shouldBe 2
                result[0].topicType shouldBe TopicType.PRODUCT
                result[1].topicType shouldBe TopicType.BRAND
            }
        }
    }
})
