package com.hrplatform.employee.domain.department.exception

import com.hrplatform.core.exception.BusinessException

class IneligibleHeadException(
    employmentId: Long,
    reason: String,
) : BusinessException(
    errorCode = "DEPARTMENT_INELIGIBLE_HEAD",
    message = "Employment($employmentId) cannot be assigned as department head: $reason",
)
