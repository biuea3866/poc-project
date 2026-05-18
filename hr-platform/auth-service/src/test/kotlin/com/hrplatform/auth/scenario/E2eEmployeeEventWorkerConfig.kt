package com.hrplatform.auth.scenario

import com.hrplatform.auth.application.auth.UserAccountSyncUseCase
import com.hrplatform.core.domain.DomainEventEnvelope
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer

/**
 * E2E 시나리오 테스트 전용 EmployeeEventWorker 설정.
 * EmployeeEventWorker는 @Profile("!test-integration")으로 비활성화되므로
 * E2E 테스트에서 Kafka Consumer를 통한 풀스택 검증을 위해 별도 Bean을 등록한다.
 */
@TestConfiguration
class E2eEmployeeEventWorkerConfig {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun employeeEventConsumerFactory(
        @Value("\${spring.kafka.bootstrap-servers}") bootstrapServers: String,
    ): ConsumerFactory<String, DomainEventEnvelope> {
        val configs = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to "e2e-auth-service-employee-sync",
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
    ): ConcurrentKafkaListenerContainerFactory<String, DomainEventEnvelope> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, DomainEventEnvelope>()
        factory.consumerFactory = employeeEventConsumerFactory
        factory.containerProperties.ackMode = ContainerProperties.AckMode.RECORD
        return factory
    }

    @Bean
    fun e2eEmployeeEventWorker(
        userAccountSyncUseCase: UserAccountSyncUseCase,
    ): E2eEmployeeEventWorker = E2eEmployeeEventWorker(userAccountSyncUseCase)
}

/**
 * E2E 전용 EmployeeEventWorker — Redis 멱등성 체크 없이 순수하게 UseCase 위임만 수행.
 * 원본 EmployeeEventWorker는 test-integration 프로파일에서 비활성화되므로 E2E 전용으로 분리.
 */
class E2eEmployeeEventWorker(
    private val userAccountSyncUseCase: UserAccountSyncUseCase,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @org.springframework.kafka.annotation.KafkaListener(
        topics = ["\${hrplatform.kafka.topics.employee:event.hr.employee.v1}"],
        groupId = "e2e-auth-service-employee-sync",
        containerFactory = "employeeEventContainerFactory",
    )
    fun consume(envelope: DomainEventEnvelope) {
        logger.info("E2E EmployeeEventWorker received: {} for aggregate {}", envelope.eventType, envelope.aggregateId)
        when (envelope.eventType) {
            "EmployeeHired" -> userAccountSyncUseCase.syncHired(envelope)
            "EmployeeResigned" -> userAccountSyncUseCase.syncResigned(envelope)
            "EmployeeSuspended" -> userAccountSyncUseCase.syncSuspended(envelope)
            "EmployeeResumed" -> userAccountSyncUseCase.syncResumed(envelope)
            else -> logger.debug("Ignoring unhandled employee event type: {}", envelope.eventType)
        }
    }
}
