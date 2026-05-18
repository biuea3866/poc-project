package com.hrplatform.auth.presentation.consumer

import com.hrplatform.auth.application.auth.UserAccountSyncUseCase
import com.hrplatform.core.domain.DomainEventEnvelope
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * employee-service 이벤트 수신 Consumer.
 * - DTO 직접 매핑으로 수신
 * - 비즈니스 로직 없음, UseCase 위임만 수행
 */
@Component
class EmployeeEventWorker(
    private val userAccountSyncUseCase: UserAccountSyncUseCase,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${hrplatform.kafka.topics.employee:event.hr.employee.v1}"],
        groupId = "\${hrplatform.kafka.consumer-groups.employee-sync:auth-service.employee.v1}",
        containerFactory = "employeeEventContainerFactory",
    )
    fun consume(envelope: DomainEventEnvelope) {
        logger.info("Received employee event: {} for aggregate {}", envelope.eventType, envelope.aggregateId)
        when (envelope.eventType) {
            "EmployeeHired" -> userAccountSyncUseCase.syncHired(envelope)
            "EmployeeResigned" -> userAccountSyncUseCase.syncResigned(envelope)
            "EmployeeSuspended" -> userAccountSyncUseCase.syncSuspended(envelope)
            "EmployeeResumed" -> userAccountSyncUseCase.syncResumed(envelope)
            else -> logger.debug("Ignoring unhandled employee event type: {}", envelope.eventType)
        }
    }
}
