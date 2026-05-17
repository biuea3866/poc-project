package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentStatus

data class CancelEmploymentEventResult(
    val employmentId: Long,
    val status: EmploymentStatus,
) {
    companion object {
        fun of(employment: Employment): CancelEmploymentEventResult = CancelEmploymentEventResult(
            employmentId = requireNotNull(employment.id) { "저장된 Employment에는 id가 있어야 합니다" },
            status = employment.status,
        )
    }
}
