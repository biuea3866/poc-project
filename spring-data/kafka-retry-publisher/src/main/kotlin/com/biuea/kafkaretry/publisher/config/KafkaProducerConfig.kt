package com.biuea.kafkaretry.publisher.config

import com.biuea.kafkaretry.common.constant.RetryTopics
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class KafkaProducerConfig(
    private val objectMapper: ObjectMapper
) {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3,
            ProducerConfig.LINGER_MS_CONFIG to 5,
            ProducerConfig.BATCH_SIZE_CONFIG to 16384
        )
        val serializer = JsonSerializer<Any>(objectMapper).apply {
            setAddTypeInfo(false)
        }
        return DefaultKafkaProducerFactory(configProps, StringSerializer(), serializer)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    @Bean
    fun paymentTopic(): NewTopic {
        return TopicBuilder.name(RetryTopics.PAYMENT_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun orderTopic(): NewTopic {
        return TopicBuilder.name(RetryTopics.ORDER_TOPIC)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun paymentDltTopic(): NewTopic {
        return TopicBuilder.name(RetryTopics.PAYMENT_DLT)
            .partitions(1)
            .replicas(1)
            .build()
    }
}
