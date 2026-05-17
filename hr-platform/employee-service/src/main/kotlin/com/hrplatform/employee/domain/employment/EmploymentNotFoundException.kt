package com.hrplatform.employee.domain.employment

import com.hrplatform.core.exception.BusinessException

class EmploymentNotFoundException : BusinessException(
    errorCode = "EMPLOYMENT_NOT_FOUND",
    message = "직원 고용 정보를 찾을 수 없습니다",
)
