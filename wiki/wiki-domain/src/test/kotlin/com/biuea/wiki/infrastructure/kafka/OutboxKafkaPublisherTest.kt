package com.biuea.wiki.infrastructure.kafka

import com.biuea.wiki.domain.outbox.OutboxService
import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import kotlin.test.assertEquals

/**
 * NAW-128 AC1, AC6 검증
 *
 * AC1: Kafka 발행 실패 시 outbox 테이블에 이벤트가 정확히 기록된다
 * AC6: 기존 Kafka 발행이 성공하면 outbox에 저장하지 않는다 (하위 호환)
 */
@ExtendWith(MockitoExtension::class)
class OutboxKafkaPublisherTest {

    @Mock
    private lateinit var kafkaTemplate: KafkaTemplate<Any, Any>

    @Mock
    private lateinit var outboxService: OutboxService

    private lateinit var objectMapper: ObjectMapper
    private lateinit var publisher: OutboxKafkaPublisher

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        publisher = OutboxKafkaPublisher(kafkaTemplate, outboxService, objectMapper)
    }

    private fun mockKafkaSuccess() {
        val successFuture: CompletableFuture<SendResult<Any, Any>> = CompletableFuture.completedFuture(null)
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any()))
            .thenReturn(successFuture)
    }

    private fun mockKafkaFailure() {
        val failFuture = CompletableFuture<SendResult<Any, Any>>()
        failFuture.completeExceptionally(RuntimeException("Kafka broker down"))
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any()))
            .thenReturn(failFuture)
    }

    // AC6: Kafka 발행 성공 시 outbox에 저장하지 않는다
    @Test
    fun `publish does NOT save to outbox when kafka send succeeds`() {
        mockKafkaSuccess()

        publisher.publish(
            topic = "event.document",
            key = "1",
            payload = mapOf("documentId" to 1),
            aggregateType = "Document",
            aggregateId = "1",
        )

        verify(outboxService, never()).save(any())
    }

    // AC1: Kafka 발행 실패 시 outbox 테이블에 이벤트가 PENDING 상태로 저장된다
    @Test
    fun `publish saves PENDING outbox event when kafka send fails`() {
        mockKafkaFailure()
        val captor = argumentCaptor<OutboxEvent>()
        whenever(outboxService.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        publisher.publish(
            topic = "event.document",
            key = "42",
            payload = mapOf("documentId" to 42),
            aggregateType = "Document",
            aggregateId = "42",
        )

        verify(outboxService).save(captor.capture())
        val saved = captor.firstValue
        assertEquals("Document", saved.aggregateType)
        assertEquals("42", saved.aggregateId)
        assertEquals("event.document", saved.topic)
        assertEquals(OutboxStatus.PENDING, saved.status)
    }

    // AC1: outbox 저장 시 payload가 JSON 직렬화된 형태로 저장된다
    @Test
    fun `publish saves JSON serialized payload to outbox`() {
        mockKafkaFailure()
        val captor = argumentCaptor<OutboxEvent>()
        whenever(outboxService.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        val payload = mapOf("documentId" to 1, "title" to "Test")
        publisher.publish(
            topic = "event.document",
            key = "1",
            payload = payload,
            aggregateType = "Document",
            aggregateId = "1",
        )

        verify(outboxService).save(captor.capture())
        val saved = captor.firstValue
        val deserializedPayload = objectMapper.readValue(saved.payload, Map::class.java)
        assertEquals(1, deserializedPayload["documentId"])
        assertEquals("Test", deserializedPayload["title"])
    }
}
