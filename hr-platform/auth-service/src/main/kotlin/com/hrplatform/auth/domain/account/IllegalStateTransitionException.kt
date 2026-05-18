package com.hrplatform.auth.domain.account

import com.hrplatform.core.exception.BusinessException

class IllegalStateTransitionException(
    from: UserAccountStatus,
    to: UserAccountStatus,
) : BusinessException(
    errorCode = "ILLEGAL_STATE_TRANSITION",
    message = "상태 전이 불가: $from → $to",
)
