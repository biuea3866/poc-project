package com.hrplatform.employee.domain.employment.exception

import com.hrplatform.core.exception.BusinessException

class CrossCompanyAccessException(
    viewerCompanyId: Long,
    targetCompanyId: Long,
) : BusinessException(
    errorCode = "EMPLOYMENT_CROSS_COMPANY_ACCESS",
    message = "다른 회사 고용 정보에 접근 불가: viewer.companyId=$viewerCompanyId, target.companyId=$targetCompanyId",
)
