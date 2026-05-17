package com.hrplatform.employee.domain.employment

import com.hrplatform.core.domain.AggregateRoot
import com.hrplatform.employee.domain.employment.event.EmployeeHiredEvent
import com.hrplatform.employee.domain.employment.event.EmployeePromotedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeePromotedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeResignedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeResumedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSalaryChangedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSalaryChangedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSuspendedCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeSuspendedEvent
import com.hrplatform.employee.domain.employment.event.EmployeeTransferredCancelledEvent
import com.hrplatform.employee.domain.employment.event.EmployeeTransferredEvent
import com.hrplatform.employee.domain.employment.exception.CrossCompanyAccessException
import com.hrplatform.employee.domain.employment.exception.IneligibleCancellationException
import com.hrplatform.employee.domain.employment.exception.InvalidStateTransitionException
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 고용 인스턴스 AggregateRoot.
 *
 * ADR-002 §4 상태 머신을 캡슐화한다.
 * 상태 전이·검증·발령 취소 보상 로직이 모두 이 Entity 안에서 완결된다.
 * Repository / Gateway / DomainEventPublisher 주입 금지 — 순수 Domain 객체.
 *
 * 생성자 파라미터를 [EmploymentSpec]으로 그룹핑하여 LongParameterList 규칙을 준수한다.
 */
