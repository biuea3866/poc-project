package com.hrplatform.auth.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.auth.application.admin.AssignRoleUseCase
import com.hrplatform.auth.application.admin.IssueApiTokenUseCase
import com.hrplatform.auth.application.admin.ListRolesUseCase
import com.hrplatform.auth.application.admin.LogoutAllSessionsUseCase
import com.hrplatform.auth.application.admin.RevokeApiTokenUseCase
import com.hrplatform.auth.application.admin.RevokeRoleUseCase
import com.hrplatform.auth.application.admin.UnlockUserAccountUseCase
import com.hrplatform.auth.domain.auth.service.ApiTokenResult
import com.hrplatform.auth.presentation.auth.AuthEmploymentIdArgumentResolver
import com.hrplatform.auth.presentation.auth.JwtAuthenticationToken
import com.hrplatform.core.exception.ForbiddenException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AdminAuthControllerTest : BehaviorSpec({

    val unlockUseCase = mockk<UnlockUserAccountUseCase>()
    val logoutAllUseCase = mockk<LogoutAllSessionsUseCase>()
    val issueApiTokenUseCase = mockk<IssueApiTokenUseCase>()
    val revokeApiTokenUseCase = mockk<RevokeApiTokenUseCase>()
    val assignRoleUseCase = mockk<AssignRoleUseCase>()
    val revokeRoleUseCase = mockk<RevokeRoleUseCase>()
    val listRolesUseCase = mockk<ListRolesUseCase>()

    val controller = AdminAuthController(
        unlockUseCase = unlockUseCase,
        logoutAllUseCase = logoutAllUseCase,
        issueApiTokenUseCase = issueApiTokenUseCase,
        revokeApiTokenUseCase = revokeApiTokenUseCase,
        assignRoleUseCase = assignRoleUseCase,
        revokeRoleUseCase = revokeRoleUseCase,
        listRolesUseCase = listRolesUseCase,
    )

    val objectMapper = ObjectMapper().findAndRegisterModules()
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .setCustomArgumentResolvers(AuthEmploymentIdArgumentResolver())
        .build()

    val now = ZonedDateTime.now(ZoneOffset.UTC)

    fun setAuthentication(employmentId: Long) {
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(
            userAccountId = 1L,
            employmentId = employmentId,
        )
    }

    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    afterEach { clearAuthentication() }

    given("POST /auth/admin/users/{id}/unlock - HR_MANAGER 권한 사용자") {
        then("200 반환") {
            setAuthentication(999L)
            every { unlockUseCase.execute(any()) } returns Unit

            mockMvc.post("/auth/admin/users/10/unlock").andExpect {
                status { isOk() }
            }
        }
    }

    given("POST /auth/admin/users/{id}/unlock - ForbiddenException 발생") {
        then("403 반환") {
            setAuthentication(999L)
            every { unlockUseCase.execute(any()) } throws ForbiddenException(
                errorCode = "FORBIDDEN",
                message = "권한이 없습니다",
            )

            mockMvc.post("/auth/admin/users/10/unlock").andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("FORBIDDEN") }
            }
        }
    }

    given("POST /auth/admin/api-tokens - HR_MANAGER 권한 사용자") {
        then("200 + ApiTokenIssueResponse 반환 (rawToken 포함)") {
            setAuthentication(999L)
            every { issueApiTokenUseCase.execute(any()) } returns ApiTokenResult(
                apiTokenId = 1L,
                rawToken = "hrp_rawtoken",
                name = "test-token",
                scopes = listOf("read"),
                expiresAt = now.plusDays(30),
            )

            mockMvc.post("/auth/admin/api-tokens") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "name" to "test-token",
                        "scopes" to listOf("read"),
                        "expiresAt" to null,
                    ),
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.rawToken") { value("hrp_rawtoken") }
            }
        }
    }
})
