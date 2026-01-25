package com.biuea.springdata.kafka.producer

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.Event
import com.biuea.springdata.kafka.dto.EventType
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class EventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val sentCount = AtomicLong(0)

    fun sendToNormal(event: Event): CompletableFuture<Unit> {
        return send(KafkaTopics.NORMAL_EVENTS, event)
    }

    fun sendToBatch(event: Event): CompletableFuture<Unit> {
        return send(KafkaTopics.BATCH_EVENTS, event)
    }

    fun sendToOptimized(event: Event): CompletableFuture<Unit> {
        return send(KafkaTopics.OPTIMIZED_EVENTS, event)
    }

    fun sendToParallel(event: Event): CompletableFuture<Unit> {
        return send(KafkaTopics.PARALLEL_EVENTS, event)
    }

    fun sendToAll(event: Event): List<CompletableFuture<Unit>> {
        return listOf(
            sendToNormal(event),
            sendToBatch(event),
            sendToOptimized(event),
            sendToParallel(event)
        )
    }

    private fun send(topic: String, event: Event): CompletableFuture<Unit> {
        val completableFuture = CompletableFuture<Unit>()

        kafkaTemplate.send(topic, event.id, event).whenComplete { result, ex ->
            if (ex == null) {
                sentCount.incrementAndGet()
                completableFuture.complete(Unit)
            } else {
                log.error("Failed to send event to {}: {}", topic, event.id, ex)
                completableFuture.completeExceptionally(ex)
            }
        }

        return completableFuture
    }

    fun sendBulk(topic: String, count: Int, delayMs: Long = 0): BulkSendResult {
        log.info("Starting bulk send: {} events to {}", count, topic)
        val startTime = System.currentTimeMillis()
        val eventTypes = EventType.entries.toTypedArray()
        var successCount = 0
        var failCount = 0

        repeat(count) { i ->
            val event = Event(
                type = eventTypes[i % eventTypes.size],
                payload = mapOf("index" to i, "data" to "test-data-$i")
            )

            try {
                when (topic) {
                    "all" -> sendToAll(event)
                    KafkaTopics.NORMAL_EVENTS -> sendToNormal(event)
                    KafkaTopics.BATCH_EVENTS -> sendToBatch(event)
                    KafkaTopics.OPTIMIZED_EVENTS -> sendToOptimized(event)
                    KafkaTopics.PARALLEL_EVENTS -> sendToParallel(event)
                    else -> throw IllegalArgumentException("Unknown topic: $topic")
                }
                successCount++
            } catch (e: Exception) {
                failCount++
                log.error("Failed to send event {}: {}", i, e.message)
            }

            if (delayMs > 0 && i % 1000 == 0) {
                Thread.sleep(delayMs)
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        val tps = if (elapsed > 0) (successCount * 1000.0 / elapsed) else 0.0

        log.info(
            "Bulk send completed: {} success, {} failed, {} ms, TPS: {:.2f}",
            successCount, failCount, elapsed, tps
        )

        return BulkSendResult(
            totalRequested = count,
            successCount = successCount,
            failCount = failCount,
            elapsedMs = elapsed,
            tps = tps
        )
    }

    fun getSentCount(): Long = sentCount.get()

    fun resetSentCount() {
        sentCount.set(0)
    }
}

data class BulkSendResult(
    val totalRequested: Int,
    val successCount: Int,
    val failCount: Int,
    val elapsedMs: Long,
    val tps: Double
)
