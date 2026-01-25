package com.biuea.springdata.kafka.config

import io.confluent.parallelconsumer.ParallelConsumerOptions
import io.confluent.parallelconsumer.ParallelConsumerOptions.ProcessingOrder
import io.confluent.parallelconsumer.ParallelStreamProcessor
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class ParallelConsumerConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun parallelConsumerKafkaConsumer(): Consumer<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "parallel-events-consumer",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 1000
        )
        return KafkaConsumer(props)
    }

    /**
     * Confluent Parallel Consumer 옵션
     *
     * ProcessingOrder 옵션:
     * - UNORDERED: 최대 병렬성, 순서 보장 없음
     * - KEY: 같은 키를 가진 메시지는 순서 보장 (권장)
     * - PARTITION: 파티션 내 순서 보장 (일반 Consumer와 동일)
     */
    @Bean
    fun parallelConsumerOptions(
        parallelConsumerKafkaConsumer: Consumer<String, String>
    ): ParallelConsumerOptions<String, String> {
        return ParallelConsumerOptions.builder<String, String>()
            .consumer(parallelConsumerKafkaConsumer)
            .ordering(ProcessingOrder.KEY)  // 같은 키는 순서 보장
            .maxConcurrency(10)            // 최대 동시 처리 수
            .build()
    }

    @Bean(destroyMethod = "close")
    fun parallelStreamProcessor(
        options: ParallelConsumerOptions<String, String>
    ): ParallelStreamProcessor<String, String> {
        return ParallelStreamProcessor.createEosStreamProcessor(options)
    }
}
