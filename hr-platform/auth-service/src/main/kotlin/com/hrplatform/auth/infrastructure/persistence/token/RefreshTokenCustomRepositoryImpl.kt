package com.hrplatform.auth.infrastructure.persistence.token

import com.hrplatform.auth.domain.token.QRefreshToken
import com.hrplatform.auth.domain.token.RefreshToken
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.ZonedDateTime

class RefreshTokenCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : RefreshTokenCustomRepository {

    private val refreshToken = QRefreshToken.refreshToken

    override fun findActiveByUserAccountId(userAccountId: Long): List<RefreshToken> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, refreshToken, "deletedAt")
        return queryFactory.selectFrom(refreshToken)
                           .where(
                               refreshToken.userAccountId.eq(userAccountId),
                               refreshToken.revokedAt.isNull,
                               deletedAtPath.isNull,
                           )
                           .fetch()
    }

    override fun revokeAllByUserAccountId(userAccountId: Long, reason: String, now: ZonedDateTime) {
        val activeTokens = findActiveByUserAccountId(userAccountId)
        activeTokens.forEach { it.revoke(reason, now) }
    }
}
