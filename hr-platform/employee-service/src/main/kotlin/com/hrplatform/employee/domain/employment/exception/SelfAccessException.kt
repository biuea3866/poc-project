package com.hrplatform.employee.domain.employment.exception

import com.hrplatform.core.exception.BusinessException

class SelfAccessException(
    employmentId: Long,
) : BusinessException(
    errorCode = "EMPLOYMENT_SELF_ACCESS",
    message = "자기 자신에 대한 접근 금지 작업: employmentId=$employmentId",
)
