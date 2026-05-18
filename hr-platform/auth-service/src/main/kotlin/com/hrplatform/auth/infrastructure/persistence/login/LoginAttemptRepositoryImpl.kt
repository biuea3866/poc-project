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

    override fun countRecentFailures(emailHash: String, since: ZonedDateTime): Int =
        jpaRepository.countRecentFailures(emailHash, since)

    override fun findRecentByEmailHash(emailHash: String, limit: Int): List<LoginAttempt> =
        jpaRepository.findRecentByEmailHash(emailHash, limit)

    override fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt> =
        jpaRepository.findRecentByUserAccountId(userAccountId, limit)
}
