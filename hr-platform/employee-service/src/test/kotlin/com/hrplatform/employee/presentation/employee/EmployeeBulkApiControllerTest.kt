package com.hrplatform.employee.presentation.employee

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.employee.application.employee.BulkHireUseCase
import com.hrplatform.employee.application.employee.BulkRecordEmploymentEventsUseCase
import com.hrplatform.employee.application.employee.BulkResult
import com.hrplatform.employee.presentation.auth.AuthEmploymentIdArgumentResolver
import com.hrplatform.employee.presentation.config.GlobalExceptionHandler
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
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

@WebMvcTest(EmployeeBulkApiController::class)
@Import(
    EmployeeBulkApiControllerTest.MockUseCaseConfig::class,
    AuthEmploymentIdArgumentResolver::class,
    WebMvcConfig::class,
    GlobalExceptionHandler::class,
)
@ContextConfiguration
class EmployeeBulkApiControllerTest(
    private val webApplicationContext: WebApplicationContext,
    private val objectMapper: ObjectMapper,
    private val bulkHireUseCase: BulkHireUseCase,
) : BehaviorSpec({

    val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

    given("POST /employees/bulk") {
        `when`("3명 일괄 입사 요청이 오면") {
            val requests = listOf(
                mapOf(
                    "personalEmail" to "a@example.com",
                    "name" to "직원A",
                    "companyId" to 1,
                    "employeeNumber" to "EMP-A01",
                    "employmentType" to "REGULAR",
                    "startDate" to "2026-01-01",
                    "country" to "KR",
                    "currency" to "KRW",
                    "timezone" to "Asia/Seoul",
                ),
                mapOf(
                    "personalEmail" to "b@example.com",
                    "name" to "직원B",
                    "companyId" to 1,
                    "employeeNumber" to "EMP-B01",
                    "employmentType" to "REGULAR",
                    "startDate" to "2026-01-01",
                    "country" to "KR",
                    "currency" to "KRW",
                    "timezone" to "Asia/Seoul",
                ),
                mapOf(
                    "personalEmail" to "c@example.com",
                    "name" to "직원C",
                    "companyId" to 1,
                    "employeeNumber" to "EMP-C01",
                    "employmentType" to "REGULAR",
                    "startDate" to "2026-01-01",
                    "country" to "KR",
                    "currency" to "KRW",
                    "timezone" to "Asia/Seoul",
                ),
            )
            val bulkResult = BulkResult(successCount = 3, failureCount = 0, failures = emptyList())
            every { bulkHireUseCase.execute(any(), any()) } returns bulkResult

            then("200 OK와 successCount=3, failureCount=0을 반환한다") {
                mockMvc.post("/employees/bulk") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(requests)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.successCount") { value(3) }
                    jsonPath("$.failureCount") { value(0) }
                }
            }
        }
    }
}) {

    @TestConfiguration
    class MockUseCaseConfig {
        @Bean fun bulkHireUseCase() = mockk<BulkHireUseCase>(relaxed = true)
        @Bean fun bulkRecordEmploymentEventsUseCase() = mockk<BulkRecordEmploymentEventsUseCase>(relaxed = true)
    }
}
