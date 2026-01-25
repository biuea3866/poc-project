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
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class BatchEventListener(
    private val eventProcessor: EventProcessor,
    private val performanceMonitor: PerformanceMonitor,
    private val batchConsumerTimer: Timer,
    private val batchConsumerCounter: AtomicLong
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.BATCH_EVENTS],
        groupId = "batch-events-consumer",
        containerFactory = "batchKafkaListenerContainerFactory"
    )
    fun listen(records: List<ConsumerRecord<String, Event>>) {
        batchConsumerTimer.record(Runnable {
            log.debug("Batch consumer - received {} records", records.size)

            val events = records.map { it.value() }
            eventProcessor.processBatch(events)

            batchConsumerCounter.addAndGet(records.size.toLong())
            performanceMonitor.recordBatchEvents(records.size)
        })
    }
}
