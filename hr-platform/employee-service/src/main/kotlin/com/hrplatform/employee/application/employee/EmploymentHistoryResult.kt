package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import java.time.LocalDate
import java.time.ZonedDateTime

data class EmploymentHistoryResult(
    val historyId: Long,
    val employmentId: Long,
    val eventType: EmploymentHistoryEventType,
    val effectiveDate: LocalDate,
    val note: String?,
    val cancelledAt: ZonedDateTime?,
) {
    companion object {
        fun of(history: EmploymentHistory): EmploymentHistoryResult = EmploymentHistoryResult(
            historyId = requireNotNull(history.id) { "EmploymentHistory에는 id가 있어야 합니다" },
            employmentId = history.employmentId,
            eventType = history.eventType,
            effectiveDate = history.effectiveDate,
            note = history.note,
            cancelledAt = history.cancelledAt,
        )
    }
}
