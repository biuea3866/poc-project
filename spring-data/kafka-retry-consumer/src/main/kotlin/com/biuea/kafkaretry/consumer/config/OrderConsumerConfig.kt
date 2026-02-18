package com.biuea.kafkaretry.consumer.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
class OrderConsumerConfig(
    private val objectMapper: ObjectMapper
) {
    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun orderConsumerFactory(): ConsumerFactory<String, String> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "order-consumer-group",
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java
        )
        return DefaultKafkaConsumerFactory(props)
    }

    @Bean
    fun orderKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, String> {
        return ConcurrentKafkaListenerContainerFactory<String, String>().apply {
            setConsumerFactory(orderConsumerFactory())
        }
    }

    @Bean
    fun retryTopicKafkaTemplate(): KafkaTemplate<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.RETRIES_CONFIG to 3
        )
        val serializer = JsonSerializer<Any>(objectMapper).apply {
            setAddTypeInfo(false)
        }
        val factory = DefaultKafkaProducerFactory(configProps, StringSerializer(), serializer)
        return KafkaTemplate(factory)
    }
}
