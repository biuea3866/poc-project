package com.hrplatform.employee.infrastructure.history

interface EmploymentHistoryCustomRepository {

    /**
     * 특정 Employment의 발령 이력을 effectiveDate DESC + id DESC 순으로 반환한다.
     */
    fun findByEmploymentIdOrdered(employmentId: Long): List<EmploymentHistoryEntity>

    /**
     * effectiveDate DESC + id DESC 기준 직전 1건만 반환한다.
     * 존재하지 않으면 null을 반환한다.
     */
    fun findLastByEmploymentIdOrdered(employmentId: Long): EmploymentHistoryEntity?
}
