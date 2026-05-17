package com.hrplatform.employee.domain.person

import com.hrplatform.core.exception.BusinessException

class MinorPersonNotAllowedException : BusinessException("PERSON_001", "미성년자는 등록할 수 없습니다")
