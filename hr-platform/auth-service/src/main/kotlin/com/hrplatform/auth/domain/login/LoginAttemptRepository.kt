package com.hrplatform.auth.domain.login

import java.time.ZonedDateTime

interface LoginAttemptRepository {
    fun save(loginAttempt: LoginAttempt): LoginAttempt
    fun countRecentFailures(email: String, since: ZonedDateTime): Int
    fun findRecentByEmail(email: String, limit: Int): List<LoginAttempt>
    fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt>
}
