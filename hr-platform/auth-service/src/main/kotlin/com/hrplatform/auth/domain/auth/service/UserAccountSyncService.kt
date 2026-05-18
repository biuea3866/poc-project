package com.hrplatform.auth.domain.auth.service

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.domain.DomainEventEnvelope
import com.hrplatform.core.event.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * employee-service 이벤트를 수신하여 UserAccount를 동기화하는 도메인 서비스.
 * EmployeeEventWorker (presentation layer)가 호출하며, UseCase를 통해 @Transactional 적용됨.
 */
@Service
class UserAccountSyncService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    private val logger = LoggerFactory.getLogger(UserAccountSyncService::class.java)

    /**
     * EmployeeHired 이벤트 수신 시 UserAccount 자동 생성하지 않음.
     * PII 정책상 employee-service는 email을 이벤트에 포함하지 않으므로,
     * UserAccount는 HR이 명시적으로 /auth/users/invite API를 호출해야 생성됨.
     * 해당 API 구현은 별도 티켓으로 분리.
     */
    fun handleHired(envelope: DomainEventEnvelope) {
        logger.info(
            "EmployeeHired 수신 — HR의 명시적 invite API 호출 대기. employmentId={}, eventId={}",
            envelope.aggregateId,
            envelope.eventId,
        )
    }

    fun handleResigned(envelope: DomainEventEnvelope) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findByEmploymentId(envelope.aggregateId) ?: return
        if (userAccount.status == UserAccountStatus.DEACTIVATED) return

        userAccount.deactivate("EmployeeResigned", envelope.actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        refreshTokenRepository.revokeAllByUserAccountId(requireNotNull(userAccount.id), "ACCOUNT_DEACTIVATED", now)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun handleSuspended(envelope: DomainEventEnvelope) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findByEmploymentId(envelope.aggregateId) ?: return
        if (userAccount.status == UserAccountStatus.SUSPENDED) return

        val reason = envelope.action.details["reason"] as? String ?: "EmployeeSuspended"
        userAccount.suspend(reason, envelope.actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }

    fun handleResumed(envelope: DomainEventEnvelope) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val userAccount = userAccountRepository.findByEmploymentId(envelope.aggregateId) ?: return
        if (userAccount.status == UserAccountStatus.ACTIVE) return

        userAccount.reactivate(envelope.actorEmploymentId, now)
        userAccountRepository.save(userAccount)
        eventPublisher.publishAll(userAccount.pullDomainEvents())
    }
}
