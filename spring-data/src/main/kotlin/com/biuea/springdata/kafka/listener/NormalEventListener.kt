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
import org.springframework.context.annotation.Profile

// 파티셔닝 재검증(rebench)은 MySQL 만 쓴다. 브로커가 없으면 컨슈머 기동에서 막히고,
// 접속 재시도가 측정 구간의 CPU 와 로그를 잠식한다.
@Profile("!rebench")
@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class NormalEventListener(
    private val eventProcessor: EventProcessor,
    private val performanceMonitor: PerformanceMonitor,
    private val normalConsumerTimer: Timer,
    private val normalConsumerCounter: AtomicLong
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.NORMAL_EVENTS],
        groupId = "normal-events-consumer",
        containerFactory = "normalKafkaListenerContainerFactory"
    )
    fun listen(record: ConsumerRecord<String, Event>) {
        normalConsumerTimer.record(Runnable {
            val event = record.value()
            log.debug(
                "Normal consumer - partition: {}, offset: {}, event: {}",
                record.partition(),
                record.offset(),
                event.id
            )

            eventProcessor.process(event)
            normalConsumerCounter.incrementAndGet()
            performanceMonitor.recordNormalEvent()
        })
    }
}
