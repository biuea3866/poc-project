package com.biuea.kafkaretry.consumer.config

import com.biuea.kafkaretry.consumer.handler.PaymentRetryListener
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.ExponentialBackOff

@Configuration
class PaymentConsumerConfig(
    private val objectMapper: ObjectMapper,
    private val paymentRetryListener: PaymentRetryListener
) {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun paymentConsumerFactory(): ConsumerFactory<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "payment-consumer-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun paymentKafkaListenerContainerFactory(
        kafkaTemplate: KafkaTemplate<String, Any>
    ): ConcurrentKafkaListenerContainerFactory<String, String> {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val backOff = ExponentialBackOff().apply {
            initialInterval = 1000L
            multiplier = 2.0
            maxInterval = 10000L
            maxElapsedTime = 30000L
        }
        val errorHandler = DefaultErrorHandler(recoverer, backOff).apply {
            setRetryListeners(paymentRetryListener)
            addNotRetryableExceptions(IllegalArgumentException::class.java)
        }
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(paymentConsumerFactory())
            setCommonErrorHandler(errorHandler)
        }
    }
}
