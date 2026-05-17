package com.hrplatform.employee.domain.person

import com.hrplatform.core.exception.BusinessException

class PersonNotFoundException(
    personId: Long,
) : BusinessException("PERSON_NOT_FOUND", "Person을 찾을 수 없습니다. id=$personId")
