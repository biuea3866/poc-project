package com.example.notification.application.service

import com.example.notification.application.port.NotificationLogWriter
import com.example.notification.domain.channel.NotificationChannelDispatcher
import com.example.notification.domain.enums.NotificationChannel
import com.example.notification.domain.enums.NotificationLogStatus
import com.example.notification.domain.enums.NotificationPriority
import com.example.notification.domain.enums.NotificationTriggerType
import com.example.notification.domain.model.EffectiveRule
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationLog
import com.example.notification.domain.model.NotificationPayload
import com.example.notification.domain.model.NotificationRecipient
import com.example.notification.domain.model.RenderedMessage
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

class NotificationDispatcherTest : BehaviorSpec({

    Given("단건 이벤트 (correlationId 없음)") {
        val messageRenderer = mockk<NotificationMessageRenderer>()
        val emailDispatcher = mockk<NotificationChannelDispatcher>()
        val logWriter = mockk<NotificationLogWriter>()

        every { emailDispatcher.supports(NotificationChannel.EMAIL) } returns true
        every { emailDispatcher.dispatch(any(), any(), any()) } just Runs
        every { messageRenderer.render(any()) } returns RenderedMessage(
            subject = "test", body = "test", pushTitle = "test",
            pushBody = "test", smsText = "test", deepLinkUrl = "/test",
        )
        every { logWriter.existsByIdempotencyKey(any()) } returns false
        every { logWriter.save(any()) } just Runs

        val dispatcher = NotificationDispatcher(messageRenderer, listOf(emailDispatcher), logWriter)

        val event = createOrderEvent(correlationId = null, correlationTotalCount = null)
        val recipient = createRecipient()
        val rule = EffectiveRule(channel = NotificationChannel.EMAIL, enabled = true, priority = NotificationPriority.HIGH)

        When("deliver 호출") {
            dispatcher.deliver(event, recipient, rule)

            Then("즉시 발송하고 SENT 로그를 저장한다") {
                val logSlot = slot<NotificationLog>()
                verify(exactly = 1) { logWriter.save(capture(logSlot)) }
                logSlot.captured.status shouldBe NotificationLogStatus.SENT
            }
        }
    }

    Given("벌크 이벤트 (correlationId + totalCount)") {
        val messageRenderer = mockk<NotificationMessageRenderer>()
        val emailDispatcher = mockk<NotificationChannelDispatcher>()
        val logWriter = mockk<NotificationLogWriter>()

        every { emailDispatcher.supports(NotificationChannel.EMAIL) } returns true
        every { emailDispatcher.dispatch(any(), any(), any()) } just Runs
        every { messageRenderer.renderCorrelatedSummary(any()) } returns RenderedMessage(
            subject = "summary", body = "summary", pushTitle = "summary",
            pushBody = "summary", smsText = "summary", deepLinkUrl = "/test",
        )
        every { logWriter.existsByIdempotencyKey(any()) } returns false
        every { logWriter.save(any()) } just Runs

        val dispatcher = NotificationDispatcher(messageRenderer, listOf(emailDispatcher), logWriter)
        val recipient = createRecipient()
        val rule = EffectiveRule(channel = NotificationChannel.EMAIL, enabled = true, priority = NotificationPriority.HIGH)

        When("totalCount 도달 전 2건 deliver") {
            val event1 = createOrderEvent(correlationId = "batch-1", correlationTotalCount = 3, orderId = 1L)
            val event2 = createOrderEvent(correlationId = "batch-1", correlationTotalCount = 3, orderId = 2L)
            dispatcher.deliver(event1, recipient, rule)
            dispatcher.deliver(event2, recipient, rule)

            Then("아직 flush 되지 않는다") {
                verify(exactly = 0) { logWriter.save(any()) }
            }
        }

        When("totalCount 도달 시 3번째 deliver") {
            val event3 = createOrderEvent(correlationId = "batch-1", correlationTotalCount = 3, orderId = 3L)
            dispatcher.deliver(event3, recipient, rule)

            Then("요약 메시지가 발송되고 SENT 로그가 저장된다") {
                val logSlot = slot<NotificationLog>()
                verify(exactly = 1) { logWriter.save(capture(logSlot)) }
                logSlot.captured.status shouldBe NotificationLogStatus.SENT
            }
        }
    }

    Given("동일 이벤트를 중복 발송하는 경우") {
        val messageRenderer = mockk<NotificationMessageRenderer>()
        val emailDispatcher = mockk<NotificationChannelDispatcher>()
        val logWriter = mockk<NotificationLogWriter>()

        every { emailDispatcher.supports(NotificationChannel.EMAIL) } returns true
        every { emailDispatcher.dispatch(any(), any(), any()) } just Runs
        every { messageRenderer.render(any()) } returns RenderedMessage(
            subject = "test", body = "test", pushTitle = "test",
            pushBody = "test", smsText = "test", deepLinkUrl = "/test",
        )
        // 첫 번째 호출: 중복 아님, 두 번째 호출: 중복
        every { logWriter.existsByIdempotencyKey(any()) } returnsMany listOf(false, true)
        every { logWriter.save(any()) } just Runs

        val dispatcher = NotificationDispatcher(messageRenderer, listOf(emailDispatcher), logWriter)
        val event = createOrderEvent(correlationId = null, correlationTotalCount = null)
        val recipient = createRecipient()
        val rule = EffectiveRule(channel = NotificationChannel.EMAIL, enabled = true, priority = NotificationPriority.HIGH)

        When("같은 이벤트를 두 번 deliver") {
            dispatcher.deliver(event, recipient, rule)
            dispatcher.deliver(event, recipient, rule)

            Then("로그는 1건만 저장된다 (중복 스킵)") {
                verify(exactly = 1) { logWriter.save(any()) }
            }
        }
    }

    Given("채널 디스패처가 예외를 던지는 경우") {
        val messageRenderer = mockk<NotificationMessageRenderer>()
        val failingDispatcher = mockk<NotificationChannelDispatcher>()
        val logWriter = mockk<NotificationLogWriter>()

        every { failingDispatcher.supports(NotificationChannel.EMAIL) } returns true
        every { failingDispatcher.dispatch(any(), any(), any()) } throws RuntimeException("SMTP connection failed")
        every { messageRenderer.render(any()) } returns RenderedMessage(
            subject = "test", body = "test", pushTitle = "test",
            pushBody = "test", smsText = "test", deepLinkUrl = "/test",
        )
        every { logWriter.existsByIdempotencyKey(any()) } returns false
        every { logWriter.save(any()) } just Runs

        val dispatcher = NotificationDispatcher(messageRenderer, listOf(failingDispatcher), logWriter)
        val event = createOrderEvent(correlationId = null, correlationTotalCount = null)
        val recipient = createRecipient()
        val rule = EffectiveRule(channel = NotificationChannel.EMAIL, enabled = true, priority = NotificationPriority.HIGH)

        When("deliver 호출") {
            dispatcher.deliver(event, recipient, rule)

            Then("FAILED 로그가 저장되고 실패 사유가 기록된다") {
                val logSlot = slot<NotificationLog>()
                verify(exactly = 1) { logWriter.save(capture(logSlot)) }
                logSlot.captured.status shouldBe NotificationLogStatus.FAILED
                logSlot.captured.failureReason shouldBe "SMTP connection failed"
            }
        }
    }
})

private fun createOrderEvent(
    correlationId: String?,
    correlationTotalCount: Int?,
    orderId: Long = 100L,
) = NotificationEvent(
    triggerType = NotificationTriggerType.ORDER_PLACED,
    storeId = 1L,
    orderId = orderId,
    payload = NotificationPayload.OrderPlaced(
        orderId = orderId,
        buyerName = "홍길동",
        storeName = "맛있는 빵집",
        amount = 35000L,
        itemCount = 1,
    ),
    correlationId = correlationId,
    correlationTotalCount = correlationTotalCount,
)

private fun createRecipient() = NotificationRecipient(
    userId = 42L,
    email = "seller@example.com",
    phone = "010-1234-5678",
    pushToken = "fcm-token-abc",
    isStoreOwner = true,
)
