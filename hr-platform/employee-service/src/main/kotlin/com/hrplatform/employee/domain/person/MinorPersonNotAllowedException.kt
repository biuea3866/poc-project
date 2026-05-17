package com.hrplatform.employee.domain.person

import com.hrplatform.core.exception.BusinessException

class MinorPersonNotAllowedException(
    message: String = "만 18세 미만은 입사 등록이 불가합니다.",
) : BusinessException(
    errorCode = "PERSON_001",
    message = message,
)
