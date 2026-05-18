package com.hrplatform.auth.presentation.consumer

import com.hrplatform.auth.application.auth.UserAccountSyncUseCase
import com.hrplatform.core.domain.DomainEventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * employee-service 이벤트 수신 Consumer.
 * - DTO 직접 매핑으로 수신
 * - 비즈니스 로직 없음, UseCase 위임만 수행
 * - eventId 기반 Redis SETNX로 멱등성 보장 (TTL 7일)
 */
@Component
@Profile("!test & !test-integration")
class EmployeeEventWorker(
    private val userAccountSyncUseCase: UserAccountSyncUseCase,
    private val redisTemplate: StringRedisTemplate,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val IDEMPOTENCY_KEY_PREFIX = "auth:idem:employee:"
        private val IDEMPOTENCY_TTL = Duration.ofDays(7)
    }

    @KafkaListener(
        topics = ["\${hrplatform.kafka.topics.employee:event.hr.employee.v1}"],
        groupId = "\${hrplatform.kafka.consumer-groups.employee-sync:auth-service.employee.v1}",
        containerFactory = "employeeEventContainerFactory",
    )
    fun consume(envelope: DomainEventEnvelope) {
        if (!acquireIdempotencyLock(envelope.eventId)) {
            logger.warn("중복 이벤트 무시: eventId={}, eventType={}", envelope.eventId, envelope.eventType)
            return
        }

        logger.info("Received employee event: {} for aggregate {}", envelope.eventType, envelope.aggregateId)
        try {
            when (envelope.eventType) {
                "EmployeeHired" -> userAccountSyncUseCase.syncHired(envelope)
                "EmployeeResigned" -> userAccountSyncUseCase.syncResigned(envelope)
                "EmployeeSuspended" -> userAccountSyncUseCase.syncSuspended(envelope)
                "EmployeeResumed" -> userAccountSyncUseCase.syncResumed(envelope)
                else -> logger.debug("Ignoring unhandled employee event type: {}", envelope.eventType)
            }
        } catch (exception: IllegalStateException) {
            releaseIdempotencyLock(envelope.eventId)
            throw exception
        }
    }

    private fun acquireIdempotencyLock(eventId: String): Boolean {
        val key = "$IDEMPOTENCY_KEY_PREFIX$eventId"
        return redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENCY_TTL) == true
    }

    private fun releaseIdempotencyLock(eventId: String) {
        val key = "$IDEMPOTENCY_KEY_PREFIX$eventId"
        redisTemplate.delete(key)
    }
}
