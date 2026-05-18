package com.hrplatform.auth.application.auth

import com.hrplatform.auth.domain.auth.service.UserAccountSyncService
import com.hrplatform.core.domain.DomainEventEnvelope
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserAccountSyncUseCase(
    private val userAccountSyncService: UserAccountSyncService,
) {

    @Transactional
    fun syncHired(envelope: DomainEventEnvelope) {
        userAccountSyncService.handleHired(envelope)
    }

    @Transactional
    fun syncResigned(envelope: DomainEventEnvelope) {
        userAccountSyncService.handleResigned(envelope)
    }

    @Transactional
    fun syncSuspended(envelope: DomainEventEnvelope) {
        userAccountSyncService.handleSuspended(envelope)
    }

    @Transactional
    fun syncResumed(envelope: DomainEventEnvelope) {
        userAccountSyncService.handleResumed(envelope)
    }
}
