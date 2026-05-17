package com.hrplatform.employee.presentation.employee

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.core.exception.ForbiddenException
import com.hrplatform.employee.application.employee.CancelEmploymentEventResult
import com.hrplatform.employee.application.employee.CancelEmploymentEventUseCase
import com.hrplatform.employee.application.employee.GetEmployeeCommand
import com.hrplatform.employee.application.employee.GetEmployeeResult
import com.hrplatform.employee.application.employee.GetEmployeeUseCase
import com.hrplatform.employee.application.employee.GetEmploymentHistoryUseCase
import com.hrplatform.employee.application.employee.HireEmployeeResult
import com.hrplatform.employee.application.employee.HireEmployeeUseCase
import com.hrplatform.employee.application.employee.RecordEmploymentEventUseCase
import com.hrplatform.employee.application.employee.ResignEmploymentUseCase
import com.hrplatform.employee.application.employee.ResumeEmploymentResult
import com.hrplatform.employee.application.employee.ResumeEmploymentUseCase
import com.hrplatform.employee.application.employee.SearchEmployeesUseCase
import com.hrplatform.employee.application.employee.SuspendEmploymentResult
import com.hrplatform.employee.application.employee.SuspendEmploymentUseCase
import com.hrplatform.employee.domain.employment.EmploymentStatus
import com.hrplatform.employee.domain.employment.EmploymentType
import com.hrplatform.employee.presentation.auth.AuthEmploymentIdArgumentResolver
import com.hrplatform.employee.presentation.config.WebMvcConfig
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

@WebMvcTest(EmployeeApiController::class)
@Import(
    EmployeeApiControllerTest.MockUseCaseConfig::class,
    AuthEmploymentIdArgumentResolver::class,
    WebMvcConfig::class,
)
@ContextConfiguration
class EmployeeApiControllerTest(
    private val webApplicationContext: WebApplicationContext,
    private val objectMapper: ObjectMapper,
    private val hireUseCase: HireEmployeeUseCase,
    private val getUseCase: GetEmployeeUseCase,
    private val suspendUseCase: SuspendEmploymentUseCase,
    private val resumeUseCase: ResumeEmploymentUseCase,
    private val cancelEventUseCase: CancelEmploymentEventUseCase,
) : BehaviorSpec({

    val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

    given("POST /employees") {
        `when`("유효한 입사 요청이 오면") {
            val request = mapOf(
                "personalEmail" to "test@example.com",
                "name" to "홍길동",
                "companyId" to 1,
                "employeeNumber" to "EMP-001",
                "employmentType" to "REGULAR",
                "startDate" to "2026-01-01",
                "country" to "KR",
                "currency" to "KRW",
                "timezone" to "Asia/Seoul",
            )
            val hireResult = HireEmployeeResult(
                employmentId = 1L,
                personId = 10L,
                companyId = 1L,
                employeeNumber = "EMP-001",
                employmentType = EmploymentType.REGULAR,
                status = EmploymentStatus.ACTIVE,
                startDate = LocalDate.of(2026, 1, 1),
            )
            every { hireUseCase.execute(any(), any()) } returns hireResult

            then("201 Created와 HireResult를 반환한다") {
                mockMvc.post("/employees") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.employmentId") { value(1) }
                    jsonPath("$.employeeNumber") { value("EMP-001") }
                }
            }
        }
    }

    given("GET /employees/{id}") {
        `when`("TEAM_LEAD가 다른 팀 직원을 조회하면") {
            every { getUseCase.execute(GetEmployeeCommand(viewerEmploymentId = 1L, targetEmploymentId = 2L)) } throws
                ForbiddenException("ACCESS_DENIED", "접근 권한이 없습니다")

            then("403 Forbidden을 반환한다") {
                mockMvc.get("/employees/2") {
                    header("X-Employment-Id", "1")
                }.andExpect {
                    status { isForbidden() }
                }
            }
        }

        `when`("접근 가능한 직원을 조회하면") {
            val getResult = GetEmployeeResult(
                employmentId = 2L,
                personId = 20L,
                companyId = 1L,
                employeeNumber = "EMP-002",
                employmentType = EmploymentType.REGULAR,
                status = EmploymentStatus.ACTIVE,
                startDate = LocalDate.of(2026, 1, 1),
                departmentId = null,
                positionId = null,
                baseSalary = null,
            )
            every { getUseCase.execute(GetEmployeeCommand(viewerEmploymentId = 100L, targetEmploymentId = 2L)) } returns getResult

            then("200 OK와 직원 정보를 반환한다") {
                mockMvc.get("/employees/2") {
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.employmentId") { value(2) }
                }
            }
        }
    }

    given("POST /employees/{id}/suspend") {
        `when`("유효한 휴직 요청이 오면") {
            val suspendRequest = mapOf("reason" to "개인 사유")
            val suspendResult = SuspendEmploymentResult(employmentId = 1L, status = EmploymentStatus.ON_LEAVE)
            every { suspendUseCase.execute(any(), any()) } returns suspendResult

            then("200 OK와 SuspendResult를 반환한다") {
                mockMvc.post("/employees/1/suspend") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(suspendRequest)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("ON_LEAVE") }
                }
            }
        }
    }

    given("POST /employees/{id}/resume") {
        `when`("유효한 복직 요청이 오면") {
            val resumeResult = ResumeEmploymentResult(employmentId = 1L, status = EmploymentStatus.ACTIVE)
            every { resumeUseCase.execute(any(), any()) } returns resumeResult

            then("200 OK와 ResumeResult를 반환한다") {
                mockMvc.post("/employees/1/resume") {
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.status") { value("ACTIVE") }
                }
            }
        }
    }

    given("DELETE /employees/{id}/employment-events/{eventId}") {
        `when`("유효한 발령 취소 요청이 오면") {
            val cancelRequest = mapOf("cancellationReason" to "오기입")
            val cancelResult = CancelEmploymentEventResult(employmentId = 1L, status = EmploymentStatus.ACTIVE)
            every { cancelEventUseCase.execute(any(), any()) } returns cancelResult

            then("200 OK를 반환한다") {
                mockMvc.delete("/employees/1/employment-events/10") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(cancelRequest)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                }
            }
        }
    }
}) {

    @TestConfiguration
    class MockUseCaseConfig {
        @Bean fun hireUseCase() = mockk<HireEmployeeUseCase>(relaxed = true)
        @Bean fun resignUseCase() = mockk<ResignEmploymentUseCase>(relaxed = true)
        @Bean fun recordEventUseCase() = mockk<RecordEmploymentEventUseCase>(relaxed = true)
        @Bean fun cancelUseCase() = mockk<CancelEmploymentEventUseCase>(relaxed = true)
        @Bean fun suspendUseCase() = mockk<SuspendEmploymentUseCase>(relaxed = true)
        @Bean fun resumeUseCase() = mockk<ResumeEmploymentUseCase>(relaxed = true)
        @Bean fun searchUseCase() = mockk<SearchEmployeesUseCase>(relaxed = true)
        @Bean fun getUseCase() = mockk<GetEmployeeUseCase>(relaxed = true)
        @Bean fun getHistoryUseCase() = mockk<GetEmploymentHistoryUseCase>(relaxed = true)
    }
}
