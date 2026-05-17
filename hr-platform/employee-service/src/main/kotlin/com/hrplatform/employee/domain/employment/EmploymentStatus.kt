package com.hrplatform.employee.domain.employment

/**
 * Employment 상태 머신.
 *
 * ADR-002 §4 전이 표:
 *   PRE_HIRED → ACTIVE (activate)
 *   ACTIVE    → ON_LEAVE (suspend)
 *   ACTIVE    → RESIGNED (resign)
 *   ON_LEAVE  → ACTIVE (resume)
 *   ON_LEAVE  → RESIGNED (resign)
 *   RESIGNED  → [*] (재입사는 새 Employment 생성)
 */
enum class EmploymentStatus {
    PRE_HIRED,
    ACTIVE,
    ON_LEAVE,
    RESIGNED,
    ;

    fun canTransitTo(target: EmploymentStatus): Boolean = when (this) {
        PRE_HIRED -> target == ACTIVE
        ACTIVE -> target == ON_LEAVE || target == RESIGNED
        ON_LEAVE -> target == ACTIVE || target == RESIGNED
        RESIGNED -> false
    }
}
