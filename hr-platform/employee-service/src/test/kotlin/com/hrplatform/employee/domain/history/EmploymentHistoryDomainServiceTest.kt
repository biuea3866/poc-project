package com.hrplatform.employee.domain.history

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeSortedBy
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate

class EmploymentHistoryDomainServiceTest : BehaviorSpec({

    val historyRepository = mockk<EmploymentHistoryRepository>()
    val historyDomainService = EmploymentHistoryDomainService(historyRepository)

    given("findByEmployment") {
        `when`("해당 employmentId에 이력이 N건 있으면") {
            val histories = listOf(
                buildHistory(id = 3L, effectiveDate = LocalDate.of(2026, 3, 1)),
                buildHistory(id = 1L, effectiveDate = LocalDate.of(2025, 1, 1)),
                buildHistory(id = 2L, effectiveDate = LocalDate.of(2025, 6, 1)),
            )
            every { historyRepository.findByEmploymentId(10L) } returns histories

            then("effectiveDate desc 순서로 정렬되어 반환된다") {
                val result = historyDomainService.findByEmployment(10L)
                result shouldBe histories.sortedByDescending { it.effectiveDate }
            }
        }

        `when`("이력이 없으면") {
            every { historyRepository.findByEmploymentId(99L) } returns emptyList()

            then("빈 리스트를 반환한다") {
                val result = historyDomainService.findByEmployment(99L)
                result shouldBe emptyList()
            }
        }
    }

    given("rebuildAt") {
        `when`("특정 날짜 이전의 이력들이 있으면") {
            val asOf = LocalDate.of(2026, 1, 1)
            val histories = listOf(
                buildHistory(
                    id = 1L,
                    effectiveDate = LocalDate.of(2025, 1, 1),
                    eventType = EmploymentHistoryEventType.HIRE,
                    oldValue = null,
                    newValue = mapOf("departmentId" to 10L),
                ),
                buildHistory(
                    id = 2L,
                    effectiveDate = LocalDate.of(2025, 6, 1),
                    eventType = EmploymentHistoryEventType.DEPT_CHANGE,
                    oldValue = mapOf("departmentId" to 10L),
                    newValue = mapOf("departmentId" to 20L),
                ),
            )
            every { historyRepository.findByEmploymentId(5L) } returns histories

            then("asOf 시점 이전 이력들을 누적해 상태 맵을 반환한다") {
                val result = historyDomainService.rebuildAt(employmentId = 5L, asOf = asOf)
                result["departmentId"] shouldBe 20L
            }
        }
    }
})

private fun buildHistory(
    id: Long,
    effectiveDate: LocalDate,
    eventType: EmploymentHistoryEventType = EmploymentHistoryEventType.HIRE,
    oldValue: Map<String, Any?>? = null,
    newValue: Map<String, Any?> = emptyMap(),
): EmploymentHistory {
    val history = EmploymentHistory.create(
        employmentId = 10L,
        eventType = eventType,
        oldValue = oldValue,
        newValue = newValue,
        effectiveDate = effectiveDate,
    )
    val idField = history.javaClass.superclass.getDeclaredField("id")
    idField.isAccessible = true
    idField.set(history, id)
    return history
}
