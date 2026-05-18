package com.hrplatform.auth.domain.account

/**
 * UserAccount 상태 머신.
 *
 * 전이 표:
 *   ACTIVE      → LOCKED (5회 실패)
 *   ACTIVE      → SUSPENDED (employee.suspended 수신)
 *   ACTIVE      → DEACTIVATED (employee.resigned 수신)
 *   LOCKED      → ACTIVE (수동 해제 또는 시간 만료 자동 해제)
 *   SUSPENDED   → ACTIVE (employee.resumed 수신)
 *   SUSPENDED   → DEACTIVATED (employee.resigned 수신)
 *   DEACTIVATED → [*] (재활성화 불가)
 */
enum class UserAccountStatus {
    ACTIVE,
    LOCKED,
    SUSPENDED,
    DEACTIVATED,
    ;

    fun canTransitTo(target: UserAccountStatus): Boolean = when (this) {
        ACTIVE -> target == LOCKED || target == SUSPENDED || target == DEACTIVATED
        LOCKED -> target == ACTIVE
        SUSPENDED -> target == ACTIVE || target == DEACTIVATED
        DEACTIVATED -> false
    }
}
