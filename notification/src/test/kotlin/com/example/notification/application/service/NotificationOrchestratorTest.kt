package com.example.notification.application.service

import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.EffectiveRule
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationPayload
import com.example.notification.domain.model.NotificationRecipient
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify

class NotificationOrchestratorTest : BehaviorSpec({

    Given("수신자 2명이 존재하는 경우") {
        val recipientResolver = mockk<NotificationRecipientResolver>()
        val ruleResolver = mockk<NotificationRuleResolver>()
        val dispatcher = mockk<NotificationDispatcher>()

        val recipients = listOf(
            NotificationRecipient(userId = 10L, email = "owner1@store.com", pushToken = "token-1", isStoreOwner = true),
            NotificationRecipient(userId = 20L, email = "owner2@store.com", pushToken = "token-2", isStoreOwner = true),
        )

        val effectiveRules = listOf(
            EffectiveRule(channel = NotificationChannel.EMAIL, enabled = true),
            EffectiveRule(channel = NotificationChannel.PUSH, enabled = true),
            EffectiveRule(channel = NotificationChannel.IN_APP, enabled = true),
            EffectiveRule(channel = NotificationChannel.SMS, enabled = false),
        )

        every { recipientResolver.resolve(any()) } returns recipients
        every { ruleResolver.resolve(any(), any(), any(), any(), any()) } returns effectiveRules
        every { dispatcher.deliver(any(), any(), any()) } just Runs

        val orchestrator = NotificationOrchestrator(recipientResolver, ruleResolver, dispatcher)

        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(100L, "홍길동", "맛있는 빵집", 35000L, 3),
        )

        When("handle 호출") {
            orchestrator.handle(event)

            Then("수신자 2명 x 활성 채널 3개 = 6번 deliver가 호출된다") {
                verify(exactly = 6) { dispatcher.deliver(any(), any(), any()) }
            }
        }
    }

    Given("수신자가 없는 경우") {
        val recipientResolver = mockk<NotificationRecipientResolver>()
        val ruleResolver = mockk<NotificationRuleResolver>()
        val dispatcher = mockk<NotificationDispatcher>()

        every { recipientResolver.resolve(any()) } returns emptyList()

        val orchestrator = NotificationOrchestrator(recipientResolver, ruleResolver, dispatcher)

        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(100L, "홍길동", "맛있는 빵집", 35000L, 1),
        )

        When("handle 호출") {
            orchestrator.handle(event)

            Then("deliver가 호출되지 않는다") {
                verify(exactly = 0) { dispatcher.deliver(any(), any(), any()) }
            }
        }
    }

    Given("Store Policy로 EMAIL이 비활성화된 경우") {
        val recipientResolver = mockk<NotificationRecipientResolver>()
        val ruleResolver = mockk<NotificationRuleResolver>()
        val dispatcher = mockk<NotificationDispatcher>()

        val recipients = listOf(
            NotificationRecipient(userId = 10L, email = "owner@store.com", pushToken = "token-1", isStoreOwner = true),
        )

        val effectiveRules = listOf(
            EffectiveRule(channel = NotificationChannel.EMAIL, enabled = false),
            EffectiveRule(channel = NotificationChannel.PUSH, enabled = true),
            EffectiveRule(channel = NotificationChannel.IN_APP, enabled = true),
            EffectiveRule(channel = NotificationChannel.SMS, enabled = false),
        )

        every { recipientResolver.resolve(any()) } returns recipients
        every { ruleResolver.resolve(any(), any(), any(), any(), any()) } returns effectiveRules
        every { dispatcher.deliver(any(), any(), any()) } just Runs

        val orchestrator = NotificationOrchestrator(recipientResolver, ruleResolver, dispatcher)

        val event = NotificationEvent(
            triggerType = NotificationTriggerType.ORDER_PLACED,
            storeId = 1L,
            orderId = 100L,
            payload = NotificationPayload.OrderPlaced(100L, "홍길동", "맛있는 빵집", 35000L, 1),
        )

        When("handle 호출") {
            orchestrator.handle(event)

            Then("PUSH + IN_APP = 2번 deliver가 호출된다") {
                verify(exactly = 2) { dispatcher.deliver(any(), any(), any()) }
            }
        }
    }
})
