package com.hrplatform.auth.infrastructure.persistence.login

import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.LoginAttemptRepository
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class LoginAttemptRepositoryImpl(
    private val jpaRepository: LoginAttemptJpaRepository,
) : LoginAttemptRepository {

    override fun save(loginAttempt: LoginAttempt): LoginAttempt =
        jpaRepository.save(loginAttempt)

    override fun countRecentFailures(email: String, since: ZonedDateTime): Int =
        jpaRepository.countRecentFailures(email, since)

    override fun findRecentByEmail(email: String, limit: Int): List<LoginAttempt> =
        jpaRepository.findRecentByEmail(email, limit)

    override fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt> =
        jpaRepository.findRecentByUserAccountId(userAccountId, limit)
}
