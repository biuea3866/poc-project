package com.hrplatform.auth.domain.auth.service

import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.token.JtiBlacklist
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.domain.DomainEventEnvelope
import com.hrplatform.core.event.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
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
    private val jtiBlacklist: JtiBlacklist,
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

        val userAccountId = requireNotNull(userAccount.id)
        val activeTokens = refreshTokenRepository.findActiveByUserAccountId(userAccountId)
        val jtis = activeTokens.mapNotNull { it.accessJti }
        if (jtis.isNotEmpty()) {
            jtiBlacklist.addAll(jtis, Duration.ofHours(2))
        }
        refreshTokenRepository.revokeAllByUserAccountId(userAccountId, "ACCOUNT_DEACTIVATED", now)
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

    /**
     * EmployeeTransferred 이벤트 수신 시 UserAccount.departmentId 갱신.
     * JWT did claim 캐시를 최신 상태로 유지. 다음 로그인/refresh 시 새 departmentId가 토큰에 반영된다.
     */
    fun handleTransferred(envelope: DomainEventEnvelope) {
        val userAccount = userAccountRepository.findByEmploymentId(envelope.aggregateId) ?: return
        val newDepartmentId = (envelope.action.details["departmentId"] as? Number)?.toLong()
        userAccount.updateDepartment(newDepartmentId)
        userAccountRepository.save(userAccount)
    }
}
