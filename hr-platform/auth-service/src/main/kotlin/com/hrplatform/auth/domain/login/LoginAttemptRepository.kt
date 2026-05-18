package com.hrplatform.auth.domain.login

import java.time.ZonedDateTime

interface LoginAttemptRepository {
    fun save(loginAttempt: LoginAttempt): LoginAttempt
    fun countRecentFailures(emailHash: String, since: ZonedDateTime): Int
    fun findRecentByEmailHash(emailHash: String, limit: Int): List<LoginAttempt>
    fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt>
}
