package com.hrplatform.auth.infrastructure.kafka

import com.hrplatform.core.domain.DomainEventEnvelope
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@Profile("!test & !test-integration & !local")
class AuthKafkaConfig {

    @Bean
    fun producerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
    ): ProducerFactory<String, String> {
        val configs = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.ACKS_CONFIG to "all",
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "lz4",
            ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG to true,
        )
        return DefaultKafkaProducerFactory(configs)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, String>): KafkaTemplate<String, String> =
        KafkaTemplate(producerFactory)

    @Bean
    fun employeeEventConsumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
        @Value("\${hrplatform.kafka.consumer-groups.employee-sync:auth-service.employee.v1}") groupId: String,
    ): ConsumerFactory<String, DomainEventEnvelope> {
        val configs = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false,
            JsonDeserializer.TRUSTED_PACKAGES to "com.hrplatform.core.domain",
            JsonDeserializer.VALUE_DEFAULT_TYPE to DomainEventEnvelope::class.java.name,
            JsonDeserializer.USE_TYPE_INFO_HEADERS to false,
        )
        return DefaultKafkaConsumerFactory(configs)
    }

    @Bean
    fun employeeEventContainerFactory(
        employeeEventConsumerFactory: ConsumerFactory<String, DomainEventEnvelope>,
        kafkaTemplate: KafkaTemplate<String, String>,
        @Value("\${hrplatform.kafka.dlq.employee:event.hr.employee.v1.dlq}") dlqTopic: String,
    ): ConcurrentKafkaListenerContainerFactory<String, DomainEventEnvelope> {
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate) { _, _ ->
            org.apache.kafka.common.TopicPartition(dlqTopic, 0)
        }
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(1000L, 3L))
        val factory = ConcurrentKafkaListenerContainerFactory<String, DomainEventEnvelope>()
        factory.consumerFactory = employeeEventConsumerFactory
        factory.setCommonErrorHandler(errorHandler)
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }
}
