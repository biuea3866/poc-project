package com.hrplatform.employee.domain.employment.exception

import com.hrplatform.core.exception.BusinessException

class InvalidStateTransitionException(
    currentStatus: String,
    targetStatus: String,
) : BusinessException(
    errorCode = "EMPLOYMENT_INVALID_STATE_TRANSITION",
    message = "고용 상태 전이 불가: $currentStatus → $targetStatus",
)
