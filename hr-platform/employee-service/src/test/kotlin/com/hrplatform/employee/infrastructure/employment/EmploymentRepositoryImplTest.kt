package com.hrplatform.employee.infrastructure.employment

import com.hrplatform.employee.domain.department.Department
import com.hrplatform.employee.domain.employment.Employment
import com.hrplatform.employee.domain.employment.EmploymentRepository
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import com.hrplatform.employee.infrastructure.department.DepartmentJpaRepository
import com.hrplatform.employee.support.BaseIntegrationTest
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.ZonedDateTime

@Suppress("LongMethod")
class EmploymentRepositoryImplTest(
    @Autowired private val employmentRepository: EmploymentRepository,
    @Autowired private val employmentJpaRepository: EmploymentJpaRepository,
    @Autowired private val departmentJpaRepository: DepartmentJpaRepository,
) : BaseIntegrationTest() {

    private val startDate = LocalDate.of(2026, 1, 1)

    private fun makeEmployment(
        companyId: Long = 1L,
        personId: Long = 100L,
        employeeNumber: String = "EMP-001",
        departmentId: Long? = null,
        managerEmploymentId: Long? = null,
        status: EmploymentStatus = EmploymentStatus.ACTIVE,
    ): Employment = Employment(
        personId = personId,
        companyId = companyId,
        employeeNumber = employeeNumber,
        employmentType = EmploymentType.REGULAR,
        status = status,
        startDate = startDate,
        country = "KR",
        currency = "KRW",
        timezone = "Asia/Seoul",
        departmentId = departmentId,
        managerEmploymentId = managerEmploymentId,
    )

    private fun makeDepartment(
        companyId: Long = 1L,
        name: String,
        code: String,
        path: String,
        parentId: Long? = null,
    ): Department = Department(
        companyId = companyId,
        name = name,
        code = code,
        path = path,
        parentId = parentId,
        orderNo = 0,
        effectiveFrom = startDate,
    )

    init {
        beforeEach {
            employmentJpaRepository.deleteAll()
            departmentJpaRepository.deleteAll()
        }

        given("Employment를 save() 하고 findById()로 조회하면") {
            `when`("저장 후 id로 조회하면") {
                val employment = makeEmployment()
                val saved = employmentRepository.save(employment)

                val found = employmentRepository.findById(saved.id!!)

                then("저장된 엔티티가 반환된다") {
                    found.shouldNotBeNull()
                    found.employeeNumber shouldBe "EMP-001"
                    found.companyId shouldBe 1L
                    found.status shouldBe EmploymentStatus.ACTIVE
                }

                then("audit 컬럼(createdAt/updatedAt)이 자동으로 채워진다") {
                    found!!.createdAt.shouldNotBeNull()
                    found.updatedAt.shouldNotBeNull()
                }
            }

            `when`("존재하지 않는 id로 조회하면") {
                val result = employmentRepository.findById(Long.MAX_VALUE)

                then("null을 반환한다") {
                    result.shouldBeNull()
                }
            }
        }

        given("findByCompanyIdAndEmployeeNumber() — companyId와 employeeNumber가 정확히 일치하면") {
            `when`("일치하는 데이터를 저장하고 조회하면") {
                employmentRepository.save(makeEmployment(companyId = 10L, employeeNumber = "EMP-UNIQUE-001"))

                val found = employmentRepository.findByCompanyIdAndEmployeeNumber(10L, "EMP-UNIQUE-001")

                then("해당 Employment를 반환한다") {
                    found.shouldNotBeNull()
                    found.employeeNumber shouldBe "EMP-UNIQUE-001"
                }
            }

            `when`("다른 companyId로 조회하면") {
                val result = employmentRepository.findByCompanyIdAndEmployeeNumber(99L, "EMP-UNIQUE-001")

                then("null을 반환한다") {
                    result.shouldBeNull()
                }
            }

            `when`("soft-delete된 Employment를 조회하면") {
                val employment = makeEmployment(companyId = 11L, employeeNumber = "EMP-DELETED-001")
                val saved = employmentRepository.save(employment)
                saved.softDelete(ZonedDateTime.now(), null)
                employmentRepository.save(saved)

                val result = employmentRepository.findByCompanyIdAndEmployeeNumber(11L, "EMP-DELETED-001")

                then("null을 반환한다") {
                    result.shouldBeNull()
                }
            }
        }

        given("findByPersonId() — 동일 personId에 Employment 2건이 있으면") {
            `when`("저장 후 personId로 조회하면") {
                val personId = 200L
                employmentRepository.save(makeEmployment(companyId = 1L, personId = personId, employeeNumber = "EMP-P-001"))
                employmentRepository.save(makeEmployment(companyId = 2L, personId = personId, employeeNumber = "EMP-P-002"))

                val results = employmentRepository.findByPersonId(personId)

                then("2건 모두 반환된다") {
                    results shouldHaveSize 2
                }
            }

            `when`("soft-delete된 Employment가 있으면") {
                val personId = 201L
                val saved = employmentRepository.save(makeEmployment(companyId = 1L, personId = personId, employeeNumber = "EMP-P-DEL-001"))
                saved.softDelete(ZonedDateTime.now(), null)
                employmentRepository.save(saved)

                val results = employmentRepository.findByPersonId(personId)

                then("결과에 포함되지 않는다") {
                    results shouldHaveSize 0
                }
            }
        }

        given("findByDepartmentTreePath() — Department path 하위 직원 조회") {
            `when`("루트 부서 직원 1명 + 하위 부서 직원 1명 + 별개 부서 직원 1명이 있으면") {
                val root = departmentJpaRepository.save(
                    makeDepartment(name = "루트", code = "ROOT-DTP", path = "/placeholder/"),
                )
                root.path = "/${root.id}/"
                departmentJpaRepository.save(root)

                val child = departmentJpaRepository.save(
                    makeDepartment(name = "하위부서", code = "CHILD-DTP", path = "${root.path}placeholder/", parentId = root.id),
                )
                child.path = "${root.path}${child.id}/"
                departmentJpaRepository.save(child)

                val other = departmentJpaRepository.save(
                    makeDepartment(name = "별개부서", code = "OTHER-DTP", path = "/placeholder2/"),
                )
                other.path = "/${other.id}/"
                departmentJpaRepository.save(other)

                employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 301L, employeeNumber = "EMP-DTP-001", departmentId = root.id),
                )
                employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 302L, employeeNumber = "EMP-DTP-002", departmentId = child.id),
                )
                employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 303L, employeeNumber = "EMP-DTP-003", departmentId = other.id),
                )

                val results = employmentRepository.findByDepartmentTreePath(root.path)

                then("루트 path prefix로 조회하면 루트+하위 부서 직원 2건이 반환된다") {
                    results shouldHaveSize 2
                    results.map { it.employeeNumber } shouldContainExactlyInAnyOrder listOf("EMP-DTP-001", "EMP-DTP-002")
                }

                then("별개 부서 직원은 포함되지 않는다") {
                    results.none { it.employeeNumber == "EMP-DTP-003" } shouldBe true
                }
            }

            `when`("soft-delete된 Employment가 있으면") {
                val dept = departmentJpaRepository.save(
                    makeDepartment(name = "소프트딜리트팀", code = "SD-DTP", path = "/placeholder/"),
                )
                dept.path = "/${dept.id}/"
                departmentJpaRepository.save(dept)

                val savedEmployment = employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 400L, employeeNumber = "EMP-SD-DTP-001", departmentId = dept.id),
                )
                savedEmployment.softDelete(ZonedDateTime.now(), null)
                employmentRepository.save(savedEmployment)

                val results = employmentRepository.findByDepartmentTreePath(dept.path)

                then("결과에 포함되지 않는다") {
                    results shouldHaveSize 0
                }
            }
        }

        given("findManagedBy() — managerEmploymentId로 부하직원 조회") {
            `when`("managerEmploymentId가 일치하는 직원 2명이 있으면") {
                val manager = employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 500L, employeeNumber = "EMP-MGR-001"),
                )
                employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 501L, employeeNumber = "EMP-SUB-001", managerEmploymentId = manager.id),
                )
                employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 502L, employeeNumber = "EMP-SUB-002", managerEmploymentId = manager.id),
                )

                val results = employmentRepository.findManagedBy(manager.id!!)

                then("2건이 반환된다") {
                    results shouldHaveSize 2
                    results.map { it.employeeNumber } shouldContainExactlyInAnyOrder listOf("EMP-SUB-001", "EMP-SUB-002")
                }
            }

            `when`("soft-delete된 부하직원이 있으면") {
                val manager = employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 600L, employeeNumber = "EMP-MGR-002"),
                )
                val subordinate = employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 601L, employeeNumber = "EMP-SUB-DEL-001", managerEmploymentId = manager.id),
                )
                subordinate.softDelete(ZonedDateTime.now(), null)
                employmentRepository.save(subordinate)

                val results = employmentRepository.findManagedBy(manager.id!!)

                then("결과에 포함되지 않는다") {
                    results shouldHaveSize 0
                }
            }
        }

        given("findByCompanyIdAndStatus() — companyId + status 조합으로 조회") {
            `when`("companyId=1 ACTIVE 2명, ON_LEAVE 1명, companyId=2 ACTIVE 1명이 있으면") {
                employmentRepository.save(makeEmployment(companyId = 1L, personId = 700L, employeeNumber = "EMP-STS-001", status = EmploymentStatus.ACTIVE))
                employmentRepository.save(makeEmployment(companyId = 1L, personId = 701L, employeeNumber = "EMP-STS-002", status = EmploymentStatus.ACTIVE))
                employmentRepository.save(makeEmployment(companyId = 1L, personId = 702L, employeeNumber = "EMP-STS-003", status = EmploymentStatus.ON_LEAVE))
                employmentRepository.save(makeEmployment(companyId = 2L, personId = 703L, employeeNumber = "EMP-STS-004", status = EmploymentStatus.ACTIVE))

                val activeResults = employmentRepository.findByCompanyIdAndStatus(1L, EmploymentStatus.ACTIVE)
                val onLeaveResults = employmentRepository.findByCompanyIdAndStatus(1L, EmploymentStatus.ON_LEAVE)

                then("ACTIVE 조회 시 companyId=1 ACTIVE 2건만 반환된다") {
                    activeResults shouldHaveSize 2
                    activeResults.map { it.employeeNumber } shouldContainExactlyInAnyOrder listOf("EMP-STS-001", "EMP-STS-002")
                }

                then("ON_LEAVE 조회 시 1건이 반환된다") {
                    onLeaveResults shouldHaveSize 1
                    onLeaveResults[0].employeeNumber shouldBe "EMP-STS-003"
                }
            }

            `when`("soft-delete된 Employment가 있으면") {
                val saved = employmentRepository.save(
                    makeEmployment(companyId = 1L, personId = 800L, employeeNumber = "EMP-STS-DEL-001", status = EmploymentStatus.ACTIVE),
                )
                saved.softDelete(ZonedDateTime.now(), null)
                employmentRepository.save(saved)

                val results = employmentRepository.findByCompanyIdAndStatus(1L, EmploymentStatus.ACTIVE)

                then("결과에 포함되지 않는다") {
                    results.none { it.employeeNumber == "EMP-STS-DEL-001" } shouldBe true
                }
            }
        }
    }
}
