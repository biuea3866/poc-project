package com.hrplatform.auth.infrastructure.persistence.twofactor

import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import org.springframework.data.jpa.repository.JpaRepository

interface TwoFactorBackupCodeJpaRepository :
    JpaRepository<TwoFactorBackupCode, Long>,
    TwoFactorBackupCodeCustomRepository