class Employment(
    id: Long?,
    val spec: EmploymentSpec,
    status: EmploymentStatus,
    departmentId: Long? = null,
    positionId: Long? = null,
    baseSalary: Long? = null,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
) : AggregateRoot(id, createdAt, updatedAt) {

    // EmploymentSpec 위임 — 자주 참조되는 필드는 직접 접근 가능하도록 프로퍼티 위임
    val personId: Long get() = spec.personId
    val companyId: Long get() = spec.companyId
    val employeeNumber: String get() = spec.employeeNumber
    val employmentType: EmploymentType get() = spec.employmentType
    val startDate: LocalDate get() = spec.startDate
    val endDate: LocalDate? get() = spec.endDate
    val country: String get() = spec.country
    val currency: String get() = spec.currency
    val timezone: String get() = spec.timezone
    val managerEmploymentId: Long? get() = spec.managerEmploymentId
    val workSchedulePolicyId: Long? get() = spec.workSchedulePolicyId
    val leavePolicyId: Long? get() = spec.leavePolicyId
    val compensationCurrency: String? get() = spec.compensationCurrency

    var status: EmploymentStatus = status
        private set

    var departmentId: Long? = departmentId
        private set

    var positionId: Long? = positionId
        private set

    var baseSalary: Long? = baseSalary
        private set

    // 발령 취소 보상을 위한 직전 스냅샷
    private var _previousDepartmentId: Long? = null
    private var _previousPositionId: Long? = null
    private var _previousBaseSalary: Long? = null
    private var _lastChangeType: LastChangeType? = null

    private enum class LastChangeType {
        TRANSFER, PROMOTION, SALARY_CHANGE, SUSPEND
    }

    // ─────────────────────────────────────────────────────────────────
    // 상태 전이 메서드
    // ─────────────────────────────────────────────────────────────────

    fun activate(now: ZonedDateTime) {
        transit(EmploymentStatus.ACTIVE)
        addDomainEvent(
            EmployeeHiredEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                personId = personId,
                employeeNumber = employeeNumber,
                occurredAt = now,
            ),
        )
    }

    fun suspend(reason: String, until: LocalDate?, now: ZonedDateTime) {
        transit(EmploymentStatus.ON_LEAVE)
        _lastChangeType = LastChangeType.SUSPEND
        addDomainEvent(
            EmployeeSuspendedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                reason = reason,
                until = until,
                occurredAt = now,
            ),
        )
    }

    fun resume(now: ZonedDateTime) {
        transit(EmploymentStatus.ACTIVE)
        addDomainEvent(
            EmployeeResumedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                occurredAt = now,
            ),
        )
    }

    fun resign(now: ZonedDateTime, reason: String?) {
        transit(EmploymentStatus.RESIGNED)
        addDomainEvent(
            EmployeeResignedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                reason = reason,
                occurredAt = now,
            ),
        )
    }

    fun resignDuringLeave(now: ZonedDateTime, reason: String?) {
        if (status != EmploymentStatus.ON_LEAVE) {
            throw InvalidStateTransitionException(status.name, EmploymentStatus.RESIGNED.name)
        }
        status = EmploymentStatus.RESIGNED
        addDomainEvent(
            EmployeeResignedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                reason = reason,
                occurredAt = now,
            ),
        )
    }

    fun transferTo(newDepartmentId: Long, now: ZonedDateTime) {
        validateNotResigned()
        _previousDepartmentId = departmentId
        departmentId = newDepartmentId
        _lastChangeType = LastChangeType.TRANSFER
        addDomainEvent(
            EmployeeTransferredEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                previousDepartmentId = _previousDepartmentId,
                newDepartmentId = newDepartmentId,
                occurredAt = now,
            ),
        )
    }

    fun promote(newPositionId: Long, now: ZonedDateTime) {
        validateNotResigned()
        _previousPositionId = positionId
        positionId = newPositionId
        _lastChangeType = LastChangeType.PROMOTION
        addDomainEvent(
            EmployeePromotedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                previousPositionId = _previousPositionId,
                newPositionId = newPositionId,
                occurredAt = now,
            ),
        )
    }

    fun changeCompensation(newBaseSalary: Long, newCurrency: String, now: ZonedDateTime) {
        validateNotResigned()
        _previousBaseSalary = baseSalary
        baseSalary = newBaseSalary
        _lastChangeType = LastChangeType.SALARY_CHANGE
        addDomainEvent(
            EmployeeSalaryChangedEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                previousBaseSalary = _previousBaseSalary,
                newBaseSalary = newBaseSalary,
                currency = newCurrency,
                occurredAt = now,
            ),
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 발령 취소 보상 — 직전 1건만 허용
    // ─────────────────────────────────────────────────────────────────

    fun cancelLastTransfer(now: ZonedDateTime) {
        if (_lastChangeType != LastChangeType.TRANSFER) {
            throw IneligibleCancellationException("직전 발령이 부서 이동이 아니므로 취소 불가")
        }
        val cancelledDepartmentId = requireNotNull(departmentId)
        departmentId = _previousDepartmentId
        _lastChangeType = null
        addDomainEvent(
            EmployeeTransferredCancelledEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                cancelledDepartmentId = cancelledDepartmentId,
                restoredDepartmentId = _previousDepartmentId,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastPromotion(now: ZonedDateTime) {
        if (_lastChangeType != LastChangeType.PROMOTION) {
            throw IneligibleCancellationException("직전 발령이 승진이 아니므로 취소 불가")
        }
        val cancelledPositionId = requireNotNull(positionId)
        positionId = _previousPositionId
        _lastChangeType = null
        addDomainEvent(
            EmployeePromotedCancelledEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                cancelledPositionId = cancelledPositionId,
                restoredPositionId = _previousPositionId,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastSalaryChange(now: ZonedDateTime) {
        if (_lastChangeType != LastChangeType.SALARY_CHANGE) {
            throw IneligibleCancellationException("직전 발령이 연봉 변경이 아니므로 취소 불가")
        }
        val cancelledBaseSalary = requireNotNull(baseSalary)
        val restoredBaseSalary = _previousBaseSalary
        baseSalary = restoredBaseSalary
        _lastChangeType = null
        addDomainEvent(
            EmployeeSalaryChangedCancelledEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                cancelledBaseSalary = cancelledBaseSalary,
                restoredBaseSalary = restoredBaseSalary,
                currency = compensationCurrency ?: currency,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastSuspend(now: ZonedDateTime) {
        if (_lastChangeType != LastChangeType.SUSPEND || status != EmploymentStatus.ON_LEAVE) {
            throw IneligibleCancellationException("직전 발령이 휴직이 아니거나 현재 상태가 ON_LEAVE가 아니므로 취소 불가")
        }
        status = EmploymentStatus.ACTIVE
        _lastChangeType = null
        addDomainEvent(
            EmployeeSuspendedCancelledEvent(
                employmentId = requireNotNull(id),
                companyId = companyId,
                occurredAt = now,
            ),
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // 검증 메서드
    // ─────────────────────────────────────────────────────────────────

    fun validateActive() {
        if (status != EmploymentStatus.ACTIVE) {
            throw InvalidStateTransitionException(status.name, "ACTIVE required")
        }
    }

    fun validateNotResigned() {
        if (status == EmploymentStatus.RESIGNED) {
            throw InvalidStateTransitionException(status.name, "non-RESIGNED required")
        }
    }

    fun validateBelongsToCompany(targetCompanyId: Long) {
        if (companyId != targetCompanyId) {
            throw CrossCompanyAccessException(viewerCompanyId = companyId, targetCompanyId = targetCompanyId)
        }
    }

    /**
     * TEAM_LEAD 권한 범위 판정.
     * viewer가 다음 조건 중 하나를 충족하면 접근 가능:
     * 1) viewer == target (본인)
     * 2) 같은 companyId + 같은 departmentId
     * 3) target.managerEmploymentId == viewer.id (직속 상관)
     */
    fun isAccessibleBy(viewer: Employment): Boolean {
        if (companyId != viewer.companyId) return false
        return isSelf(viewer) || isSameDepartment(viewer) || isDirectReport(viewer)
    }

    // ─────────────────────────────────────────────────────────────────
    // 내부 헬퍼
    // ─────────────────────────────────────────────────────────────────

    private fun transit(targetStatus: EmploymentStatus) {
        if (!status.canTransitTo(targetStatus)) {
            throw InvalidStateTransitionException(status.name, targetStatus.name)
        }
        status = targetStatus
    }

    private fun isSelf(viewer: Employment): Boolean = id != null && id == viewer.id

    private fun isSameDepartment(viewer: Employment): Boolean =
        departmentId != null && departmentId == viewer.departmentId

    private fun isDirectReport(viewer: Employment): Boolean =
        managerEmploymentId != null && managerEmploymentId == viewer.id
}
