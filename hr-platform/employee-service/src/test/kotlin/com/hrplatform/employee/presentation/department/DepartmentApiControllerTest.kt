package com.hrplatform.employee.presentation.department

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.employee.application.department.AssignDepartmentHeadUseCase
import com.hrplatform.employee.application.department.CreateDepartmentUseCase
import com.hrplatform.employee.application.department.DepartmentResult
import com.hrplatform.employee.application.department.MoveDepartmentUseCase
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate

@WebMvcTest(DepartmentApiController::class)
@Import(
    DepartmentApiControllerTest.MockUseCaseConfig::class,
    AuthEmploymentIdArgumentResolver::class,
    WebMvcConfig::class,
    GlobalExceptionHandler::class,
)
@ContextConfiguration
class DepartmentApiControllerTest(
    private val webApplicationContext: WebApplicationContext,
    private val objectMapper: ObjectMapper,
    private val createUseCase: CreateDepartmentUseCase,
    private val moveUseCase: MoveDepartmentUseCase,
    private val assignHeadUseCase: AssignDepartmentHeadUseCase,
) : BehaviorSpec({

    val mockMvc: MockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

    given("POST /departments") {
        `when`("유효한 부서 생성 요청이 오면") {
            val request = mapOf(
                "companyId" to 1,
                "name" to "개발팀",
                "code" to "DEV",
                "orderNo" to 1,
                "effectiveFrom" to "2026-01-01",
            )
            val departmentResult = DepartmentResult(
                departmentId = 1L,
                companyId = 1L,
                name = "개발팀",
                code = "DEV",
                parentId = null,
                path = "/1/",
                headEmploymentId = null,
                effectiveFrom = LocalDate.of(2026, 1, 1),
            )
            every { createUseCase.execute(any()) } returns departmentResult

            then("201 Created와 DepartmentResult를 반환한다") {
                mockMvc.post("/departments") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isCreated() }
                    jsonPath("$.departmentId") { value(1) }
                    jsonPath("$.name") { value("개발팀") }
                }
            }
        }
    }

    given("PATCH /departments/{id}") {
        `when`("부서 이동 요청이 오면") {
            val request = mapOf("newParentId" to 2)
            val moveResult = DepartmentResult(
                departmentId = 1L,
                companyId = 1L,
                name = "개발팀",
                code = "DEV",
                parentId = 2L,
                path = "/1/2/1/",
                headEmploymentId = null,
                effectiveFrom = LocalDate.of(2026, 1, 1),
            )
            every { moveUseCase.execute(any()) } returns moveResult

            then("200 OK와 변경된 DepartmentResult를 반환한다") {
                mockMvc.patch("/departments/1") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.parentId") { value(2) }
                }
            }
        }
    }

    given("PATCH /departments/{id}/head") {
        `when`("부서장 지정 요청이 오면") {
            val request = mapOf("employmentId" to 50)
            val headResult = DepartmentResult(
                departmentId = 1L,
                companyId = 1L,
                name = "개발팀",
                code = "DEV",
                parentId = null,
                path = "/1/",
                headEmploymentId = 50L,
                effectiveFrom = LocalDate.of(2026, 1, 1),
            )
            every { assignHeadUseCase.execute(any()) } returns headResult

            then("200 OK와 headEmploymentId가 설정된 DepartmentResult를 반환한다") {
                mockMvc.patch("/departments/1/head") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                    header("X-Employment-Id", "100")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.headEmploymentId") { value(50) }
                }
            }
        }
    }
}) {

    @TestConfiguration
    class MockUseCaseConfig {
        @Bean fun createDepartmentUseCase() = mockk<CreateDepartmentUseCase>(relaxed = true)
        @Bean fun moveDepartmentUseCase() = mockk<MoveDepartmentUseCase>(relaxed = true)
        @Bean fun assignDepartmentHeadUseCase() = mockk<AssignDepartmentHeadUseCase>(relaxed = true)
    }
}
