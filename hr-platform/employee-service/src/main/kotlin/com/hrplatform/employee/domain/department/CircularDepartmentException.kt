package com.hrplatform.employee.domain.department

import com.hrplatform.core.exception.BusinessException

class CircularDepartmentException : BusinessException(
    errorCode = "DEPARTMENT_CIRCULAR_REFERENCE",
    message = "순환 참조가 되는 부모 부서로 이동할 수 없습니다",
)
