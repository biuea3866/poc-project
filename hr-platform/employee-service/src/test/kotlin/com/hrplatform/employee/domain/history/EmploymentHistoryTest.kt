package com.hrplatform.employee.domain.history

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EmploymentHistoryTest : BehaviorSpec({

    val now = ZonedDateTime.of(2026, 5, 17, 9, 0, 0, 0, ZoneOffset.UTC)
    val effectiveDate = LocalDate.of(2026, 5, 17)

    given("EmploymentHistory.create() 팩토리를 호출하면") {

        `when`("HIRE 이벤트로 newValue만 제공 시") {
            val history = EmploymentHistory.create(
                employmentId = 1L,
                eventType = EmploymentHistoryEventType.HIRE,
                newValue = mapOf("status" to "ACTIVE"),
                effectiveDate = effectiveDate,
                createdAt = now,
            )

            then("EmploymentHistory가 생성되고 cancelledAt은 null이다") {
                history.employmentId shouldBe 1L
                history.eventType shouldBe EmploymentHistoryEventType.HIRE
                history.oldValue.shouldBeNull()
                history.newValue shouldBe mapOf("status" to "ACTIVE")
                history.effectiveDate shouldBe effectiveDate
                history.cancelledAt.shouldBeNull()
                history.createdAt shouldBe now
            }
        }

        `when`("DEPT_CHANGE 이벤트로 oldValue와 메타 정보를 제공하면") {
            val history = EmploymentHistory.create(
                employmentId = 2L,
                eventType = EmploymentHistoryEventType.DEPT_CHANGE,
                newValue = mapOf("departmentId" to 20L),
                effectiveDate = effectiveDate,
                createdAt = now,
                oldValue = mapOf("departmentId" to 10L),
                createdByEmploymentId = 100L,
                note = "부서 이동",
            )

            then("oldValue, createdByEmploymentId, note가 모두 저장된다") {
                history.oldValue shouldBe mapOf("departmentId" to 10L)
                history.newValue shouldBe mapOf("departmentId" to 20L)
                history.createdByEmploymentId shouldBe 100L
                history.note shouldBe "부서 이동"
            }
        }
    }

    given("EmploymentHistory.markCancelled()를 호출하면") {
        val history = EmploymentHistory.create(
            employmentId = 1L,
            eventType = EmploymentHistoryEventType.PROMOTION,
            newValue = mapOf("positionId" to 2L),
            effectiveDate = effectiveDate,
            createdAt = now,
            oldValue = mapOf("positionId" to 1L),
        )

        `when`("취소 시각을 전달하면") {
            val cancelledAt = now.plusHours(1)
            history.markCancelled(cancelledAt)

            then("cancelledAt이 기록된다") {
                history.cancelledAt shouldNotBeNull { this shouldBe cancelledAt }
            }
        }
    }

    given("EmploymentHistory의 불변성을 확인하면") {

        then("constructor가 module-internal 가시성을 가진다 — 외부 모듈에서 직접 생성 불가") {
            val primaryConstructor = EmploymentHistory::class.constructors.single()
            primaryConstructor.visibility shouldBe kotlin.reflect.KVisibility.INTERNAL
        }

        then("cancelledAt 외의 공개 변경 가능 프로퍼티(public var)가 없다") {
            val publicVarProperties = EmploymentHistory::class.members
                .filterIsInstance<kotlin.reflect.KMutableProperty<*>>()
                .filter { it.visibility == kotlin.reflect.KVisibility.PUBLIC }
                .map { it.name }
            publicVarProperties shouldBe listOf("cancelledAt")
        }
    }

    given("EmploymentHistoryEventType enum을 확인하면") {
        then("7종 이벤트 타입이 정확히 존재한다") {
            val typeNames = EmploymentHistoryEventType.values().map { it.name }
            typeNames shouldBe listOf(
                "HIRE", "PROMOTION", "DEPT_CHANGE", "SALARY_CHANGE", "SUSPEND", "RESUME", "RESIGN",
            )
        }
    }
})
