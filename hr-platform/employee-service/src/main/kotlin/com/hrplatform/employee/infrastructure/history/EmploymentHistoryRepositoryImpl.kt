package com.hrplatform.employee.infrastructure.history

import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import org.springframework.stereotype.Repository

@Repository
class EmploymentHistoryRepositoryImpl(
    private val jpaRepository: EmploymentHistoryJpaRepository,
) : EmploymentHistoryRepository {

    override fun save(employmentHistory: EmploymentHistory): EmploymentHistory =
        jpaRepository.saveAndFlush(employmentHistory.toEntity()).toDomain()

    override fun findById(id: Long): EmploymentHistory? =
        jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findByEmploymentId(employmentId: Long): List<EmploymentHistory> =
        jpaRepository.findByEmploymentIdOrdered(employmentId).map { it.toDomain() }

    override fun findLastByEmploymentId(employmentId: Long): EmploymentHistory? =
        jpaRepository.findLastByEmploymentIdOrdered(employmentId)?.toDomain()

    private fun EmploymentHistory.toEntity(): EmploymentHistoryEntity =
        EmploymentHistoryEntity(
            id = this.id,
            employmentId = this.employmentId,
            eventType = this.eventType,
            oldValue = this.oldValue,
            newValue = this.newValue,
            effectiveDate = this.effectiveDate,
            createdByEmploymentId = this.createdByEmploymentId,
            note = this.note,
            cancelledAt = this.cancelledAt,
            createdAt = this.createdAt,
        )

    private fun EmploymentHistoryEntity.toDomain(): EmploymentHistory =
        EmploymentHistory(
            id = requireNotNull(this.id) { "DB에서 조회한 이력의 id는 null일 수 없습니다" },
            employmentId = this.employmentId,
            eventType = this.eventType,
            oldValue = this.oldValue,
            newValue = this.newValue,
            effectiveDate = this.effectiveDate,
            createdByEmploymentId = this.createdByEmploymentId,
            note = this.note,
            cancelledAt = this.cancelledAt,
            createdAt = this.createdAt,
        )
}
