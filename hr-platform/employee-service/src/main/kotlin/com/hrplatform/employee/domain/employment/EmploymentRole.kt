package com.hrplatform.employee.domain.employment

/**
 * Employment의 역할(Role).
 * HR_MANAGER 이상은 전사 전체 직원 조회 권한을 가진다.
 * TEAM_LEAD는 자신이 속한 부서 트리 하위 직원만 조회한다.
 * EMPLOYEE는 자기 자신만 조회한다.
 */
enum class EmploymentRole {
    EMPLOYEE,
    TEAM_LEAD,
    HR_MANAGER,
    ADMIN,
    ;

    fun isAtLeast(role: EmploymentRole): Boolean = ordinal >= role.ordinal
}
