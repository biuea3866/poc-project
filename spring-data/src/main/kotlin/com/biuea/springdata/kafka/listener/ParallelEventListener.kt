package com.biuea.springdata.kafka.listener

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.Event
import com.biuea.springdata.kafka.dto.EventType
import com.biuea.springdata.kafka.service.PerformanceMonitor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.confluent.parallelconsumer.ParallelStreamProcessor
import io.confluent.parallelconsumer.PollContext
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Confluent Parallel Consumer 리스너
 *
 * 핵심 특징:
 * 1. 파티션 내 순서를 보장하면서 병렬 처리 가능 (KEY ordering)
 * 2. 개별 메시지 단위 오프셋 커밋 (fine-grained commit)
 * 3. 실패한 메시지만 재처리 (batch 전체 재처리 X)
 * 4. 내부적으로 offset 관리 최적화
 *
 * vs OptimizedEventListener (전략 C):
 * - 전략 C: 배치를 청크로 나눠 병렬 처리 → 순서 보장 X, 실패 시 배치 전체 재처리
 * - Parallel Consumer: 메시지별 병렬 처리 → KEY별 순서 보장, 실패 메시지만 재처리
 */
@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ParallelEventListener(
    private val parallelStreamProcessor: ParallelStreamProcessor<String, String>,
    private val performanceMonitor: PerformanceMonitor,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val processedCount = AtomicLong(0)
    private val isRunning = AtomicBoolean(false)

    @PostConstruct
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            log.info("Starting Parallel Consumer for topic: {}", KafkaTopics.PARALLEL_EVENTS)

            // 토픽 구독
            parallelStreamProcessor.subscribe(listOf(KafkaTopics.PARALLEL_EVENTS))

            // 병렬 처리 시작
            parallelStreamProcessor.poll { context: PollContext<String, String> ->
                processMessage(context)
            }
        }
    }

    @PreDestroy
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            log.info("Stopping Parallel Consumer")
            parallelStreamProcessor.close()
        }
    }

    private fun processMessage(context: PollContext<String, String>) {
        val record = context.singleRecord.consumerRecord

        try {
            log.debug(
                "Parallel consumer - partition: {}, offset: {}, key: {}",
                record.partition(),
                record.offset(),
                record.key()
            )

            // JSON 역직렬화
            val event = parseEvent(record.value())

            // 비즈니스 로직 처리
            processEvent(event)

            // PerformanceMonitor에 기록
            performanceMonitor.recordParallelEvent()

            // 처리 완료 카운트
            val count = processedCount.incrementAndGet()
            if (count % 1000 == 0L) {
                log.info("Parallel consumer processed {} events", count)
            }

        } catch (e: Exception) {
            log.error(
                "Error processing message - partition: {}, offset: {}, error: {}",
                record.partition(),
                record.offset(),
                e.message,
                e
            )
            // 예외를 다시 던지면 Parallel Consumer가 재시도 처리
            throw e
        }
    }

    private fun parseEvent(json: String): Event {
        return try {
            objectMapper.readValue(json)
        } catch (e: Exception) {
            // 파싱 실패 시 기본 이벤트 생성
            log.warn("Failed to parse event JSON, using default: {}", e.message)
            Event(type = EventType.NOTIFICATION_SENT, payload = mapOf("raw" to json))
        }
    }

    private fun processEvent(event: Event) {
        // 실제 비즈니스 로직
        log.trace("Processing event: id={}, type={}", event.id, event.type)
    }

    fun getProcessedCount(): Long = processedCount.get()

    fun resetCount() {
        processedCount.set(0)
    }
}
