package com.hrplatform.auth.infrastructure.persistence.login

import com.hrplatform.auth.domain.login.LoginAttempt
import java.time.ZonedDateTime

interface LoginAttemptCustomRepository {
    fun countRecentFailures(emailHash: String, since: ZonedDateTime): Int
    fun findRecentByEmailHash(emailHash: String, limit: Int): List<LoginAttempt>
    fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt>
}
