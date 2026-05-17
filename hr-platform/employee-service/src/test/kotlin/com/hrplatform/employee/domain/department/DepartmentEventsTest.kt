package com.hrplatform.employee.domain.department

import com.hrplatform.employee.domain.department.event.DepartmentChangedEvent
import com.hrplatform.employee.domain.department.event.DepartmentHeadChangedEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.ZonedDateTime

class DepartmentEventsTest : BehaviorSpec({

    fun buildDepartment(
        id: Long,
        parentId: Long?,
        path: String,
        headEmploymentId: Long? = null,
        companyId: Long = 10L,
    ): Department = Department(
        id = id,
        companyId = companyId,
        name = "부서-$id",
        code = "CODE-$id",
        parentId = parentId,
        path = path,
        headEmploymentId = headEmploymentId,
        orderNo = 0,
        effectiveFrom = LocalDate.of(2024, 1, 1),
        effectiveTo = null,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now(),
    )

    // ─────────────────────────────────────────────────────────────────
    // DepartmentChangedEvent 페이로드 완전 검증
    // ─────────────────────────────────────────────────────────────────
    given("DepartmentChangedEvent") {
        val newParent = buildDepartment(id = 5L, parentId = null, path = "/5/", companyId = 10L)
        val department = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/", companyId = 10L)

        `when`("moveTo(newParent) 후 이벤트를 꺼내면") {
            department.moveTo(newParent)
            val event = department.pullDomainEvents()[0] as DepartmentChangedEvent

            then("eventType 이 'department.changed' 이다") {
                event.eventType shouldBe "department.changed"
            }

            then("oldParentId 는 12") {
                event.oldParentId shouldBe 12L
            }

            then("newParentId 는 5") {
                event.newParentId shouldBe 5L
            }

            then("oldPath 는 '/1/12/35/'") {
                event.oldPath shouldBe "/1/12/35/"
            }

            then("newPath 는 '/5/35/'") {
                event.newPath shouldBe "/5/35/"
            }

            then("departmentId 는 35") {
                event.departmentId shouldBe 35L
            }

            then("companyId 는 10") {
                event.companyId shouldBe 10L
            }

            then("occurredAt 은 null 이 아니다") {
                event.occurredAt shouldNotBe null
            }
        }
    }

    given("루트 이동 DepartmentChangedEvent") {
        val department = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/", companyId = 10L)

        `when`("moveTo(null) 후 이벤트를 꺼내면") {
            department.moveTo(null)
            val event = department.pullDomainEvents()[0] as DepartmentChangedEvent

            then("oldParentId 는 12") {
                event.oldParentId shouldBe 12L
            }

            then("newParentId 는 null") {
                event.newParentId shouldBe null
            }

            then("newPath 는 '/35/'") {
                event.newPath shouldBe "/35/"
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // DepartmentHeadChangedEvent 페이로드 완전 검증
    // ─────────────────────────────────────────────────────────────────
    given("DepartmentHeadChangedEvent — assignHead") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/", headEmploymentId = 50L, companyId = 10L)

        `when`("assignHead(99) 후 이벤트를 꺼내면") {
            department.assignHead(99L)
            val event = department.pullDomainEvents()[0] as DepartmentHeadChangedEvent

            then("eventType 이 'department.head_changed' 이다") {
                event.eventType shouldBe "department.head_changed"
            }

            then("oldHead 는 50") {
                event.oldHead shouldBe 50L
            }

            then("newHead 는 99") {
                event.newHead shouldBe 99L
            }

            then("departmentId 는 1") {
                event.departmentId shouldBe 1L
            }

            then("companyId 는 10") {
                event.companyId shouldBe 10L
            }

            then("occurredAt 은 null 이 아니다") {
                event.occurredAt shouldNotBe null
            }
        }
    }

    given("DepartmentHeadChangedEvent — removeHead") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/", headEmploymentId = 77L, companyId = 10L)

        `when`("removeHead() 후 이벤트를 꺼내면") {
            department.removeHead()
            val event = department.pullDomainEvents()[0] as DepartmentHeadChangedEvent

            then("oldHead 는 77") {
                event.oldHead shouldBe 77L
            }

            then("newHead 는 null") {
                event.newHead shouldBe null
            }
        }
    }

    given("pullDomainEvents 멱등성 — 두 번 호출 시 두 번째는 빈 리스트") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/")
        department.assignHead(99L)

        `when`("첫 번째 pullDomainEvents 호출") {
            val first = department.pullDomainEvents()

            then("이벤트 1건") {
                first.size shouldBe 1
            }
        }

        `when`("두 번째 pullDomainEvents 호출") {
            val second = department.pullDomainEvents()

            then("빈 리스트") {
                second.size shouldBe 0
            }
        }
    }
})
