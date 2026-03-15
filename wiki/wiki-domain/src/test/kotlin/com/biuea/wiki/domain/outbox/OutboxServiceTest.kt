package com.biuea.wiki.domain.outbox

import com.biuea.wiki.domain.outbox.entity.OutboxEvent
import com.biuea.wiki.domain.outbox.entity.OutboxStatus
import com.biuea.wiki.infrastructure.outbox.OutboxEventRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.util.Optional
import kotlin.test.assertEquals

/**
 * NAW-128 AC5 검증
 *
 * AC5: 어드민 API로 실패 이벤트 조회 및 수동 재처리가 가능하다
 */
@ExtendWith(MockitoExtension::class)
class OutboxServiceTest {

    @Mock
    private lateinit var outboxEventRepository: OutboxEventRepository

    private lateinit var outboxService: OutboxService

    @BeforeEach
    fun setUp() {
        outboxService = OutboxService(outboxEventRepository)
    }

    private fun createPendingEvent(): OutboxEvent {
        return OutboxEvent.create(
            aggregateType = "Document",
            aggregateId = "1",
            topic = "event.document",
            payload = """{"documentId":1}""",
        )
    }

    // AC5: status 필터로 outbox 이벤트를 조회할 수 있다
    @Test
    fun `findByStatus returns paged outbox events by status`() {
        val events = listOf(createPendingEvent())
        val page = PageImpl(events)
        whenever(outboxEventRepository.findAllByStatus(OutboxStatus.PENDING, Pageable.unpaged()))
            .thenReturn(page)

        val result = outboxService.findByStatus(OutboxStatus.PENDING, Pageable.unpaged())

        assertEquals(1, result.totalElements)
        assertEquals(OutboxStatus.PENDING, result.content[0].status)
    }

    // AC5: 수동 재처리 - resetForRetry 호출 시 PENDING으로 리셋된다
    @Test
    fun `resetForRetry resets event status to PENDING`() {
        val event = createPendingEvent()
        repeat(5) { event.markFailed("error") }
        assertEquals(OutboxStatus.DEAD_LETTER, event.status)

        whenever(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event))
        whenever(outboxEventRepository.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        val result = outboxService.resetForRetry(1L)

        assertEquals(OutboxStatus.PENDING, result.status)
        verify(outboxEventRepository).save(event)
    }

    // AC5: 존재하지 않는 이벤트 ID로 재처리 시도 시 예외가 발생한다
    @Test
    fun `resetForRetry throws when event not found`() {
        whenever(outboxEventRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<IllegalArgumentException> {
            outboxService.resetForRetry(999L)
        }
    }

    // markSuccess: 성공 처리 후 repository.save 호출 검증
    @Test
    fun `markSuccess saves event after marking success`() {
        val event = createPendingEvent()
        whenever(outboxEventRepository.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        outboxService.markSuccess(event)

        assertEquals(OutboxStatus.SUCCESS, event.status)
        verify(outboxEventRepository).save(event)
    }

    // markFailed: 실패 처리 후 repository.save 호출 검증
    @Test
    fun `markFailed saves event after marking failed`() {
        val event = createPendingEvent()
        whenever(outboxEventRepository.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        outboxService.markFailed(event, "Kafka error")

        assertEquals(OutboxStatus.FAILED, event.status)
        verify(outboxEventRepository).save(event)
    }

    // DEAD_LETTER 전이 — retryCount가 maxRetries에 도달 시 DEAD_LETTER로 전이된다
    @Test
    fun `markFailed transitions to DEAD_LETTER when retryCount reaches maxRetries`() {
        val event = createPendingEvent()
        whenever(outboxEventRepository.save(any())).thenAnswer { it.arguments[0] as OutboxEvent }

        repeat(5) { outboxService.markFailed(event, "error") }

        assertEquals(OutboxStatus.DEAD_LETTER, event.status)
        assertEquals(5, event.retryCount)
    }
}
