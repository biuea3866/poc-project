package com.hrplatform.employee.infrastructure.history

import com.querydsl.jpa.impl.JPAQueryFactory

class EmploymentHistoryCustomRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : EmploymentHistoryCustomRepository {

    private val history = QEmploymentHistoryEntity.employmentHistoryEntity

    override fun findByEmploymentIdOrdered(employmentId: Long): List<EmploymentHistoryEntity> =
        queryFactory.selectFrom(history)
            .where(history.employmentId.eq(employmentId))
            .orderBy(history.effectiveDate.desc(), history.id.desc())
            .fetch()

    override fun findLastByEmploymentIdOrdered(employmentId: Long): EmploymentHistoryEntity? =
        queryFactory.selectFrom(history)
            .where(history.employmentId.eq(employmentId))
            .orderBy(history.effectiveDate.desc(), history.id.desc())
            .limit(1)
            .fetchOne()
}
