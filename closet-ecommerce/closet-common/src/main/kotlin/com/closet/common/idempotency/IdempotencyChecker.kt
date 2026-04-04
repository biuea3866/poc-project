package com.closet.common.idempotency

import mu.KotlinLogging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Component
class IdempotencyChecker(
    private val processedEventRepository: ProcessedEventRepository,
) {

    /**
     * 이벤트 멱등성을 보장하며 비즈니스 로직을 실행한다.
     *
     * - 이미 처리된 이벤트인 경우 null을 반환하고 로그를 남긴다.
     * - 미처리 이벤트인 경우 processed_event에 INSERT 후 block()을 실행하고 결과를 반환한다.
     * - INSERT와 비즈니스 로직이 동일 트랜잭션 내에서 실행되므로, 비즈니스 로직 실패 시 INSERT도 롤백된다.
     * - 동시에 동일 이벤트가 수신된 경우 UNIQUE KEY 위반으로 하나만 성공한다.
     *
     * @param eventId 이벤트 고유 식별자
     * @param topic Kafka 토픽명
     * @param consumerGroup Consumer Group 이름
     * @param block 실행할 비즈니스 로직
     * @return 비즈니스 로직 실행 결과. 이미 처리된 이벤트인 경우 null
     */
    @Transactional
    fun <T> process(eventId: String, topic: String, consumerGroup: String, block: () -> T): T? {
        if (processedEventRepository.existsByEventId(eventId)) {
            logger.info { "이미 처리된 이벤트입니다. eventId=$eventId, topic=$topic, consumerGroup=$consumerGroup" }
            return null
        }

        return try {
            val processedEvent = ProcessedEvent.create(
                eventId = eventId,
                topic = topic,
                consumerGroup = consumerGroup,
            )
            processedEventRepository.save(processedEvent)

            val result = block()

            logger.debug { "이벤트 처리 완료. eventId=$eventId, topic=$topic, consumerGroup=$consumerGroup" }
            result
        } catch (e: DataIntegrityViolationException) {
            logger.info { "이벤트 동시 처리 감지 (UNIQUE KEY 위반). eventId=$eventId, topic=$topic, consumerGroup=$consumerGroup" }
            null
        }
    }
}
