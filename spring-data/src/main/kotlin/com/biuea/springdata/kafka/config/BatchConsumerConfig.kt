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
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class BatchConsumerConfig(
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun batchEventConsumerFactory(): ConsumerFactory<String, Event> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "batch-events-consumer",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to 500,
            ConsumerConfig.FETCH_MIN_BYTES_CONFIG to 1048576,
            ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG to 500
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
    fun batchKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Event> {
        return ConcurrentKafkaListenerContainerFactory<String, Event>().apply {
            setConsumerFactory(batchEventConsumerFactory())
            setConcurrency(10)
            setBatchListener(true)
            setCommonErrorHandler(
                DefaultErrorHandler(
                    { record, ex ->
                        log.error("Batch consumer error - topic: {}, value: {}", record?.topic(), record?.value(), ex)
                    },
                    FixedBackOff(1000L, 3L)
                )
            )
        }
    }
}
