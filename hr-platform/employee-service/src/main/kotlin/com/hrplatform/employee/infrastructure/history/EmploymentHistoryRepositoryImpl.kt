package com.hrplatform.employee.infrastructure.history

import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import com.hrplatform.employee.domain.history.QEmploymentHistory
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class EmploymentHistoryRepositoryImpl(
    private val jpaRepository: EmploymentHistoryJpaRepository,
    private val queryFactory: JPAQueryFactory,
) : EmploymentHistoryRepository {

    override fun save(history: EmploymentHistory): EmploymentHistory =
        jpaRepository.save(history)

    override fun findById(id: Long): EmploymentHistory? =
        jpaRepository.findById(id).orElse(null)

    override fun findByEmploymentId(employmentId: Long): List<EmploymentHistory> {
        val employmentHistory = QEmploymentHistory.employmentHistory
        return queryFactory.selectFrom(employmentHistory)
            .where(employmentHistory.employmentId.eq(employmentId))
            .orderBy(
                employmentHistory.effectiveDate.desc(),
                employmentHistory.id.desc(),
            )
            .fetch()
    }

    override fun findLastByEmploymentId(employmentId: Long): EmploymentHistory? =
        findByEmploymentId(employmentId).firstOrNull()
}
