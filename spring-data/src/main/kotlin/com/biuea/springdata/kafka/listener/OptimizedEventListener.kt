package com.biuea.springdata.kafka.listener

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.Event
import com.biuea.springdata.kafka.service.EventProcessor
import com.biuea.springdata.kafka.service.PerformanceMonitor
import io.micrometer.core.instrument.Timer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class OptimizedEventListener(
    private val eventProcessor: EventProcessor,
    private val performanceMonitor: PerformanceMonitor,
    private val kafkaConsumerExecutor: ThreadPoolTaskExecutor,
    private val optimizedConsumerTimer: Timer,
    private val optimizedConsumerCounter: AtomicLong
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.OPTIMIZED_EVENTS],
        groupId = "optimized-events-consumer",
        containerFactory = "optimizedKafkaListenerContainerFactory"
    )
    fun listen(records: List<ConsumerRecord<String, Event>>, acknowledgment: Acknowledgment) {
        optimizedConsumerTimer.record(Runnable {
            log.trace("Optimized consumer - received {} records", records.size)

            val futures = records.map { record ->
                CompletableFuture.runAsync({
                    try {
                        eventProcessor.process(record.value())
                    } catch (e: Exception) {
                        log.error("Failed to process event: {}", e.message)
                    }
                }, kafkaConsumerExecutor)
            }

            // Wait for all messages in the batch to be submitted/processed
            CompletableFuture.allOf(*futures.toTypedArray()).join()

            // Manual acknowledgment for the entire batch
            acknowledgment.acknowledge()

            optimizedConsumerCounter.addAndGet(records.size.toLong())
            performanceMonitor.recordOptimizedEvents(records.size)
        })
    }
}
