package com.biuea.springdata.kafka.config

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.atomic.AtomicLong

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class MetricsConfig {

    @Bean
    fun normalConsumerTimer(meterRegistry: MeterRegistry): Timer {
        return Timer.builder("kafka.consumer.normal.processing.time")
            .description("Time taken to process normal consumer messages")
            .register(meterRegistry)
    }

    @Bean
    fun batchConsumerTimer(meterRegistry: MeterRegistry): Timer {
        return Timer.builder("kafka.consumer.batch.processing.time")
            .description("Time taken to process batch consumer messages")
            .register(meterRegistry)
    }

    @Bean
    fun optimizedConsumerTimer(meterRegistry: MeterRegistry): Timer {
        return Timer.builder("kafka.consumer.optimized.processing.time")
            .description("Time taken to process optimized consumer messages")
            .register(meterRegistry)
    }

    @Bean
    fun normalConsumerCounter(meterRegistry: MeterRegistry): AtomicLong {
        return meterRegistry.gauge("kafka.consumer.normal.count", AtomicLong(0))!!
    }

    @Bean
    fun batchConsumerCounter(meterRegistry: MeterRegistry): AtomicLong {
        return meterRegistry.gauge("kafka.consumer.batch.count", AtomicLong(0))!!
    }

    @Bean
    fun optimizedConsumerCounter(meterRegistry: MeterRegistry): AtomicLong {
        return meterRegistry.gauge("kafka.consumer.optimized.count", AtomicLong(0))!!
    }
}
