package com.closet.notification

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.notification.application.RestockSubscriptionService
import com.closet.notification.domain.Notification
import com.closet.notification.domain.RestockSubscription
import com.closet.notification.domain.repository.NotificationRepository
import com.closet.notification.domain.repository.RestockSubscriptionRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.time.LocalDateTime

class RestockSubscriptionServiceTest : BehaviorSpec({
    val restockSubscriptionRepository = mockk<RestockSubscriptionRepository>()
    val notificationRepository = mockk<NotificationRepository>()
    val restockService = RestockSubscriptionService(restockSubscriptionRepository, notificationRepository)

    Given("재입고 알림 구독 요청이 주어졌을 때") {
        val memberId = 1L
        val productOptionId = 100L

        When("아직 구독하지 않았으면") {
            every {
                restockSubscriptionRepository.existsByMemberIdAndProductOptionIdAndDeletedAtIsNull(memberId, productOptionId)
            } returns false

            val subscriptionSlot = slot<RestockSubscription>()
            every { restockSubscriptionRepository.save(capture(subscriptionSlot)) } answers {
                subscriptionSlot.captured.apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            val result = restockService.subscribe(memberId, productOptionId)

            Then("구독이 정상적으로 생성된다") {
                result.memberId shouldBe memberId
                result.productOptionId shouldBe productOptionId
                result.isNotified shouldBe false
            }
        }

        When("이미 구독 중이면") {
            every {
                restockSubscriptionRepository.existsByMemberIdAndProductOptionIdAndDeletedAtIsNull(memberId, productOptionId)
            } returns true

            Then("DUPLICATE_ENTITY 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    restockService.subscribe(memberId, productOptionId)
                }
                exception.errorCode shouldBe ErrorCode.DUPLICATE_ENTITY
            }
        }
    }

    Given("재입고 알림 발송 요청이 주어졌을 때") {
        val productOptionId = 100L

        When("미발송 구독자가 2명이면") {
            val subscriptions = listOf(
                RestockSubscription.create(memberId = 1L, productOptionId = productOptionId).apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                },
                RestockSubscription.create(memberId = 2L, productOptionId = productOptionId).apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                },
            )

            every {
                restockSubscriptionRepository.findByProductOptionIdAndIsNotifiedFalseAndDeletedAtIsNull(productOptionId)
            } returns subscriptions

            every { notificationRepository.save(any<Notification>()) } answers {
                (firstArg() as Notification).apply {
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            val count = restockService.notifyRestock(productOptionId)

            Then("2명 모두에게 알림이 발송된다") {
                count shouldBe 2
                subscriptions.forEach { it.isNotified shouldBe true }
            }
        }
    }

    Given("재입고 구독 취소 요청이 주어졌을 때") {
        val memberId = 1L
        val productOptionId = 100L

        When("구독이 존재하지 않으면") {
            every {
                restockSubscriptionRepository.findByMemberIdAndProductOptionIdAndDeletedAtIsNull(memberId, 999L)
            } returns null

            Then("ENTITY_NOT_FOUND 예외가 발생한다") {
                val exception = shouldThrow<BusinessException> {
                    restockService.unsubscribe(memberId, 999L)
                }
                exception.errorCode shouldBe ErrorCode.ENTITY_NOT_FOUND
            }
        }
    }
})
