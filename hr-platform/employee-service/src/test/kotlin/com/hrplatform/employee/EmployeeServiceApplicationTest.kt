package com.hrplatform.employee

import com.hrplatform.employee.infrastructure.department.DepartmentJpaRepository
import com.hrplatform.employee.infrastructure.employment.EmploymentJpaRepository
import com.hrplatform.employee.infrastructure.history.EmploymentHistoryJpaRepository
import com.hrplatform.employee.infrastructure.person.PersonJpaRepository
import com.ninjasquad.springmockk.MockkBean
import com.querydsl.jpa.impl.JPAQueryFactory
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class EmployeeServiceApplicationTest(
    private val applicationContext: ApplicationContext,
) : BehaviorSpec({

    given("EmployeeServiceApplication") {
        `when`("Spring 컨텍스트가 로드되면") {
            then("ApplicationContext 가 null 이 아니다") {
                applicationContext shouldNotBe null
            }
        }
    }
}) {
    @MockkBean
    lateinit var departmentJpaRepository: DepartmentJpaRepository

    @MockkBean
    lateinit var personJpaRepository: PersonJpaRepository

    @MockkBean
    lateinit var employmentHistoryJpaRepository: EmploymentHistoryJpaRepository

    @MockkBean
    lateinit var employmentJpaRepository: EmploymentJpaRepository

    @MockkBean
    lateinit var jpaQueryFactory: JPAQueryFactory
}
