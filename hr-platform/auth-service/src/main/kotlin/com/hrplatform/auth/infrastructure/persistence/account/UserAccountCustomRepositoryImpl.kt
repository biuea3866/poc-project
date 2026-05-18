package com.hrplatform.auth.infrastructure.persistence.account

import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.QUserAccount
import com.querydsl.core.types.dsl.Expressions
import com.querydsl.jpa.impl.JPAQueryFactory
import java.time.ZonedDateTime

class UserAccountCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : UserAccountCustomRepository {

    private val userAccount = QUserAccount.userAccount

    override fun findByEmploymentId(employmentId: Long): UserAccount? {
        val deletedAtPath = Expressions.dateTimePath(ZonedDateTime::class.java, userAccount, "deletedAt")
        return queryFactory.selectFrom(userAccount)
                           .where(
                               userAccount.employmentId.eq(employmentId),
                               deletedAtPath.isNull,
                           )
                           .fetchOne()
    }
}
