package com.hrplatform.employee.domain.department

import com.hrplatform.core.domain.AggregateRoot
import com.hrplatform.employee.domain.department.event.DepartmentChangedEvent
import com.hrplatform.employee.domain.department.event.DepartmentHeadChangedEvent
import com.hrplatform.employee.domain.department.exception.CircularDepartmentException
import java.time.LocalDate
import java.time.ZonedDateTime

class Department(
    id: Long?,
    val companyId: Long,
    var name: String,
    val code: String,
    var parentId: Long?,
    var path: String,
    var headEmploymentId: Long?,
    var orderNo: Int,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    createdAt: ZonedDateTime,
    updatedAt: ZonedDateTime,
) : AggregateRoot(id, createdAt, updatedAt) {

    /**
     * 부서를 새 부모 아래로 이동한다.
     * - path 를 재계산한다: newParent.path + this.id + "/" (루트면 "/" + this.id + "/")
     * - 자식 path 일괄 갱신은 DomainService(BE-06) 책임.
     * - 자기 자신 또는 자손으로의 이동은 CircularDepartmentException 을 발생시킨다.
     */
    fun moveTo(newParent: Department?) {
        validateNotCircular(newParent)

        val oldParentId = this.parentId
        val oldPath = this.path

        val newPath = if (newParent == null) {
            "/${requireNotNull(id) { "Department id must not be null" }}/"
        } else {
            "${newParent.path}${requireNotNull(id) { "Department id must not be null" }}/"
        }

        this.parentId = newParent?.id
        this.path = newPath

        addDomainEvent(
            DepartmentChangedEvent(
                departmentId = requireNotNull(id),
                companyId = companyId,
                oldParentId = oldParentId,
                newParentId = newParent?.id,
                oldPath = oldPath,
                newPath = newPath,
                occurredAt = ZonedDateTime.now(),
            ),
        )
    }

    /**
     * 부서장을 지정한다.
     * ON_LEAVE / RESIGNED Employment 의 부서장 지정 가능 여부 검증은 DomainService(BE-06) 책임.
     * 본 메서드는 headEmploymentId 변경 + 이벤트 적재만 담당한다.
     */
    fun assignHead(employmentId: Long) {
        val oldHead = this.headEmploymentId
        this.headEmploymentId = employmentId

        addDomainEvent(
            DepartmentHeadChangedEvent(
                departmentId = requireNotNull(id),
                companyId = companyId,
                oldHead = oldHead,
                newHead = employmentId,
                occurredAt = ZonedDateTime.now(),
            ),
        )
    }

    /**
     * 부서장을 제거한다.
     */
    fun removeHead() {
        val oldHead = this.headEmploymentId
        this.headEmploymentId = null

        addDomainEvent(
            DepartmentHeadChangedEvent(
                departmentId = requireNotNull(id),
                companyId = companyId,
                oldHead = oldHead,
                newHead = null,
                occurredAt = ZonedDateTime.now(),
            ),
        )
    }

    /**
     * 주어진 날짜 기준 부서가 활성 상태인지 검증한다.
     * effectiveFrom <= date < effectiveTo (effectiveTo = null 이면 미종료)
     */
    fun validateActive(date: LocalDate): Boolean {
        if (date.isBefore(effectiveFrom)) return false
        val to = effectiveTo ?: return true
        return date.isBefore(to)
    }

    private fun validateNotCircular(newParent: Department?) {
        if (newParent == null) return

        val newParentId = newParent.id
        val selfId = requireNotNull(id) { "Department id must not be null" }

        // 자기 자신을 부모로 지정
        if (newParentId == selfId) {
            throw CircularDepartmentException(selfId, newParentId)
        }

        // 자신의 자손을 부모로 지정 — newParent.path 가 self.path 로 시작하면 자손
        if (newParent.path.startsWith(this.path)) {
            throw CircularDepartmentException(selfId, newParentId ?: selfId)
        }
    }
}
