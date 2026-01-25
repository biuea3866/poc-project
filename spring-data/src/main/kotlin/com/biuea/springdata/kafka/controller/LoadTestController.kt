package com.biuea.springdata.kafka.controller

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.producer.BulkSendResult
import com.biuea.springdata.kafka.producer.EventProducer
import com.biuea.springdata.kafka.service.PerformanceMonitor
import com.biuea.springdata.kafka.service.PerformanceStats
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@RestController
@RequestMapping("/api/kafka/load-test")
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class LoadTestController(
    private val eventProducer: EventProducer,
    private val performanceMonitor: PerformanceMonitor
) {

    private val executor = Executors.newFixedThreadPool(10)

    @PostMapping("/send/{topic}")
    fun sendEvents(
        @PathVariable topic: String,
        @RequestParam(defaultValue = "10000") count: Int,
        @RequestParam(defaultValue = "0") delayMs: Long
    ): ResponseEntity<BulkSendResult> {
        val targetTopic = when (topic) {
            "normal" -> KafkaTopics.NORMAL_EVENTS
            "batch" -> KafkaTopics.BATCH_EVENTS
            "optimized" -> KafkaTopics.OPTIMIZED_EVENTS
            "parallel" -> KafkaTopics.PARALLEL_EVENTS
            "all" -> "all"
            else -> return ResponseEntity.badRequest().build()
        }

        val result = eventProducer.sendBulk(targetTopic, count, delayMs)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/send/{topic}/async")
    fun sendEventsAsync(
        @PathVariable topic: String,
        @RequestParam(defaultValue = "10000") count: Int,
        @RequestParam(defaultValue = "0") delayMs: Long
    ): ResponseEntity<Map<String, Any>> {
        val targetTopic = when (topic) {
            "normal" -> KafkaTopics.NORMAL_EVENTS
            "batch" -> KafkaTopics.BATCH_EVENTS
            "optimized" -> KafkaTopics.OPTIMIZED_EVENTS
            "parallel" -> KafkaTopics.PARALLEL_EVENTS
            "all" -> "all"
            else -> return ResponseEntity.badRequest().build()
        }

        CompletableFuture.runAsync({
            eventProducer.sendBulk(targetTopic, count, delayMs)
        }, executor)

        return ResponseEntity.accepted().body(
            mapOf(
                "status" to "started",
                "topic" to topic,
                "count" to count
            )
        )
    }

    @GetMapping("/stats")
    fun getStats(): ResponseEntity<PerformanceStats> {
        return ResponseEntity.ok(performanceMonitor.getStats())
    }

    @PostMapping("/stats/reset")
    fun resetStats(): ResponseEntity<Map<String, String>> {
        performanceMonitor.reset()
        eventProducer.resetSentCount()
        return ResponseEntity.ok(mapOf("status" to "reset"))
    }

    @GetMapping("/sent-count")
    fun getSentCount(): ResponseEntity<Map<String, Long>> {
        return ResponseEntity.ok(mapOf("sentCount" to eventProducer.getSentCount()))
    }
}
