package com.hrplatform.employee.application.employee

import com.hrplatform.employee.domain.query.SearchCriteria
import org.springframework.data.domain.Pageable

data class SearchEmployeesCommand(
    val viewerEmploymentId: Long,
    val companyId: Long,
    val keyword: String?,
    val departmentId: Long?,
    val pageable: Pageable,
) {
    fun toCriteria(): SearchCriteria = SearchCriteria(
        companyId = companyId,
        keyword = keyword,
        departmentId = departmentId,
    )
}
