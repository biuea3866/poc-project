package com.biuea.springdata.kafka.listener

import com.biuea.springdata.kafka.config.KafkaTopics
import com.biuea.springdata.kafka.dto.User
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile

// 파티셔닝 재검증(rebench)은 MySQL 만 쓴다. 브로커가 없으면 컨슈머 기동에서 막히고,
// 접속 재시도가 측정 구간의 CPU 와 로그를 잠식한다.
@Profile("!rebench")
@Component
@ConditionalOnProperty(name = ["spring.kafka.enabled"], havingValue = "true", matchIfMissing = true)
class UserEventListener {

    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.USER_EVENTS],
        groupId = "user-events-consumer",
        containerFactory = "userKafkaListenerContainerFactory"
    )
    fun listen(record: ConsumerRecord<String, User>) {
        val user = record.value()
        log.info(
            "Received user event - partition: {}, offset: {}, user: [id={}, name={}, email={}]",
            record.partition(),
            record.offset(),
            user.id,
            user.name,
            user.email
        )
    }
}
