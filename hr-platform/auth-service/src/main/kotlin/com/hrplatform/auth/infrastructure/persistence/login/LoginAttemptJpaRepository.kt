package com.hrplatform.auth.infrastructure.persistence.login

import com.hrplatform.auth.domain.login.LoginAttempt
import org.springframework.data.jpa.repository.JpaRepository

interface LoginAttemptJpaRepository :
    JpaRepository<LoginAttempt, Long>,
    LoginAttemptCustomRepository
