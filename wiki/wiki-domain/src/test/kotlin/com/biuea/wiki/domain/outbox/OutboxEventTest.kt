package com.biuea.wiki.domain.outbox

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * NAW-128 AC1, AC3, AC4 검증
 *
 * AC1: Kafka 발행 실패 시 outbox 테이블에 이벤트가 정확히 기록된다
 * AC3: 재발행 성공 시 outbox 상태가 SUCCESS로 전이된다
 * AC4: 재시도 5회 초과 시 DEAD_LETTER 상태로 전이된다
 */
class OutboxEventTest {

    private fun createPendingEvent(maxRetries: Int = 5): OutboxEvent {
        return OutboxEvent.create(
            aggregateType = "Document",
            aggregateId = "1",
            topic = "event.document",
            payload = """{"documentId":1}""",
        ).also {
            // maxRetries는 기본 5이므로 별도로 검증할 수 없음 — 생성 시 올바르게 세팅되는지 확인
        }
    }

    // AC1: outbox 이벤트 생성 시 PENDING 상태로 기록된다
    @Test
    fun `create factory creates PENDING event with correct fields`() {
        val event = OutboxEvent.create(
            aggregateType = "Document",
            aggregateId = "42",
            topic = "event.document",
            payload = """{"documentId":42}""",
        )

        assertEquals(OutboxStatus.PENDING, event.status)
        assertEquals("Document", event.aggregateType)
        assertEquals("42", event.aggregateId)
        assertEquals("event.document", event.topic)
        assertEquals("""{"documentId":42}""", event.payload)
        assertEquals(0, event.retryCount)
        assertEquals(5, event.maxRetries)
        assertNull(event.processedAt)
        assertNull(event.errorMessage)
    }

    // AC3: 재발행 성공 시 SUCCESS로 전이되고 processedAt이 기록된다
    @Test
    fun `markSuccess transitions status to SUCCESS and sets processedAt`() {
        val event = createPendingEvent()

        event.markSuccess()

        assertEquals(OutboxStatus.SUCCESS, event.status)
        assertNotNull(event.processedAt)
        assertNull(event.errorMessage)
    }

    // AC4: 재시도 횟수 증가 — max_retries 미만이면 FAILED 상태로 유지
    @Test
    fun `markFailed increments retryCount and stays FAILED when under maxRetries`() {
        val event = createPendingEvent()

        event.markFailed("Kafka connection timeout")

        assertEquals(OutboxStatus.FAILED, event.status)
        assertEquals(1, event.retryCount)
        assertEquals("Kafka connection timeout", event.errorMessage)
    }

    // AC4: 재시도 5회 초과 시 DEAD_LETTER로 전이된다
    @Test
    fun `markFailed transitions to DEAD_LETTER when retryCount reaches maxRetries`() {
        val event = createPendingEvent()

        // 5번 실패 → 마지막에 DEAD_LETTER
        repeat(5) { event.markFailed("error") }

        assertEquals(OutboxStatus.DEAD_LETTER, event.status)
        assertEquals(5, event.retryCount)
    }

    // resetForRetry: DEAD_LETTER 이벤트를 수동으로 PENDING으로 복구
    @Test
    fun `resetForRetry resets status to PENDING and clears errorMessage`() {
        val event = createPendingEvent()
        repeat(5) { event.markFailed("error") }
        assertEquals(OutboxStatus.DEAD_LETTER, event.status)

        event.resetForRetry()

        assertEquals(OutboxStatus.PENDING, event.status)
        assertNull(event.errorMessage)
    }

    // retryCount가 maxRetries - 1 에서 markFailed 하면 DEAD_LETTER 직행
    @Test
    fun `markFailed at maxRetries minus one transitions to DEAD_LETTER`() {
        val event = createPendingEvent()
        repeat(4) { event.markFailed("previous error") }
        assertEquals(OutboxStatus.FAILED, event.status)
        assertEquals(4, event.retryCount)

        event.markFailed("final error")

        assertEquals(OutboxStatus.DEAD_LETTER, event.status)
        assertEquals(5, event.retryCount)
    }
}
