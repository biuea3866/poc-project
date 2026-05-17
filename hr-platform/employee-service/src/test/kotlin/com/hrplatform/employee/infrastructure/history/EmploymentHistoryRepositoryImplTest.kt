package com.hrplatform.employee.infrastructure.history

import com.hrplatform.employee.domain.history.EmploymentHistory
import com.hrplatform.employee.domain.history.EmploymentHistoryEventType
import com.hrplatform.employee.domain.history.EmploymentHistoryRepository
import com.hrplatform.employee.support.BaseIntegrationTest
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EmploymentHistoryRepositoryImplTest(
    @Autowired private val employmentHistoryRepository: EmploymentHistoryRepository,
    @Autowired private val employmentHistoryJpaRepository: EmploymentHistoryJpaRepository,
) : BaseIntegrationTest() {

    private val effectiveDateBase: LocalDate = LocalDate.of(2026, 1, 1)

    init {
        beforeEach {
            employmentHistoryJpaRepository.deleteAll()
        }

        given("EmploymentHistory 를 save() 하면") {
            val history = EmploymentHistory.create(
                employmentId = 100L,
                eventType = EmploymentHistoryEventType.HIRE,
                oldValue = null,
                newValue = mapOf("departmentId" to 10L, "employeeNumber" to "EMP-001"),
                effectiveDate = effectiveDateBase,
                createdByEmploymentId = 1L,
                note = "신규 입사",
            )

            `when`("저장 후 findById() 로 조회하면") {
                val saved = employmentHistoryRepository.save(history)

                val found = employmentHistoryRepository.findById(saved.id!!)

                then("저장된 이력이 조회된다") {
                    found.shouldNotBeNull()
                    found.employmentId shouldBe 100L
                    found.eventType shouldBe EmploymentHistoryEventType.HIRE
                    found.note shouldBe "신규 입사"
                    found.createdByEmploymentId shouldBe 1L
                }

                then("JSON newValue 가 round-trip 으로 복원된다") {
                    found!!.newValue["departmentId"] shouldBe 10L
                    found.newValue["employeeNumber"] shouldBe "EMP-001"
                }

                then("oldValue 는 null 이다") {
                    found!!.oldValue.shouldBeNull()
                }
            }
        }

        given("동일 employmentId 에 대해 effectiveDate 가 다른 이력 3건을 저장하면") {
            val employmentId = 200L

            `when`("findByEmploymentId() 로 조회하면") {
                val history1 = EmploymentHistory.create(
                    employmentId = employmentId,
                    eventType = EmploymentHistoryEventType.HIRE,
                    oldValue = null,
                    newValue = mapOf("positionId" to 1L),
                    effectiveDate = LocalDate.of(2025, 1, 1),
                )
                val history2 = EmploymentHistory.create(
                    employmentId = employmentId,
                    eventType = EmploymentHistoryEventType.PROMOTION,
                    oldValue = mapOf("positionId" to 1L),
                    newValue = mapOf("positionId" to 2L),
                    effectiveDate = LocalDate.of(2025, 6, 1),
                )
                val history3 = EmploymentHistory.create(
                    employmentId = employmentId,
                    eventType = EmploymentHistoryEventType.DEPT_CHANGE,
                    oldValue = mapOf("departmentId" to 10L),
                    newValue = mapOf("departmentId" to 20L),
                    effectiveDate = LocalDate.of(2026, 1, 1),
                )
                employmentHistoryRepository.save(history1)
                employmentHistoryRepository.save(history2)
                employmentHistoryRepository.save(history3)

                val results = employmentHistoryRepository.findByEmploymentId(employmentId)

                then("3건이 조회된다") {
                    results.size shouldBe 3
                }

                then("effectiveDate DESC 순으로 정렬된다") {
                    results[0].effectiveDate shouldBe LocalDate.of(2026, 1, 1)
                    results[1].effectiveDate shouldBe LocalDate.of(2025, 6, 1)
                    results[2].effectiveDate shouldBe LocalDate.of(2025, 1, 1)
                }
            }
        }

        given("동일 employmentId 에 대해 이력 2건을 저장하면") {
            val employmentId = 300L

            `when`("findLastByEmploymentId() 로 조회하면") {
                val olderHistory = EmploymentHistory.create(
                    employmentId = employmentId,
                    eventType = EmploymentHistoryEventType.HIRE,
                    oldValue = null,
                    newValue = mapOf("positionId" to 1L),
                    effectiveDate = LocalDate.of(2025, 1, 1),
                )
                val newerHistory = EmploymentHistory.create(
                    employmentId = employmentId,
                    eventType = EmploymentHistoryEventType.PROMOTION,
                    oldValue = mapOf("positionId" to 1L),
                    newValue = mapOf("positionId" to 3L),
                    effectiveDate = LocalDate.of(2026, 3, 1),
                )
                employmentHistoryRepository.save(olderHistory)
                employmentHistoryRepository.save(newerHistory)

                val last = employmentHistoryRepository.findLastByEmploymentId(employmentId)

                then("effectiveDate 가 가장 최신인 이력이 반환된다") {
                    last.shouldNotBeNull()
                    last.effectiveDate shouldBe LocalDate.of(2026, 3, 1)
                    last.eventType shouldBe EmploymentHistoryEventType.PROMOTION
                }
            }
        }

        given("존재하지 않는 id 로 findById() 를 호출하면") {
            `when`("조회하면") {
                val result = employmentHistoryRepository.findById(999999L)
                then("null 을 반환한다") {
                    result.shouldBeNull()
                }
            }
        }

        given("이력이 없는 employmentId 로 findLastByEmploymentId() 를 호출하면") {
            `when`("조회하면") {
                val result = employmentHistoryRepository.findLastByEmploymentId(999999L)
                then("null 을 반환한다") {
                    result.shouldBeNull()
                }
            }
        }

        given("oldValue 와 newValue 에 복잡한 JSON 을 저장하면") {
            val complexOldValue = mapOf(
                "departmentId" to 10L,
                "positionId" to 1L,
                "baseSalary" to 5000000L,
            )
            val complexNewValue = mapOf(
                "departmentId" to 20L,
                "positionId" to 2L,
                "baseSalary" to 6000000L,
            )

            `when`("findById() 로 조회하면") {
                val history = EmploymentHistory.create(
                    employmentId = 400L,
                    eventType = EmploymentHistoryEventType.PROMOTION,
                    oldValue = complexOldValue,
                    newValue = complexNewValue,
                    effectiveDate = effectiveDateBase,
                )
                val saved = employmentHistoryRepository.save(history)

                val found = employmentHistoryRepository.findById(saved.id!!)

                then("JSON 컬럼 round-trip 이 정확하다") {
                    found.shouldNotBeNull()
                    found.oldValue.shouldNotBeNull()
                    found.oldValue!!["departmentId"] shouldBe 10L
                    found.oldValue!!["positionId"] shouldBe 1L
                    found.oldValue!!["baseSalary"] shouldBe 5000000L
                    found.newValue["departmentId"] shouldBe 20L
                    found.newValue["positionId"] shouldBe 2L
                    found.newValue["baseSalary"] shouldBe 6000000L
                }
            }
        }

        given("다른 employmentId 의 이력이 섞여 있을 때") {
            val targetEmploymentId = 500L
            val otherEmploymentId = 600L

            `when`("findByEmploymentId(targetEmploymentId) 를 호출하면") {
                val targetHistory = EmploymentHistory.create(
                    employmentId = targetEmploymentId,
                    eventType = EmploymentHistoryEventType.HIRE,
                    oldValue = null,
                    newValue = mapOf("positionId" to 1L),
                    effectiveDate = effectiveDateBase,
                )
                val otherHistory = EmploymentHistory.create(
                    employmentId = otherEmploymentId,
                    eventType = EmploymentHistoryEventType.HIRE,
                    oldValue = null,
                    newValue = mapOf("positionId" to 1L),
                    effectiveDate = effectiveDateBase,
                )
                employmentHistoryRepository.save(targetHistory)
                employmentHistoryRepository.save(otherHistory)

                val results = employmentHistoryRepository.findByEmploymentId(targetEmploymentId)

                then("다른 employment 의 이력은 포함되지 않는다") {
                    results.size shouldBe 1
                    results[0].employmentId shouldBe targetEmploymentId
                }
            }
        }
    }
}
