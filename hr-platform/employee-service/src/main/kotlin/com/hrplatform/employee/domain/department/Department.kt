package com.hrplatform.employee.domain.department

import com.hrplatform.core.domain.AggregateRoot
import com.hrplatform.employee.domain.department.event.DepartmentChangedEvent
import com.hrplatform.employee.domain.department.event.DepartmentHeadChangedEvent
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate
import java.time.ZonedDateTime

@Entity
@Table(
    name = "department",
    uniqueConstraints = [UniqueConstraint(columnNames = ["company_id", "code"])],
)
class Department(
    @Column(name = "company_id", nullable = false)
    var companyId: Long,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var code: String,

    @Column(name = "parent_id")
    var parentId: Long? = null,

    @Column(nullable = false)
    var path: String,

    @Column(name = "head_employment_id")
    var headEmploymentId: Long? = null,

    @Column(name = "order_no", nullable = false)
    var orderNo: Int,

    @Column(name = "effective_from", nullable = false)
    var effectiveFrom: LocalDate,

    @Column(name = "effective_to")
    var effectiveTo: LocalDate? = null,
) : AggregateRoot() {

    init {
        require(path.startsWith("/") && path.endsWith("/")) { "path는 / 로 시작·끝나야 합니다" }
    }

    fun moveTo(newParent: Department?, actorEmploymentId: Long?, now: ZonedDateTime) {
        validateNotCircular(newParent)
        val oldParentId = parentId
        val oldPath = path
        parentId = newParent?.id
        path = buildPath(newParent)
        addDomainEvent(
            DepartmentChangedEvent(
                departmentId = requireNotNull(id) { "저장되지 않은 Department에는 moveTo를 호출할 수 없습니다" },
                companyIdValue = companyId,
                oldParentId = oldParentId,
                newParentId = parentId,
                oldPath = oldPath,
                newPath = path,
                actorEmploymentId = actorEmploymentId,
                occurredAt = now,
                statusValue = if (isDeleted) "ARCHIVED" else "ACTIVE",
                snapshotHeadEmploymentId = headEmploymentId,
                effectiveFromValue = effectiveFrom,
                effectiveToValue = effectiveTo,
            ),
        )
    }

    fun assignHead(employmentId: Long, actorEmploymentId: Long?, now: ZonedDateTime) {
        if (headEmploymentId == employmentId) return
        val oldHead = headEmploymentId
        headEmploymentId = employmentId
        addDomainEvent(buildHeadChangedEvent(oldHead, headEmploymentId, actorEmploymentId, now))
    }

    fun removeHead(actorEmploymentId: Long?, now: ZonedDateTime) {
        if (headEmploymentId == null) return
        val oldHead = headEmploymentId
        headEmploymentId = null
        addDomainEvent(buildHeadChangedEvent(oldHead, headEmploymentId, actorEmploymentId, now))
    }

    fun isActive(date: LocalDate): Boolean =
        effectiveFrom <= date && (effectiveTo == null || date < effectiveTo)

    private fun buildPath(newParent: Department?): String {
        val selfId = requireNotNull(id) { "저장되지 않은 Department에는 path를 재계산할 수 없습니다" }
        return if (newParent == null) "/$selfId/" else "${newParent.path}$selfId/"
    }

    private fun buildHeadChangedEvent(
        oldHead: Long?,
        newHead: Long?,
        actorEmploymentId: Long?,
        now: ZonedDateTime,
    ): DepartmentHeadChangedEvent = DepartmentHeadChangedEvent(
        departmentId = requireNotNull(id) { "저장되지 않은 Department에는 이벤트를 발행할 수 없습니다" },
        companyIdValue = companyId,
        oldHeadEmploymentId = oldHead,
        newHeadEmploymentId = newHead,
        actorEmploymentId = actorEmploymentId,
        occurredAt = now,
        statusValue = if (isDeleted) "ARCHIVED" else "ACTIVE",
        snapshotParentId = parentId,
        snapshotPath = path,
        effectiveFromValue = effectiveFrom,
        effectiveToValue = effectiveTo,
    )

    private fun validateNotCircular(newParent: Department?) {
        if (newParent == null) return
        if (newParent.id == id) throw CircularDepartmentException()
        if (newParent.path.startsWith(path)) throw CircularDepartmentException()
    }
}
