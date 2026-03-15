package com.biuea.wiki.infrastructure.kafka

import com.biuea.wiki.domain.outbox.OutboxService
import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture

/**
 * NAW-128 AC2, AC3, AC4 검증
 *
 * AC2: 스케줄러가 PENDING 상태의 outbox 이벤트를 1분 주기로 재발행한다
 * AC3: 재발행 성공 시 outbox 상태가 SUCCESS로 전이된다
 * AC4: 재시도 5회 초과 시 DEAD_LETTER 상태로 전이되고 알림이 발생한다
 */
@ExtendWith(MockitoExtension::class)
class OutboxSchedulerTest {

    @Mock
    private lateinit var outboxService: OutboxService

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<Any, Any>

    private lateinit var objectMapper: ObjectMapper
    private lateinit var scheduler: OutboxScheduler

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        scheduler = OutboxScheduler(outboxService, kafkaTemplate, objectMapper)
    }

    private fun createPendingEvent(id: Long = 1L): OutboxEvent {
        return OutboxEvent.create(
            aggregateType = "Document",
            aggregateId = id.toString(),
            topic = "event.document",
            payload = """{"documentId":$id}""",
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockKafkaSend(future: CompletableFuture<SendResult<Any, Any>>) {
        whenever(kafkaTemplate.send(any<ProducerRecord<Any, Any>>())).thenReturn(future)
    }

    // AC2: pending 이벤트가 없으면 아무 작업도 하지 않는다
    @Test
    fun `publishPendingOutboxEvents does nothing when no pending events`() {
        whenever(outboxService.findPendingAndFailed(anyInt())).thenReturn(emptyList())

        scheduler.publishPendingOutboxEvents()

        verify(outboxService, never()).markSuccess(any())
        verify(outboxService, never()).markFailed(any(), any())
    }

    // AC2, AC3: PENDING 이벤트가 있으면 Kafka에 재발행하고 SUCCESS로 전이한다
    @Test
    fun `publishPendingOutboxEvents marks event SUCCESS on successful kafka send`() {
        val event = createPendingEvent(id = 1L)
        whenever(outboxService.findPendingAndFailed(anyInt())).thenReturn(listOf(event))

        val future: CompletableFuture<SendResult<Any, Any>> = CompletableFuture.completedFuture(null)
        mockKafkaSend(future)

        scheduler.publishPendingOutboxEvents()

        verify(outboxService).markSuccess(event)
        verify(outboxService, never()).markFailed(any(), any())
    }

    // AC4: Kafka 발행 실패 시 markFailed가 호출된다
    @Test
    fun `publishPendingOutboxEvents marks event failed on kafka send exception`() {
        val event = createPendingEvent(id = 1L)
        whenever(outboxService.findPendingAndFailed(anyInt())).thenReturn(listOf(event))

        val failedFuture = CompletableFuture<SendResult<Any, Any>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka broker unavailable"))
        mockKafkaSend(failedFuture)

        scheduler.publishPendingOutboxEvents()

        verify(outboxService).markFailed(any(), any())
        verify(outboxService, never()).markSuccess(any())
    }

    // AC4: 여러 이벤트 중 일부 성공/일부 실패 — 각각 올바르게 처리된다
    @Test
    fun `publishPendingOutboxEvents processes multiple events independently`() {
        val event1 = createPendingEvent(id = 1L)
        val event2 = createPendingEvent(id = 2L)
        whenever(outboxService.findPendingAndFailed(anyInt())).thenReturn(listOf(event1, event2))

        val successFuture: CompletableFuture<SendResult<Any, Any>> = CompletableFuture.completedFuture(null)
        val failFuture = CompletableFuture<SendResult<Any, Any>>()
        failFuture.completeExceptionally(RuntimeException("send failed"))

        @Suppress("UNCHECKED_CAST")
        whenever(kafkaTemplate.send(any<ProducerRecord<Any, Any>>()))
            .thenReturn(successFuture)
            .thenReturn(failFuture)

        scheduler.publishPendingOutboxEvents()

        verify(outboxService).markSuccess(event1)
        verify(outboxService).markFailed(any(), any())
    }
}
