package com.biuea.springdata.kafka.config

import com.biuea.springdata.kafka.dto.User
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
import org.springframework.kafka.listener.CommonErrorHandler
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
import org.springframework.util.backoff.FixedBackOff

@Configuration
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class KafkaConsumerConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun userConsumerFactory(): ConsumerFactory<String, User> {
        val configProps = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "user-events-consumer",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ErrorHandlingDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to true,
            ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS to "org.springframework.kafka.support.serializer.JsonDeserializer",
            "spring.json.trusted.packages" to "*",
            "spring.json.value.default.type" to User::class.java.name,
            "spring.json.use.type.headers" to false
        )

        return DefaultKafkaConsumerFactory(configProps)
    }

    @Bean
    fun userKafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, User> {
        return ConcurrentKafkaListenerContainerFactory<String, User>().apply {
            setConsumerFactory(userConsumerFactory())
            setCommonErrorHandler(defaultErrorHandler())
        }
    }

    @Bean
    fun defaultErrorHandler(): CommonErrorHandler {
        return DefaultErrorHandler(
            { record, exception ->
                log.error(
                    "Error processing record - topic: {}, partition: {}, offset: {}, value: {}",
                    record?.topic(),
                    record?.partition(),
                    record?.offset(),
                    record?.value(),
                    exception
                )
            },
            FixedBackOff(1000L, 3L)
        )
    }
}
