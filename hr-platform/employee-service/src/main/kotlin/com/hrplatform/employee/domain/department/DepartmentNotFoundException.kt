package com.hrplatform.employee.domain.department

import com.hrplatform.core.exception.BusinessException

class DepartmentNotFoundException : BusinessException(
    errorCode = "DEPARTMENT_NOT_FOUND",
    message = "부서를 찾을 수 없습니다",
)
