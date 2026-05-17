package com.hrplatform.employee.domain.department

import com.hrplatform.core.exception.BusinessException

class IneligibleHeadException : BusinessException(
    errorCode = "DEPARTMENT_INELIGIBLE_HEAD",
    message = "ACTIVE 상태의 직원만 부서장으로 지정할 수 있습니다",
)
