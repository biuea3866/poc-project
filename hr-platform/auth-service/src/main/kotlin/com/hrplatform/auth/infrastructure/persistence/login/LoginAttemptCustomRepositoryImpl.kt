package com.hrplatform.auth.infrastructure.persistence.login

import com.hrplatform.auth.domain.login.LoginAttempt
import com.hrplatform.auth.domain.login.QLoginAttempt
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.ZonedDateTime

class LoginAttemptCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : LoginAttemptCustomRepository {

    private val loginAttempt = QLoginAttempt.loginAttempt

    override fun countRecentFailures(emailHash: String, since: ZonedDateTime): Int =
        queryFactory.selectFrom(loginAttempt)
                    .where(
                        loginAttempt.emailHash.eq(emailHash),
                        loginAttempt.success.isFalse,
                        loginAttempt.attemptedAt.goe(since),
                    )
                    .fetch()
                    .size

    override fun findRecentByEmailHash(emailHash: String, limit: Int): List<LoginAttempt> =
        queryFactory.selectFrom(loginAttempt)
                    .where(loginAttempt.emailHash.eq(emailHash))
                    .orderBy(loginAttempt.attemptedAt.desc())
                    .limit(limit.toLong())
                    .fetch()

    override fun findRecentByUserAccountId(userAccountId: Long, limit: Int): List<LoginAttempt> =
        queryFactory.selectFrom(loginAttempt)
                    .where(loginAttempt.userAccountId.eq(userAccountId))
                    .orderBy(loginAttempt.attemptedAt.desc())
                    .limit(limit.toLong())
                    .fetch()
}
