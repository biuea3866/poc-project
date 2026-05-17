package com.hrplatform.employee.domain.history

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EmploymentHistoryDomainService(
    private val repository: EmploymentHistoryRepository,
) {
    fun findByEmployment(employmentId: Long): List<EmploymentHistory> =
        repository.findByEmploymentId(employmentId).sortedByDescending { it.effectiveDate }

    fun rebuildAt(employmentId: Long, asOf: LocalDate): Map<String, Any?> {
        val histories = repository.findByEmploymentId(employmentId)
            .filter { it.effectiveDate <= asOf && it.cancelledAt == null }
            .sortedBy { it.effectiveDate }
        val snapshot = mutableMapOf<String, Any?>()
        for (history in histories) {
            snapshot.putAll(history.newValue)
        }
        return snapshot
    }
}
