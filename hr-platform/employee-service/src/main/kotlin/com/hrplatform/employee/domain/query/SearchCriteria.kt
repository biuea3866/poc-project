package com.hrplatform.employee.domain.query

data class SearchCriteria(
    val companyId: Long,
    val keyword: String?,
    val departmentId: Long?,
)
