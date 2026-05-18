package com.hrplatform.auth.domain.auth.service

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountRepository
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.domain.account.event.UserCreatedEvent
import com.hrplatform.auth.domain.role.RoleCode
import com.hrplatform.auth.domain.role.RoleRepository
import com.hrplatform.auth.domain.role.UserAccountRole
import com.hrplatform.auth.domain.role.UserAccountRoleRepository
import com.hrplatform.auth.domain.token.RefreshTokenRepository
import com.hrplatform.core.domain.DomainEventEnvelope
import com.hrplatform.core.event.DomainEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

/**
 * employee-service 이벤트를 수신하여 UserAccount를 동기화하는 도메인 서비스.
 * EmployeeEventWorker (presentation layer)가 호출하며, UseCase를 통해 @Transactional 적용됨.
 */
@Service
class UserAccountSyncService(
    private val userAccountRepository: UserAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val roleRepository: RoleRepository,
    private val userAccountRoleRepository: UserAccountRoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: DomainEventPublisher,
) {

    fun handleHired(envelope: DomainEventEnvelope) {
        val now = ZonedDateTime.now(ZoneOffset.UTC)
        val employmentId = envelope.aggregateId
        val companyId = envelope.companyId
        val email = envelope.action.details["email"] as? String
            ?: error("EmployeeHired 이벤트에 email 누락. eventId=${envelope.eventId}, aggregateId=${envelope.aggregateId}")

        val existing = userAccountRepository.findByEmploymentId(employmentId)
        if (existing != null) return

        val tempPassword = UUID.randomUUID().toString()
        val userAccount = UserAccount.create(
            employmentId = employmentId,
            companyId = companyId,
            email = email,
            passwordHash = passwordEncoder.encode(tempPassword),
        )
        val saved = userAccountRepository.save(userAccount)

        val employeeRole = roleRepository.findByCompanyIdAndCode(0L, RoleCode.EMPLOYEE.name)
        if (employeeRole != null) {
            userAccountRoleRepository.save(
                UserAccountRole(
                    userAccountId = requireNotNull(saved.id),
                    roleId = requireNotNull(employeeRole.id),
                    assignedAt = now,
                    assignedBy = null,
                ),
            )
        }

        val createdEvent = UserCreatedEvent(
            userAccountId = requireNotNull(saved.id),
            companyIdValue = companyId,
            employmentId = employmentId,
            email = email,
            defaultRoleCode = RoleCode.EMPLOYEE.name,
            actorEmploymentId = envelope.actorEmploymentId,
            occurredAt = now,
        )
        eventPublisher.publish(createdEvent)
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
