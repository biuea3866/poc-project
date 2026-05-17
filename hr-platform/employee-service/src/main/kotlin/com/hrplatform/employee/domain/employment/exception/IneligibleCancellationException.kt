package com.hrplatform.employee.domain.employment.exception

import com.hrplatform.core.exception.BusinessException

class IneligibleCancellationException(
    message: String,
) : BusinessException(
    errorCode = "EMPLOYMENT_INELIGIBLE_CANCELLATION",
    message = message,
)
