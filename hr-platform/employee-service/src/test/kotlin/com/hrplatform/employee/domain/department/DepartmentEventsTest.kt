package com.hrplatform.employee.domain.department

import com.hrplatform.core.util.ZonedDateTimes
import com.hrplatform.employee.domain.department.event.DepartmentChangedEvent
import com.hrplatform.employee.domain.department.event.DepartmentHeadChangedEvent
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate

class DepartmentEventsTest : BehaviorSpec({

    val now = ZonedDateTimes.nowUtc()
    val today = LocalDate.now()

    fun makeDepartment(
        id: Long,
        parentId: Long? = null,
        path: String = "/$id/",
        headEmploymentId: Long? = null,
    ): Department {
        val dept = Department(
            companyId = 100L,
            name = "개발팀",
            code = "DEV-$id",
            parentId = parentId,
            path = path,
            headEmploymentId = headEmploymentId,
            orderNo = 0,
            effectiveFrom = today,
            effectiveTo = null,
        )
        val field = dept.javaClass.superclass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(dept, id)
        return dept
    }

    given("moveTo 호출 후 DepartmentChangedEvent 페이로드") {
        val parent = makeDepartment(id = 2L, path = "/2/")
        val child = makeDepartment(id = 1L, path = "/1/")

        child.moveTo(parent, actorEmploymentId = 99L, now = now)
        val event = child.pullDomainEvents().first() as DepartmentChangedEvent

        `when`("이벤트 기본 필드를 확인하면") {
            then("eventType이 DepartmentChanged이다") {
                event.eventType shouldBe "DepartmentChanged"
            }
            then("eventVersion이 1이다") {
                event.eventVersion shouldBe 1
            }
            then("aggregateType이 Department이다") {
                event.aggregateType shouldBe "Department"
            }
            then("aggregateId가 departmentId와 일치한다") {
                event.aggregateId shouldBe 1L
            }
            then("companyId가 일치한다") {
                event.companyId shouldBe 100L
            }
            then("actorEmploymentId가 일치한다") {
                event.actorEmploymentId shouldBe 99L
            }
            then("eventId가 UUID로 생성된다") {
                event.eventId shouldNotBe null
            }
        }

        `when`("action 페이로드를 확인하면") {
            then("action.type이 MOVE이다") {
                event.action.type shouldBe "MOVE"
            }
            then("action.details에 oldPath, newPath, oldParentId, newParentId가 포함된다") {
                event.action.details.containsKey("oldPath") shouldBe true
                event.action.details.containsKey("newPath") shouldBe true
                event.action.details.containsKey("oldParentId") shouldBe true
                event.action.details.containsKey("newParentId") shouldBe true
            }
            then("oldPath는 /1/이다") {
                event.action.details["oldPath"] shouldBe "/1/"
            }
            then("newPath는 /2/1/이다") {
                event.action.details["newPath"] shouldBe "/2/1/"
            }
        }

        `when`("state 페이로드를 확인하면") {
            then("state.status가 ACTIVE이다") {
                event.state.status shouldBe "ACTIVE"
            }
            then("state.snapshot에 parentId, path, headEmploymentId, effectiveFrom, effectiveTo가 포함된다") {
                event.state.snapshot.containsKey("parentId") shouldBe true
                event.state.snapshot.containsKey("path") shouldBe true
                event.state.snapshot.containsKey("headEmploymentId") shouldBe true
                event.state.snapshot.containsKey("effectiveFrom") shouldBe true
                event.state.snapshot.containsKey("effectiveTo") shouldBe true
            }
            then("state.snapshot.path가 newPath와 일치한다") {
                event.state.snapshot["path"] shouldBe "/2/1/"
            }
            then("state.snapshot.parentId가 newParentId와 일치한다") {
                event.state.snapshot["parentId"] shouldBe 2L
            }
        }
    }

    given("assignHead 호출 후 DepartmentHeadChangedEvent 페이로드") {
        val dept = makeDepartment(id = 1L, headEmploymentId = 20L)

        dept.assignHead(employmentId = 30L, actorEmploymentId = 99L, now = now)
        val event = dept.pullDomainEvents().first() as DepartmentHeadChangedEvent

        `when`("이벤트 기본 필드를 확인하면") {
            then("eventType이 DepartmentHeadChanged이다") {
                event.eventType shouldBe "DepartmentHeadChanged"
            }
            then("aggregateType이 Department이다") {
                event.aggregateType shouldBe "Department"
            }
            then("aggregateId가 departmentId와 일치한다") {
                event.aggregateId shouldBe 1L
            }
        }

        `when`("action 페이로드를 확인하면") {
            then("action.type이 CHANGE_HEAD이다") {
                event.action.type shouldBe "CHANGE_HEAD"
            }
            then("action.details에 oldHeadEmploymentId, newHeadEmploymentId가 포함된다") {
                event.action.details.containsKey("oldHeadEmploymentId") shouldBe true
                event.action.details.containsKey("newHeadEmploymentId") shouldBe true
            }
            then("oldHeadEmploymentId가 20L이다") {
                event.action.details["oldHeadEmploymentId"] shouldBe 20L
            }
            then("newHeadEmploymentId가 30L이다") {
                event.action.details["newHeadEmploymentId"] shouldBe 30L
            }
        }

        `when`("state 페이로드를 확인하면") {
            then("state.status가 ACTIVE이다") {
                event.state.status shouldBe "ACTIVE"
            }
            then("state.snapshot.headEmploymentId가 30L이다") {
                event.state.snapshot["headEmploymentId"] shouldBe 30L
            }
            then("state.snapshot에 parentId, path, effectiveFrom, effectiveTo가 포함된다") {
                event.state.snapshot.containsKey("parentId") shouldBe true
                event.state.snapshot.containsKey("path") shouldBe true
                event.state.snapshot.containsKey("effectiveFrom") shouldBe true
                event.state.snapshot.containsKey("effectiveTo") shouldBe true
            }
        }
    }

    given("removeHead 호출 후 DepartmentHeadChangedEvent 페이로드") {
        val dept = makeDepartment(id = 1L, headEmploymentId = 50L)

        dept.removeHead(actorEmploymentId = null, now = now)
        val event = dept.pullDomainEvents().first() as DepartmentHeadChangedEvent

        `when`("action 페이로드를 확인하면") {
            then("action.details.oldHeadEmploymentId가 50L이다") {
                event.action.details["oldHeadEmploymentId"] shouldBe 50L
            }
            then("action.details.newHeadEmploymentId가 null이다") {
                event.action.details["newHeadEmploymentId"] shouldBe null
            }
        }

        `when`("state.snapshot.headEmploymentId를 확인하면") {
            then("null이다") {
                event.state.snapshot["headEmploymentId"] shouldBe null
            }
        }
    }

    given("삭제된 Department에서 이벤트 발행 시 state.status") {
        `when`("soft-delete 후 moveTo를 호출하면") {
            val dept = makeDepartment(id = 1L, path = "/1/")
            dept.softDelete(now, by = null)
            val parent = makeDepartment(id = 2L, path = "/2/")

            dept.moveTo(parent, actorEmploymentId = null, now = now)
            val event = dept.pullDomainEvents().first() as DepartmentChangedEvent

            then("state.status가 ARCHIVED이다") {
                event.state.status shouldBe "ARCHIVED"
            }
        }
    }
})
