package com.hrplatform.employee.domain.department

import com.hrplatform.core.util.ZonedDateTimes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate

class DepartmentTest : BehaviorSpec({

    val now = ZonedDateTimes.nowUtc()
    val today = LocalDate.now()

    fun makeDepartment(
        id: Long = 1L,
        parentId: Long? = null,
        path: String = "/$id/",
        headEmploymentId: Long? = null,
        effectiveFrom: LocalDate = today,
        effectiveTo: LocalDate? = null,
    ): Department {
        val dept = Department(
            companyId = 100L,
            name = "개발팀",
            code = "DEV",
            parentId = parentId,
            path = path,
            headEmploymentId = headEmploymentId,
            orderNo = 0,
            effectiveFrom = effectiveFrom,
            effectiveTo = effectiveTo,
        )
        // id를 리플렉션으로 설정 (테스트 전용)
        val field = dept.javaClass.superclass.superclass.getDeclaredField("id")
        field.isAccessible = true
        field.set(dept, id)
        return dept
    }

    given("Department moveTo") {
        `when`("루트 부서를 다른 루트로 이동하면") {
            val parent = makeDepartment(id = 2L, path = "/2/")
            val child = makeDepartment(id = 1L, path = "/1/")

            child.moveTo(parent, actorEmploymentId = 10L, now = now)

            then("path가 부모 path + selfId 형식으로 재계산된다") {
                child.path shouldBe "/2/1/"
            }
            then("parentId가 부모 id로 변경된다") {
                child.parentId shouldBe 2L
            }
            then("DepartmentChangedEvent가 발행된다") {
                child.pullDomainEvents().size shouldBe 1
            }
        }

        `when`("부서를 루트로 이동하면 (parent = null)") {
            val child = makeDepartment(id = 3L, parentId = 2L, path = "/2/3/")

            child.moveTo(newParent = null, actorEmploymentId = null, now = now)

            then("path가 /id/ 형식이 된다") {
                child.path shouldBe "/3/"
            }
            then("parentId가 null이 된다") {
                child.parentId shouldBe null
            }
        }

        `when`("자기 자신을 부모로 설정하면") {
            val dept = makeDepartment(id = 5L, path = "/5/")

            then("CircularDepartmentException이 발생한다") {
                shouldThrow<CircularDepartmentException> {
                    dept.moveTo(dept, actorEmploymentId = null, now = now)
                }
            }
        }

        `when`("자신의 하위 부서를 부모로 설정하면") {
            val parent = makeDepartment(id = 1L, path = "/1/")
            val child = makeDepartment(id = 2L, parentId = 1L, path = "/1/2/")

            then("CircularDepartmentException이 발생한다") {
                shouldThrow<CircularDepartmentException> {
                    parent.moveTo(child, actorEmploymentId = null, now = now)
                }
            }
        }
    }

    given("Department assignHead") {
        `when`("새 부서장을 지정하면") {
            val dept = makeDepartment(id = 1L, headEmploymentId = null)

            dept.assignHead(employmentId = 50L, actorEmploymentId = 10L, now = now)

            then("headEmploymentId가 변경된다") {
                dept.headEmploymentId shouldBe 50L
            }
            then("DepartmentHeadChangedEvent가 발행된다") {
                dept.pullDomainEvents().size shouldBe 1
            }
        }

        `when`("이미 같은 부서장을 지정하면 (멱등)") {
            val dept = makeDepartment(id = 1L, headEmploymentId = 50L)

            dept.assignHead(employmentId = 50L, actorEmploymentId = 10L, now = now)

            then("이벤트가 발행되지 않는다") {
                dept.pullDomainEvents().size shouldBe 0
            }
        }
    }

    given("Department removeHead") {
        `when`("부서장이 있을 때 removeHead를 호출하면") {
            val dept = makeDepartment(id = 1L, headEmploymentId = 50L)

            dept.removeHead(actorEmploymentId = 10L, now = now)

            then("headEmploymentId가 null이 된다") {
                dept.headEmploymentId shouldBe null
            }
            then("DepartmentHeadChangedEvent가 발행된다") {
                dept.pullDomainEvents().size shouldBe 1
            }
        }

        `when`("부서장이 없을 때 removeHead를 호출하면 (멱등)") {
            val dept = makeDepartment(id = 1L, headEmploymentId = null)

            dept.removeHead(actorEmploymentId = null, now = now)

            then("이벤트가 발행되지 않는다") {
                dept.pullDomainEvents().size shouldBe 0
            }
        }
    }

    given("Department isActive") {
        `when`("오늘이 effectiveFrom 이후이고 effectiveTo가 null이면") {
            val dept = makeDepartment(effectiveFrom = today.minusDays(1), effectiveTo = null)

            then("isActive는 true이다") {
                dept.isActive(today) shouldBe true
            }
        }

        `when`("오늘이 effectiveTo 이전이면") {
            val dept = makeDepartment(effectiveFrom = today.minusDays(10), effectiveTo = today.plusDays(1))

            then("isActive는 true이다") {
                dept.isActive(today) shouldBe true
            }
        }

        `when`("오늘이 effectiveTo 당일이면") {
            val dept = makeDepartment(effectiveFrom = today.minusDays(10), effectiveTo = today)

            then("isActive는 false이다 (종료일 당일 제외)") {
                dept.isActive(today) shouldBe false
            }
        }

        `when`("오늘이 effectiveFrom 이전이면") {
            val dept = makeDepartment(effectiveFrom = today.plusDays(1), effectiveTo = null)

            then("isActive는 false이다") {
                dept.isActive(today) shouldBe false
            }
        }
    }

    given("Department path 유효성 검증") {
        `when`("path가 /로 시작하지 않으면") {
            then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Department(
                        companyId = 1L,
                        name = "팀",
                        code = "TEAM",
                        parentId = null,
                        path = "invalid/",
                        headEmploymentId = null,
                        orderNo = 0,
                        effectiveFrom = today,
                        effectiveTo = null,
                    )
                }
            }
        }

        `when`("path가 /로 끝나지 않으면") {
            then("IllegalArgumentException이 발생한다") {
                shouldThrow<IllegalArgumentException> {
                    Department(
                        companyId = 1L,
                        name = "팀",
                        code = "TEAM",
                        parentId = null,
                        path = "/invalid",
                        headEmploymentId = null,
                        orderNo = 0,
                        effectiveFrom = today,
                        effectiveTo = null,
                    )
                }
            }
        }
    }
})
