package com.biuea.springdata.kafka.controller

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.Event
import com.biuea.springdata.kafka.dto.EventType
import com.biuea.springdata.kafka.listener.ParallelEventListener
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/kafka/parallel")
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ParallelConsumerController(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
    private val parallelEventListener: ParallelEventListener,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val executor = Executors.newFixedThreadPool(10)

    /**
     * Parallel Consumer 토픽에 메시지 발송
     */
    @PostMapping("/send")
    fun sendEvents(
        @RequestParam(defaultValue = "10000") count: Int,
        @RequestParam(defaultValue = "10") keyCount: Int  // 키 개수 (순서 보장 그룹)
    ): ResponseEntity<Map<String, Any>> {
        log.info("Sending {} events to parallel-events topic with {} unique keys", count, keyCount)

        val startTime = System.currentTimeMillis()
        val eventTypes = EventType.entries.toTypedArray()
        var successCount = 0

        repeat(count) { i ->
            val event = Event(
                type = eventTypes[i % eventTypes.size],
                payload = mapOf("index" to i, "data" to "parallel-test-$i")
            )

            // 키를 keyCount 개로 분산 (같은 키는 순서 보장됨)
            val key = "key-${i % keyCount}"

            try {
                // Parallel Consumer는 String 메시지 사용
                kafkaTemplate.send(KafkaTopics.PARALLEL_EVENTS, key, event)
                successCount++
            } catch (e: Exception) {
                log.error("Failed to send event {}: {}", i, e.message)
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val tps = if (elapsed > 0) (successCount * 1000.0 / elapsed) else 0.0

        return ResponseEntity.ok(
            mapOf(
                "topic" to KafkaTopics.PARALLEL_EVENTS,
                "totalRequested" to count,
                "successCount" to successCount,
                "keyCount" to keyCount,
                "elapsedMs" to elapsed,
                "producerTps" to tps
            )
        )
    }

    /**
     * 비동기 대량 발송
     */
    @PostMapping("/send/async")
    fun sendEventsAsync(
        @RequestParam(defaultValue = "100000") count: Int,
        @RequestParam(defaultValue = "100") keyCount: Int
    ): ResponseEntity<Map<String, Any>> {
        CompletableFuture.runAsync({
            sendEvents(count, keyCount)
        }, executor)

        return ResponseEntity.accepted().body(
            mapOf(
                "status" to "started",
                "topic" to KafkaTopics.PARALLEL_EVENTS,
                "count" to count,
                "keyCount" to keyCount
            )
        )
    }

    /**
     * Parallel Consumer 처리 통계
     */
    @GetMapping("/stats")
    fun getStats(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "processedCount" to parallelEventListener.getProcessedCount()
            )
        )
    }

    /**
     * 통계 초기화
     */
    @PostMapping("/stats/reset")
    fun resetStats(): ResponseEntity<Map<String, String>> {
        parallelEventListener.resetCount()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }
}
