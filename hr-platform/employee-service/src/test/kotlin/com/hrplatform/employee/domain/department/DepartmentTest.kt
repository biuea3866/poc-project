package com.hrplatform.employee.domain.department

import com.hrplatform.employee.domain.department.event.DepartmentChangedEvent
import com.hrplatform.employee.domain.department.event.DepartmentHeadChangedEvent
import com.hrplatform.employee.domain.department.exception.CircularDepartmentException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.time.LocalDate
import java.time.ZonedDateTime

class DepartmentTest : BehaviorSpec({

    fun buildDepartment(
        id: Long,
        parentId: Long?,
        path: String,
        headEmploymentId: Long? = null,
        effectiveFrom: LocalDate = LocalDate.of(2024, 1, 1),
        effectiveTo: LocalDate? = null,
    ): Department = Department(
        id = id,
        companyId = 1L,
        name = "부서-$id",
        code = "CODE-$id",
        parentId = parentId,
        path = path,
        headEmploymentId = headEmploymentId,
        orderNo = 0,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now(),
    )

    // ─────────────────────────────────────────────────────────────────
    // moveTo — path 재계산 검증
    // ─────────────────────────────────────────────────────────────────
    given("루트 부서 아래로 이동 시") {
        val parent = buildDepartment(id = 1L, parentId = null, path = "/1/")
        val child = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/")

        `when`("moveTo(parent) 호출 시") {
            child.moveTo(parent)

            then("path 가 '/1/35/' 로 변경된다") {
                child.path shouldBe "/1/35/"
            }

            then("parentId 가 parent.id 로 변경된다") {
                child.parentId shouldBe 1L
            }

            then("DepartmentChangedEvent 가 1건 적재된다") {
                val events = child.pullDomainEvents()
                events.size shouldBe 1
                events[0].shouldBeInstanceOf<DepartmentChangedEvent>()
            }
        }
    }

    given("루트 부서로 이동 시 (newParent = null)") {
        val child = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/")

        `when`("moveTo(null) 호출 시") {
            child.moveTo(null)

            then("path 가 '/35/' 로 변경된다") {
                child.path shouldBe "/35/"
            }

            then("parentId 가 null 로 변경된다") {
                child.parentId shouldBe null
            }

            then("DepartmentChangedEvent 가 적재된다") {
                val events = child.pullDomainEvents()
                events.size shouldBe 1
                events[0].shouldBeInstanceOf<DepartmentChangedEvent>()
            }
        }
    }

    given("DepartmentChangedEvent 페이로드 검증") {
        val parent = buildDepartment(id = 2L, parentId = null, path = "/2/")
        val child = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/")
        val oldPath = child.path
        val oldParentId = child.parentId

        child.moveTo(parent)

        `when`("이벤트를 꺼내면") {
            val event = child.pullDomainEvents()[0] as DepartmentChangedEvent

            then("oldParentId, newParentId, oldPath, newPath, departmentId, companyId 가 정확하다") {
                event.oldParentId shouldBe oldParentId
                event.newParentId shouldBe 2L
                event.oldPath shouldBe oldPath
                event.newPath shouldBe "/2/35/"
                event.departmentId shouldBe 35L
                event.companyId shouldBe 1L
                event.occurredAt shouldNotBe null
            }
        }
    }

    given("자기 자신을 부모로 지정 시") {
        val department = buildDepartment(id = 35L, parentId = 12L, path = "/1/12/35/")

        `when`("moveTo(자기 자신) 호출 시") {
            then("CircularDepartmentException 발생") {
                shouldThrow<CircularDepartmentException> {
                    department.moveTo(department)
                }
            }
        }
    }

    given("자신의 자손 부서를 부모로 지정 시") {
        val ancestor = buildDepartment(id = 10L, parentId = null, path = "/10/")
        // path 로 자손 여부를 판단: 자손의 path 는 ancestor.path 로 시작
        val descendant = buildDepartment(id = 20L, parentId = 10L, path = "/10/20/")

        `when`("ancestor.moveTo(descendant) 호출 시") {
            then("CircularDepartmentException 발생") {
                shouldThrow<CircularDepartmentException> {
                    ancestor.moveTo(descendant)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // assignHead / removeHead
    // ─────────────────────────────────────────────────────────────────
    given("부서장 지정") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/")

        `when`("assignHead(employmentId=99) 호출 시") {
            department.assignHead(99L)

            then("headEmploymentId 가 99 로 변경된다") {
                department.headEmploymentId shouldBe 99L
            }

            then("DepartmentHeadChangedEvent 가 1건 적재된다") {
                val events = department.pullDomainEvents()
                events.size shouldBe 1
                events[0].shouldBeInstanceOf<DepartmentHeadChangedEvent>()
            }
        }
    }

    given("DepartmentHeadChangedEvent 페이로드 검증") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/", headEmploymentId = 50L)

        department.assignHead(99L)

        `when`("이벤트를 꺼내면") {
            val event = department.pullDomainEvents()[0] as DepartmentHeadChangedEvent

            then("oldHead=50, newHead=99, departmentId=1, companyId=1") {
                event.oldHead shouldBe 50L
                event.newHead shouldBe 99L
                event.departmentId shouldBe 1L
                event.companyId shouldBe 1L
            }
        }
    }

    given("부서장 제거") {
        val department = buildDepartment(id = 1L, parentId = null, path = "/1/", headEmploymentId = 50L)

        `when`("removeHead() 호출 시") {
            department.removeHead()

            then("headEmploymentId 가 null 로 변경된다") {
                department.headEmploymentId shouldBe null
            }

            then("DepartmentHeadChangedEvent 가 적재된다") {
                val events = department.pullDomainEvents()
                events.size shouldBe 1
                events[0].shouldBeInstanceOf<DepartmentHeadChangedEvent>()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // validateActive
    // ─────────────────────────────────────────────────────────────────
    given("활성 부서 (effectiveTo = null)") {
        val department = buildDepartment(
            id = 1L,
            parentId = null,
            path = "/1/",
            effectiveFrom = LocalDate.of(2024, 1, 1),
            effectiveTo = null,
        )

        `when`("현재 날짜로 validateActive 호출 시") {
            then("true 반환") {
                department.validateActive(LocalDate.of(2025, 6, 1)) shouldBe true
            }
        }

        `when`("effectiveFrom 이전 날짜로 validateActive 호출 시") {
            then("false 반환") {
                department.validateActive(LocalDate.of(2023, 12, 31)) shouldBe false
            }
        }
    }

    given("폐지된 부서 (effectiveTo = 2024-12-31)") {
        val department = buildDepartment(
            id = 1L,
            parentId = null,
            path = "/1/",
            effectiveFrom = LocalDate.of(2024, 1, 1),
            effectiveTo = LocalDate.of(2024, 12, 31),
        )

        `when`("effectiveTo 당일(2024-12-31)로 validateActive 호출 시") {
            then("false 반환 (effectiveTo 는 exclusive)") {
                department.validateActive(LocalDate.of(2024, 12, 31)) shouldBe false
            }
        }

        `when`("effectiveTo 이전 날짜(2024-12-30)로 validateActive 호출 시") {
            then("true 반환") {
                department.validateActive(LocalDate.of(2024, 12, 30)) shouldBe true
            }
        }
    }
})
