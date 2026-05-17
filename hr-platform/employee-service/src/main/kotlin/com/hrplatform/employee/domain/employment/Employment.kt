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
import io.hypersistence.utils.hibernate.type.json.JsonStringType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Type
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "employment",
    uniqueConstraints = [UniqueConstraint(columnNames = ["company_id", "employee_number"])],
)
@Suppress("LongParameterList")
class Employment(
    @Column(name = "person_id", nullable = false)
    var personId: Long,

    @Column(name = "company_id", nullable = false)
    var companyId: Long,

    @Column(name = "employee_number", nullable = false)
    var employeeNumber: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    var employmentType: EmploymentType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmploymentStatus,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: EmploymentRole = EmploymentRole.EMPLOYEE,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(columnDefinition = "CHAR(2)", nullable = false)
    var country: String,

    @Column(columnDefinition = "CHAR(3)", nullable = false)
    var currency: String,

    @Column(nullable = false)
    var timezone: String,

    @Column(name = "position_id")
    var positionId: Long? = null,

    @Column(name = "department_id")
    var departmentId: Long? = null,

    @Column(name = "manager_employment_id")
    var managerEmploymentId: Long? = null,

    @Column(name = "work_schedule_policy_id")
    var workSchedulePolicyId: Long? = null,

    @Column(name = "leave_policy_id")
    var leavePolicyId: Long? = null,

    @Column(name = "base_salary")
    var baseSalary: Long? = null,

    @Column(name = "compensation_currency", columnDefinition = "CHAR(3)")
    var compensationCurrency: String? = null,

    @Type(JsonStringType::class)
    @Column(name = "additional_compensation", columnDefinition = "TEXT")
    var additionalCompensation: Map<String, Any?>? = null,
) : AggregateRoot() {

    // ========== 상태 전이 메서드 ==========

    fun activate(now: ZonedDateTime, actorEmploymentId: Long?) {
        requireTransition(EmploymentStatus.ACTIVE)
        status = EmploymentStatus.ACTIVE
        addDomainEvent(
            EmployeeHiredEvent(
                employmentId = id ?: 0L,
                personId = personId,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                employmentType = employmentType,
                startDate = startDate,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun suspend(reason: String, until: LocalDate?, now: ZonedDateTime, actorEmploymentId: Long?) {
        requireTransition(EmploymentStatus.ON_LEAVE)
        status = EmploymentStatus.ON_LEAVE
        addDomainEvent(
            EmployeeSuspendedEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                reason = reason,
                until = until,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun resume(now: ZonedDateTime, actorEmploymentId: Long?) {
        requireTransition(EmploymentStatus.ACTIVE)
        status = EmploymentStatus.ACTIVE
        addDomainEvent(
            EmployeeResumedEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                resumedAt = now,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun resign(now: ZonedDateTime, reason: String?, actorEmploymentId: Long?) {
        requireTransition(EmploymentStatus.RESIGNED)
        status = EmploymentStatus.RESIGNED
        addDomainEvent(
            EmployeeResignedEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                reason = reason,
                effectiveDate = now.toLocalDate(),
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun transferTo(newDepartmentId: Long, now: ZonedDateTime, actorEmploymentId: Long?) {
        validateActive()
        val oldDepartmentId = departmentId
        departmentId = newDepartmentId
        addDomainEvent(
            EmployeeTransferredEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                oldDepartmentId = oldDepartmentId,
                newDepartmentId = newDepartmentId,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun promote(newPositionId: Long, now: ZonedDateTime, actorEmploymentId: Long?) {
        validateActive()
        val oldPositionId = positionId
        positionId = newPositionId
        addDomainEvent(
            EmployeePromotedEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                oldPositionId = oldPositionId,
                newPositionId = newPositionId,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun changeCompensation(newBaseSalary: Long, newCurrency: String, now: ZonedDateTime, actorEmploymentId: Long?) {
        validateActive()
        val oldBaseSalary = baseSalary
        baseSalary = newBaseSalary
        compensationCurrency = newCurrency
        addDomainEvent(
            EmployeeSalaryChangedEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                oldBaseSalary = oldBaseSalary,
                newBaseSalary = newBaseSalary,
                salaryChangeCurrency = newCurrency,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    // ========== 발령 취소 4종 ==========

    fun cancelLastTransfer(
        cancelledHistoryId: Long,
        cancellationReason: String,
        previousDepartmentId: Long?,
        now: ZonedDateTime,
        actorEmploymentId: Long?,
    ) {
        validateNotResigned()
        departmentId = previousDepartmentId
        addDomainEvent(
            EmployeeTransferredCancelledEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                cancelledHistoryId = cancelledHistoryId,
                cancellationReason = cancellationReason,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastPromotion(
        cancelledHistoryId: Long,
        cancellationReason: String,
        previousPositionId: Long?,
        now: ZonedDateTime,
        actorEmploymentId: Long?,
    ) {
        validateNotResigned()
        positionId = previousPositionId
        addDomainEvent(
            EmployeePromotedCancelledEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                cancelledHistoryId = cancelledHistoryId,
                cancellationReason = cancellationReason,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastSalaryChange(
        cancelledHistoryId: Long,
        cancellationReason: String,
        previousBaseSalary: Long?,
        previousCurrency: String?,
        now: ZonedDateTime,
        actorEmploymentId: Long?,
    ) {
        validateNotResigned()
        baseSalary = previousBaseSalary
        compensationCurrency = previousCurrency
        addDomainEvent(
            EmployeeSalaryChangedCancelledEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                cancelledHistoryId = cancelledHistoryId,
                cancellationReason = cancellationReason,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    fun cancelLastSuspend(
        cancelledHistoryId: Long,
        cancellationReason: String,
        previousSuspendReason: String,
        previousSuspendUntil: LocalDate?,
        now: ZonedDateTime,
        actorEmploymentId: Long?,
    ) {
        validateNotResigned()
        check(status == EmploymentStatus.ON_LEAVE) {
            "휴직 취소는 ON_LEAVE 상태에서만 가능합니다. 현재 상태: $status"
        }
        status = EmploymentStatus.ACTIVE
        addDomainEvent(
            EmployeeSuspendedCancelledEvent(
                employmentId = id ?: 0L,
                companyIdValue = companyId,
                departmentId = departmentId,
                managerEmploymentId = managerEmploymentId,
                country = country,
                currency = currency,
                timezone = timezone,
                cancelledHistoryId = cancelledHistoryId,
                cancellationReason = cancellationReason,
                previousSuspendReason = previousSuspendReason,
                previousSuspendUntil = previousSuspendUntil,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
            ),
        )
    }

    // ========== 검증 ==========

    fun validateActive() {
        check(status == EmploymentStatus.ACTIVE) {
            "ACTIVE 상태의 Employment에서만 가능합니다. 현재 상태: $status"
        }
    }

    fun validateNotResigned() {
        if (status == EmploymentStatus.RESIGNED) {
            throw InvalidStateTransitionException(EmploymentStatus.RESIGNED, EmploymentStatus.RESIGNED)
        }
    }

    fun isAccessibleBy(viewer: Employment): Boolean {
        if (viewer.status != EmploymentStatus.ACTIVE) return false
        return companyId == viewer.companyId
    }

    // ========== 내부 ==========

    private fun requireTransition(target: EmploymentStatus) {
        if (!status.canTransitTo(target)) {
            throw InvalidStateTransitionException(status, target)
        }
    }
}
