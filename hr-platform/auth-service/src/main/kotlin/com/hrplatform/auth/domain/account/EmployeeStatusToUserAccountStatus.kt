package com.hrplatform.auth.domain.account

/**
 * employee-service 이벤트 타입 → UserAccountStatus 매핑 value object.
 * ADR-003 §9 누락 요구사항.
 */
object EmployeeStatusToUserAccountStatus {

    fun mapHiredEvent(): UserAccountStatus = UserAccountStatus.ACTIVE

    fun mapResignedEvent(): UserAccountStatus = UserAccountStatus.DEACTIVATED

    fun mapSuspendedEvent(): UserAccountStatus = UserAccountStatus.SUSPENDED

    fun mapResumedEvent(): UserAccountStatus = UserAccountStatus.ACTIVE
}
