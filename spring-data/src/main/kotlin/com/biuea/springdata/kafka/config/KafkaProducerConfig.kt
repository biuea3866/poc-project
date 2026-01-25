package com.biuea.springdata.kafka.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
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
            ProducerConfig.BATCH_SIZE_CONFIG to 16384,
            ProducerConfig.BUFFER_MEMORY_CONFIG to 33554432,
            ProducerConfig.COMPRESSION_TYPE_CONFIG to "lz4"
        )
        
        val serializer = JsonSerializer<Any>(objectMapper).apply {
            setAddTypeInfo(false)
        }
        
        return DefaultKafkaProducerFactory(
            configProps,
            StringSerializer(),
            serializer
        )
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory())
    }

    // Topic definitions
    @Bean
    fun userEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS)
            .partitions(3)
            .replicas(1)
            .build()
    }

    @Bean
    fun normalEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.NORMAL_EVENTS)
            .partitions(10)
            .replicas(1)
            .build()
    }

    @Bean
    fun batchEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.BATCH_EVENTS)
            .partitions(10)
            .replicas(1)
            .build()
    }

    @Bean
    fun optimizedEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.OPTIMIZED_EVENTS)
            .partitions(1)
            .replicas(1)
            .build()
    }

    // DLT Topics
    @Bean
    fun normalEventsDltTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.NORMAL_EVENTS_DLT)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun batchEventsDltTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.BATCH_EVENTS_DLT)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun optimizedEventsDltTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.OPTIMIZED_EVENTS_DLT)
            .partitions(1)
            .replicas(1)
            .build()
    }

    @Bean
    fun parallelEventsTopic(): NewTopic {
        return TopicBuilder.name(KafkaTopics.PARALLEL_EVENTS)
            .partitions(1)
            .replicas(1)
            .build()
    }
}
