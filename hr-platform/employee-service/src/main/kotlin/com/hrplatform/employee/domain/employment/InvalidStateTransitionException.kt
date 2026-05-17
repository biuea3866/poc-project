package com.hrplatform.employee.domain.employment

class InvalidStateTransitionException(
    from: EmploymentStatus,
    to: EmploymentStatus,
) : IllegalStateException("Employment 상태 전이 불가: $from → $to")
