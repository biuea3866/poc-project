package com.hrplatform.employee.domain.history

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.ZoneOffset
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

class EmploymentHistoryTest : BehaviorSpec({

    val fixedNow: ZonedDateTime = ZonedDateTime.of(2026, 5, 17, 0, 0, 0, 0, ZoneOffset.UTC)
    val effectiveDate: LocalDate = LocalDate.of(2026, 5, 1)

    given("EmploymentHistory.create() 로 이력을 생성하면") {

        `when`("필수 파라미터만 전달하면") {
            val history = EmploymentHistory.create(
                employmentId = 1L,
                eventType = EmploymentHistoryEventType.HIRE,
                oldValue = null,
                newValue = mapOf("departmentId" to 10L),
                effectiveDate = effectiveDate,
            )

            then("employmentId, eventType, newValue, effectiveDate 가 설정된다") {
                history.employmentId shouldBe 1L
                history.eventType shouldBe EmploymentHistoryEventType.HIRE
                history.newValue shouldBe mapOf("departmentId" to 10L)
                history.effectiveDate shouldBe effectiveDate
            }

            then("oldValue, createdByEmploymentId, note 는 null 이다") {
                history.oldValue.shouldBeNull()
                history.createdByEmploymentId.shouldBeNull()
                history.note.shouldBeNull()
            }

            then("cancelledAt 은 null 이다") {
                history.cancelledAt.shouldBeNull()
            }
        }

        `when`("선택 파라미터를 모두 전달하면") {
            val history = EmploymentHistory.create(
                employmentId = 2L,
                eventType = EmploymentHistoryEventType.DEPT_CHANGE,
                oldValue = mapOf("departmentId" to 10L),
                newValue = mapOf("departmentId" to 20L),
                effectiveDate = effectiveDate,
                createdByEmploymentId = 99L,
                note = "부서 이동 메모",
            )

            then("모든 필드가 설정된다") {
                history.employmentId shouldBe 2L
                history.eventType shouldBe EmploymentHistoryEventType.DEPT_CHANGE
                history.oldValue shouldBe mapOf("departmentId" to 10L)
                history.newValue shouldBe mapOf("departmentId" to 20L)
                history.createdByEmploymentId shouldBe 99L
                history.note shouldBe "부서 이동 메모"
            }
        }
    }

    given("EmploymentHistoryEventType 에는 7종 enum 이 정의되어 있다") {
        then("HIRE, PROMOTION, DEPT_CHANGE, SALARY_CHANGE, SUSPEND, RESUME, RESIGN 이 존재한다") {
            val types = EmploymentHistoryEventType.values().map { it.name }.toSet()
            types shouldBe setOf(
                "HIRE",
                "PROMOTION",
                "DEPT_CHANGE",
                "SALARY_CHANGE",
                "SUSPEND",
                "RESUME",
                "RESIGN",
            )
        }
    }

    given("취소되지 않은 EmploymentHistory 에 markCancelled() 를 호출하면") {
        val history = EmploymentHistory.create(
            employmentId = 3L,
            eventType = EmploymentHistoryEventType.PROMOTION,
            oldValue = mapOf("positionId" to 5L),
            newValue = mapOf("positionId" to 8L),
            effectiveDate = effectiveDate,
        )

        `when`("markCancelled(at) 을 호출하면") {
            history.markCancelled(fixedNow)

            then("cancelledAt 이 설정된다") {
                history.cancelledAt shouldBe fixedNow
            }
        }
    }

    given("이미 취소된 EmploymentHistory 에 markCancelled() 를 재호출하면") {
        val history = EmploymentHistory.create(
            employmentId = 4L,
            eventType = EmploymentHistoryEventType.SALARY_CHANGE,
            oldValue = mapOf("baseSalary" to 5000000L),
            newValue = mapOf("baseSalary" to 6000000L),
            effectiveDate = effectiveDate,
        )
        history.markCancelled(fixedNow)

        `when`("두 번째 markCancelled() 를 호출하면") {
            then("IllegalStateException 이 발생한다") {
                shouldThrow<IllegalStateException> {
                    history.markCancelled(fixedNow.plusHours(1))
                }
            }
        }
    }

    given("EmploymentHistory 는 append-only 이므로 cancelledAt 외 mutable 프로퍼티가 없어야 한다") {
        `when`("EmploymentHistory 클래스의 멤버 프로퍼티를 Reflection 으로 검사하면") {
            val history = EmploymentHistory.create(
                employmentId = 5L,
                eventType = EmploymentHistoryEventType.HIRE,
                oldValue = null,
                newValue = mapOf("employeeNumber" to "EMP-001"),
                effectiveDate = effectiveDate,
            )

            then("cancelledAt 을 제외한 외부 접근 가능한 var 프로퍼티가 0개이다") {
                val mutablePublicProps = EmploymentHistory::class.memberProperties
                    .filter { prop ->
                        prop.isAccessible.let { _ ->
                            prop.name != "cancelledAt" &&
                                prop.name != "id" &&
                                prop.name != "createdAt" &&
                                prop.name != "createdBy" &&
                                prop.name != "updatedAt" &&
                                prop.name != "updatedBy" &&
                                prop.name != "deletedAt" &&
                                prop.name != "deletedBy"
                        }
                    }
                    .filterIsInstance<kotlin.reflect.KMutableProperty<*>>()
                    .filter { prop ->
                        val setter = prop.setter
                        setter.visibility == kotlin.reflect.KVisibility.PUBLIC
                    }

                mutablePublicProps.size shouldBe 0
            }
        }
    }
})
