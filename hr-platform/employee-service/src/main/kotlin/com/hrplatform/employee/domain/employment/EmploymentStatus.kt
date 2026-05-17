package com.hrplatform.employee.domain.employment

enum class EmploymentStatus {
    PRE_HIRED,
    ACTIVE,
    ON_LEAVE,
    RESIGNED,
    ;

    fun canTransitTo(newStatus: EmploymentStatus): Boolean = when (this) {
        PRE_HIRED -> newStatus == ACTIVE
        ACTIVE -> newStatus == ON_LEAVE || newStatus == RESIGNED
        ON_LEAVE -> newStatus == ACTIVE || newStatus == RESIGNED
        RESIGNED -> false
    }
}
