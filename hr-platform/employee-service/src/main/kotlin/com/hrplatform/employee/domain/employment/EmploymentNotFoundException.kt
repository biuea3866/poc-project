package com.hrplatform.employee.domain.employment

import com.hrplatform.core.exception.BusinessException

class EmploymentNotFoundException(
    employmentId: Long,
) : BusinessException("EMPLOYMENT_NOT_FOUND", "Employment을 찾을 수 없습니다. id=$employmentId")
