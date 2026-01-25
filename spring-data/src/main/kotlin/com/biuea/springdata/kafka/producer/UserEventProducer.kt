package com.biuea.springdata.kafka.producer

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.User
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class UserEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, Any>
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun send(user: User) {
        kafkaTemplate.send(KafkaTopics.USER_EVENTS, user.id.toString(), user)
    }
}
