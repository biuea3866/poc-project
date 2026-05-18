package com.hrplatform.auth.infrastructure.persistence.twofactor

import com.hrplatform.auth.domain.twofactor.QTwoFactorBackupCode
import com.hrplatform.auth.domain.twofactor.TwoFactorBackupCode
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.ZonedDateTime

class TwoFactorBackupCodeCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : TwoFactorBackupCodeCustomRepository {

    private val twoFactorBackupCode = QTwoFactorBackupCode.twoFactorBackupCode

    override fun findUnused(userAccountId: Long): List<TwoFactorBackupCode> {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, twoFactorBackupCode, "deletedAt")
        return queryFactory.selectFrom(twoFactorBackupCode)
                           .where(
                               twoFactorBackupCode.userAccountId.eq(userAccountId),
                               twoFactorBackupCode.usedAt.isNull,
                               deletedAtPath.isNull,
                           )
                           .fetch()
    }

    override fun deleteAllByUserAccountId(userAccountId: Long, deletedBy: Long?, now: ZonedDateTime) {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, twoFactorBackupCode, "deletedAt")
        val codes = queryFactory.selectFrom(twoFactorBackupCode)
                                .where(
                                    twoFactorBackupCode.userAccountId.eq(userAccountId),
                                    deletedAtPath.isNull,
                                )
                                .fetch()
        codes.forEach { it.softDelete(now, deletedBy) }
    }
}
