package com.hrplatform.employee.domain.history

interface EmploymentHistoryRepository {

    fun save(history: EmploymentHistory): EmploymentHistory

    fun findById(id: Long): EmploymentHistory?

    /**
     * 특정 Employment의 발령 이력을 effectiveDate DESC + id DESC 순으로 반환한다.
     * tie-breaker: 같은 effectiveDate 내에서는 id DESC (나중에 생성된 이력이 우선).
     */
    fun findByEmploymentId(employmentId: Long): List<EmploymentHistory>

    /**
     * 발령 취소 검증용 — effectiveDate DESC + id DESC 기준 직전 1건만 반환한다.
     * 존재하지 않으면 null을 반환한다.
     */
    fun findLastByEmploymentId(employmentId: Long): EmploymentHistory?
}
