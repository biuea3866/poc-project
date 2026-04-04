// 패턴: 통합 알림 시스템의 발송 디스패처
package com.example.notification.application.service

import com.example.notification.application.port.NotificationLogWriter
import com.example.notification.domain.channel.NotificationChannelDispatcher
import com.example.notification.domain.model.EffectiveRule
import com.example.notification.domain.model.NotificationEvent
import com.example.notification.domain.model.NotificationLog
import com.example.notification.domain.model.NotificationRecipient
import com.example.notification.domain.model.RenderedMessage
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 알림 발송 통합 디스패처.
 *
 * - 단건 (correlationId == null) -> 즉시 발송
 * - 벌크 (correlationId != null) -> buffer -> flush (count 도달 즉시 or timeout)
 *
 * correlationId 기반 배칭:
 * 1. totalCount 도달 시 즉시 flush
 * 2. TIMEOUT_MS 내 totalCount 미도달 시 타임아웃 flush
 * 3. 1건이면 단건 발송, 2건+ 이면 요약 메시지 발송
 */
@Component
class NotificationDispatcher(
    private val messageRenderer: NotificationMessageRenderer,
    private val channelDispatchers: List<NotificationChannelDispatcher>,
    private val logWriter: NotificationLogWriter,
) {
    private data class CorrelationKey(
        val recipientUserId: Long,
        val triggerType: String,
        val correlationId: String,
    )

    private data class CorrelationEntry(
        val events: MutableList<NotificationEvent> = mutableListOf(),
        var recipient: NotificationRecipient,
        var rule: EffectiveRule,
        var timeoutFuture: ScheduledFuture<*>? = null,
    )

    private val buffer = ConcurrentHashMap<CorrelationKey, CorrelationEntry>()

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "notification-batch-timeout").apply { isDaemon = true }
    }

    companion object {
        const val TIMEOUT_MS = 5000L
    }

    fun deliver(event: NotificationEvent, recipient: NotificationRecipient, rule: EffectiveRule) {
        if (event.isCorrelated) {
            enqueue(event, recipient, rule)
        } else {
            sendSingle(event, recipient, rule)
        }
    }

    // -- 단건 --

    private fun sendSingle(event: NotificationEvent, recipient: NotificationRecipient, rule: EffectiveRule) {
        val idempotencyKey = event.generateIdempotencyKey(recipient.userId, rule.channel)
        if (isDuplicate(idempotencyKey)) return

        val message = messageRenderer.render(event)
        sendAndLog(message, event, recipient, rule, idempotencyKey)
    }

    // -- 벌크 --

    private fun enqueue(event: NotificationEvent, recipient: NotificationRecipient, rule: EffectiveRule) {
        val key = CorrelationKey(recipient.userId, event.triggerType.name, event.correlationId!!)
        val entry = buffer.getOrPut(key) { CorrelationEntry(recipient = recipient, rule = rule) }

        synchronized(entry) {
            entry.events.add(event)

            if (event.isCorrelationCountReached(entry.events.size)) {
                entry.timeoutFuture?.cancel(false)
                flush(key)
            } else if (entry.timeoutFuture == null) {
                entry.timeoutFuture = scheduler.schedule({ flush(key) }, TIMEOUT_MS, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun flush(key: CorrelationKey) {
        val entry = buffer.remove(key) ?: return

        if (entry.events.size <= 1) {
            sendSingle(entry.events.first(), entry.recipient, entry.rule)
        } else {
            sendCorrelatedSummary(entry.events, entry.recipient, entry.rule)
        }
    }

    private fun sendCorrelatedSummary(
        events: List<NotificationEvent>,
        recipient: NotificationRecipient,
        rule: EffectiveRule,
    ) {
        val firstEvent = events.first()
        val idempotencyKey = firstEvent.generateCorrelationIdempotencyKey(recipient.userId, rule.channel)
        if (isDuplicate(idempotencyKey)) return

        val message = messageRenderer.renderCorrelatedSummary(events)
        sendAndLog(message, firstEvent, recipient, rule, idempotencyKey)

        println(
            "Correlated summary dispatched: ${events.size} events, " +
                "correlationId=${firstEvent.correlationId}, recipient=${recipient.userId}"
        )
    }

    // -- 공통 --

    private fun sendAndLog(
        message: RenderedMessage,
        event: NotificationEvent,
        recipient: NotificationRecipient,
        rule: EffectiveRule,
        idempotencyKey: String,
    ) {
        try {
            channelDispatchers.find { it.supports(rule.channel) }?.dispatch(message, recipient, event)
            logWriter.save(NotificationLog.sent(event, recipient, rule, idempotencyKey))
        } catch (e: Exception) {
            println("Dispatch failed: trigger=${event.triggerType}, recipient=${recipient.userId}, channel=${rule.channel}, error=${e.message}")
            logWriter.save(NotificationLog.failed(event, recipient, rule, idempotencyKey, e.message ?: e.toString()))
        }
    }

    private fun isDuplicate(idempotencyKey: String): Boolean {
        if (logWriter.existsByIdempotencyKey(idempotencyKey)) {
            println("Duplicate notification skipped: $idempotencyKey")
            return true
        }
        return false
    }
}
