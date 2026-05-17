package com.hrplatform.employee.domain.employment

import com.hrplatform.core.event.DomainEventPublisher
import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import com.hrplatform.employee.domain.person.PersonDomainService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class EmploymentDomainService(
    private val employmentRepository: EmploymentRepository,
    private val personDomainService: PersonDomainService,
    private val historyRepository: EmploymentHistoryRepository,
    private val eventPublisher: DomainEventPublisher,
) {
    fun hire(command: HireCommand, now: ZonedDateTime): Employment {
        val person = personDomainService.findOrCreate(
            personalEmail = command.personalEmail,
            name = command.name,
            birthDate = command.birthDate,
            nationality = command.nationality,
            gender = command.gender,
        )
        val employment = buildNewEmployment(person.id, command)
        employment.activate(now, command.actorEmploymentId)
        val saved = employmentRepository.save(employment)
        historyRepository.save(buildHireHistory(saved, command))
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    private fun buildNewEmployment(personId: Long?, command: HireCommand): Employment = Employment(
        personId = requireNotNull(personId) { "저장된 Person에는 id가 있어야 합니다" },
        companyId = command.companyId,
        employeeNumber = command.employeeNumber,
        employmentType = command.employmentType,
        status = EmploymentStatus.PRE_HIRED,
        startDate = command.startDate,
        country = command.country,
        currency = command.currency,
        timezone = command.timezone,
        departmentId = command.departmentId,
        managerEmploymentId = command.managerEmploymentId,
    )

    private fun buildHireHistory(saved: Employment, command: HireCommand): EmploymentHistory =
        EmploymentHistory.create(
            employmentId = requireNotNull(saved.id) { "저장된 Employment에는 id가 있어야 합니다" },
            eventType = EmploymentHistoryEventType.HIRE,
            oldValue = null,
            newValue = mapOf("status" to "ACTIVE", "startDate" to command.startDate.toString()),
            effectiveDate = command.startDate,
            createdByEmploymentId = command.actorEmploymentId,
        )

    fun recordEvent(command: RecordEmploymentEventCommand, now: ZonedDateTime): Employment {
        val employment = findOrThrow(command.employmentId)
        val (oldValue, newValue) = applyRecordableEvent(employment, command, now)
        val saved = employmentRepository.save(employment)
        historyRepository.save(
            EmploymentHistory.create(
                employmentId = requireNotNull(saved.id),
                eventType = command.eventType.toHistoryEventType(),
                oldValue = oldValue,
                newValue = newValue,
                effectiveDate = command.effectiveDate,
                createdByEmploymentId = command.actorEmploymentId,
                note = command.note,
            ),
        )
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    fun cancelEvent(
        employmentId: Long,
        historyId: Long,
        cancellationReason: String,
        actorEmploymentId: Long?,
        now: ZonedDateTime,
    ): Employment {
        val employment = findOrThrow(employmentId)
        val lastHistory = findCancellableHistory(employmentId, historyId)
        cancelEmploymentByHistory(employment, lastHistory, cancellationReason, actorEmploymentId, now)
        lastHistory.markCancelled(now)
        historyRepository.save(lastHistory)
        val saved = employmentRepository.save(employment)
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    private fun findCancellableHistory(employmentId: Long, historyId: Long): EmploymentHistory {
        val lastHistory = historyRepository.findLastByEmploymentId(employmentId)
            ?: throw IneligibleCancellationException("취소 가능한 이력이 없습니다")
        if (lastHistory.id != historyId) throw IneligibleCancellationException("직전 이력만 취소할 수 있습니다")
        if (lastHistory.eventType == EmploymentHistoryEventType.HIRE ||
            lastHistory.eventType == EmploymentHistoryEventType.RESIGN
        ) throw IneligibleCancellationException("입사/퇴사 이력은 취소할 수 없습니다")
        return lastHistory
    }

    fun suspend(
        employmentId: Long,
        reason: String,
        until: LocalDate?,
        actorEmploymentId: Long?,
        now: ZonedDateTime,
    ): Employment {
        val employment = findOrThrow(employmentId)
        employment.suspend(reason, until, now, actorEmploymentId)
        val saved = employmentRepository.save(employment)
        historyRepository.save(
            EmploymentHistory.create(
                employmentId = requireNotNull(saved.id),
                eventType = EmploymentHistoryEventType.SUSPEND,
                oldValue = mapOf("status" to "ACTIVE"),
                newValue = mapOf("status" to "ON_LEAVE", "reason" to reason, "until" to until?.toString()),
                effectiveDate = now.toLocalDate(),
                createdByEmploymentId = actorEmploymentId,
            ),
        )
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    fun resume(employmentId: Long, actorEmploymentId: Long?, now: ZonedDateTime): Employment {
        val employment = findOrThrow(employmentId)
        employment.resume(now, actorEmploymentId)
        val saved = employmentRepository.save(employment)
        historyRepository.save(
            EmploymentHistory.create(
                employmentId = requireNotNull(saved.id),
                eventType = EmploymentHistoryEventType.RESUME,
                oldValue = mapOf("status" to "ON_LEAVE"),
                newValue = mapOf("status" to "ACTIVE"),
                effectiveDate = now.toLocalDate(),
                createdByEmploymentId = actorEmploymentId,
            ),
        )
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    fun resign(employmentId: Long, reason: String?, actorEmploymentId: Long?, now: ZonedDateTime): Employment {
        val employment = findOrThrow(employmentId)
        val previousStatus = employment.status.name
        employment.resign(now, reason, actorEmploymentId)
        val saved = employmentRepository.save(employment)
        historyRepository.save(
            EmploymentHistory.create(
                employmentId = requireNotNull(saved.id),
                eventType = EmploymentHistoryEventType.RESIGN,
                oldValue = mapOf("status" to previousStatus),
                newValue = mapOf("status" to "RESIGNED", "reason" to reason),
                effectiveDate = now.toLocalDate(),
                createdByEmploymentId = actorEmploymentId,
            ),
        )
        eventPublisher.publishAll(saved.pullDomainEvents())
        return saved
    }

    fun getById(employmentId: Long): Employment =
        employmentRepository.findById(employmentId) ?: throw EmploymentNotFoundException()

    private fun findOrThrow(employmentId: Long): Employment = getById(employmentId)

    private fun applyRecordableEvent(
        employment: Employment,
        command: RecordEmploymentEventCommand,
        now: ZonedDateTime,
    ): Pair<Map<String, Any?>, Map<String, Any?>> = when (command.eventType) {
        RecordableEventType.DEPT_CHANGE -> {
            val oldDeptId = employment.departmentId
            employment.transferTo(
                requireNotNull(command.newDepartmentId) { "부서 이동에는 newDepartmentId가 필요합니다" },
                now,
                command.actorEmploymentId,
            )
            mapOf("departmentId" to oldDeptId) to mapOf("departmentId" to employment.departmentId)
        }
        RecordableEventType.PROMOTION -> {
            val oldPositionId = employment.positionId
            employment.promote(
                requireNotNull(command.newPositionId) { "승진에는 newPositionId가 필요합니다" },
                now,
                command.actorEmploymentId,
            )
            mapOf("positionId" to oldPositionId) to mapOf("positionId" to employment.positionId)
        }
        RecordableEventType.SALARY_CHANGE -> {
            val oldSalary = employment.baseSalary
            employment.changeCompensation(
                requireNotNull(command.newBaseSalary) { "연봉 변경에는 newBaseSalary가 필요합니다" },
                requireNotNull(command.newCurrency) { "연봉 변경에는 newCurrency가 필요합니다" },
                now,
                command.actorEmploymentId,
            )
            mapOf("baseSalary" to oldSalary) to mapOf("baseSalary" to employment.baseSalary)
        }
        RecordableEventType.SUSPEND -> {
            employment.suspend("발령 기록", null, now, command.actorEmploymentId)
            mapOf("status" to "ACTIVE") to mapOf("status" to "ON_LEAVE")
        }
    }

    private fun cancelEmploymentByHistory(
        employment: Employment,
        history: EmploymentHistory,
        cancellationReason: String,
        actorEmploymentId: Long?,
        now: ZonedDateTime,
    ) {
        val oldValue = history.oldValue
        when (history.eventType) {
            EmploymentHistoryEventType.DEPT_CHANGE -> employment.cancelLastTransfer(
                cancelledHistoryId = requireNotNull(history.id),
                cancellationReason = cancellationReason,
                previousDepartmentId = oldValue?.get("departmentId") as? Long,
                now = now,
                actorEmploymentId = actorEmploymentId,
            )
            EmploymentHistoryEventType.PROMOTION -> employment.cancelLastPromotion(
                cancelledHistoryId = requireNotNull(history.id),
                cancellationReason = cancellationReason,
                previousPositionId = oldValue?.get("positionId") as? Long,
                now = now,
                actorEmploymentId = actorEmploymentId,
            )
            EmploymentHistoryEventType.SALARY_CHANGE -> employment.cancelLastSalaryChange(
                cancelledHistoryId = requireNotNull(history.id),
                cancellationReason = cancellationReason,
                previousBaseSalary = oldValue?.get("baseSalary") as? Long,
                previousCurrency = oldValue?.get("currency") as? String,
                now = now,
                actorEmploymentId = actorEmploymentId,
            )
            EmploymentHistoryEventType.SUSPEND -> employment.cancelLastSuspend(
                cancelledHistoryId = requireNotNull(history.id),
                cancellationReason = cancellationReason,
                previousSuspendReason = oldValue?.get("reason") as? String ?: "",
                previousSuspendUntil = null,
                now = now,
                actorEmploymentId = actorEmploymentId,
            )
            else -> throw IneligibleCancellationException("취소 불가능한 이력 유형입니다: ${history.eventType}")
        }
    }
}

private fun RecordableEventType.toHistoryEventType(): EmploymentHistoryEventType = when (this) {
    RecordableEventType.DEPT_CHANGE -> EmploymentHistoryEventType.DEPT_CHANGE
    RecordableEventType.PROMOTION -> EmploymentHistoryEventType.PROMOTION
    RecordableEventType.SALARY_CHANGE -> EmploymentHistoryEventType.SALARY_CHANGE
    RecordableEventType.SUSPEND -> EmploymentHistoryEventType.SUSPEND
}
