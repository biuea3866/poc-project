package com.biuea.springdata.kafka.config

import com.biuea.springdata.kafka.dto.Event
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class OptimizedConsumerConfig(
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun optimizedEventConsumerFactory(): ConsumerFactory<String, Event> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "optimized-events-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500,
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1048576,
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 500,
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to 300000
        )

        val jsonDeserializer = JsonDeserializer(Event::class.java, objectMapper).apply {
            addTrustedPackages("*")
            setUseTypeHeaders(false)
        }

        return DefaultKafkaConsumerFactory(
            configProps,
            StringDeserializer(),
            ErrorHandlingDeserializer(jsonDeserializer)
        )
    }

    @Bean
    fun kafkaConsumerExecutor(): ThreadPoolTaskExecutor {
        return ThreadPoolTaskExecutor().apply {
            corePoolSize = 10
            maxPoolSize = 10
            queueCapacity = 10000
            setThreadNamePrefix("kafka-consumer-")
            initialize()
        }
    }

    @Bean
    fun optimizedErrorHandler(kafkaTemplate: KafkaTemplate<String, Any>): CommonErrorHandler {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { record, _ ->
            org.apache.kafka.common.TopicPartition(
                "${record.topic()}.DLT",
                record.partition()
            )
        }

        val backOff = ExponentialBackOff().apply {
            initialInterval = 1000L
            multiplier = 2.0
            maxInterval = 10000L
            maxElapsedTime = 60000L
        }

        return DefaultErrorHandler(recoverer, backOff).apply {
            addNotRetryableExceptions(
                IllegalArgumentException::class.java,
                NullPointerException::class.java
            )
        }
    }

    @Bean
    fun optimizedKafkaListenerContainerFactory(
        kafkaTemplate: KafkaTemplate<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, Event> {
        return ConcurrentKafkaListenerContainerFactory<String, Event>().apply {
            setConsumerFactory(optimizedEventConsumerFactory())
            setConcurrency(1)
            setBatchListener(true)
            containerProperties.ackMode = ContainerProperties.AckMode.MANUAL_IMMEDIATE
            containerProperties.isAsyncAcks = true
            containerProperties.listenerTaskExecutor = kafkaConsumerExecutor()
            setCommonErrorHandler(optimizedErrorHandler(kafkaTemplate))
        }
    }
}
