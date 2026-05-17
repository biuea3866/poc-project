package com.hrplatform.employee.domain.department.exception

import com.hrplatform.core.exception.BusinessException

class CircularDepartmentException(
    departmentId: Long,
    targetParentId: Long,
) : BusinessException(
    errorCode = "DEPARTMENT_CIRCULAR_REFERENCE",
    message = "Department($departmentId) cannot be moved under Department($targetParentId): circular reference detected.",
)
