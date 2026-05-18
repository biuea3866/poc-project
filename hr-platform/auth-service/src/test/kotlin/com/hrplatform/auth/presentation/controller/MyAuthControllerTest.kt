package com.hrplatform.auth.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.auth.application.auth.ChangePasswordUseCase
import com.hrplatform.auth.application.auth.GetMeUseCase
import com.hrplatform.auth.application.auth.MeResult
import com.hrplatform.auth.application.twofactor.DisableTwoFactorUseCase
import com.hrplatform.auth.domain.account.UserAccount
import com.hrplatform.auth.domain.account.UserAccountStatus
import com.hrplatform.auth.presentation.auth.AuthUserAccountIdArgumentResolver
import com.hrplatform.auth.presentation.auth.JwtAuthenticationToken
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class MyAuthControllerTest : BehaviorSpec({

    val getMeUseCase = mockk<GetMeUseCase>()
    val changePasswordUseCase = mockk<ChangePasswordUseCase>()
    val disableTwoFactorUseCase = mockk<DisableTwoFactorUseCase>()

    val controller = MyAuthController(
        getMeUseCase = getMeUseCase,
        changePasswordUseCase = changePasswordUseCase,
        disableTwoFactorUseCase = disableTwoFactorUseCase,
    )

    val objectMapper = ObjectMapper().findAndRegisterModules()
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .setCustomArgumentResolvers(AuthUserAccountIdArgumentResolver())
        .build()

    fun setAuthentication(userAccountId: Long) {
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(
            userAccountId = userAccountId,
            employmentId = null,
        )
    }

    fun clearAuthentication() {
        SecurityContextHolder.clearContext()
    }

    afterEach { clearAuthentication() }

    given("GET /auth/me - 인증된 사용자") {
        then("200 + MeResponse 반환") {
            setAuthentication(42L)
            val account = mockk<UserAccount>()
            every { account.id } returns 42L
            every { account.employmentId } returns 100L
            every { account.companyId } returns 1L
            every { account.status } returns UserAccountStatus.ACTIVE
            every { account.twoFactorEnabled } returns false
            every { account.lastLoginAt } returns null

            every { getMeUseCase.execute(any()) } returns MeResult(userAccount = account, roles = emptyList())

            mockMvc.get("/auth/me").andExpect {
                status { isOk() }
                jsonPath("$.userAccountId") { value(42) }
                jsonPath("$.roles") { isArray() }
            }
        }
    }

    given("GET /auth/me - 인증 없음") {
        then("401 반환") {
            clearAuthentication()

            mockMvc.get("/auth/me").andExpect {
                status { isUnauthorized() }
            }
        }
    }

    given("POST /auth/me/password/change - 인증된 사용자") {
        then("204 반환") {
            setAuthentication(42L)
            every { changePasswordUseCase.execute(any()) } returns Unit

            mockMvc.post("/auth/me/password/change") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "oldPassword" to "OldPassword123!",
                        "newPassword" to "NewPassword123!",
                    ),
                )
            }.andExpect {
                status { isNoContent() }
            }
        }
    }

    given("POST /auth/me/password/change - 인증 없음") {
        then("401 반환") {
            clearAuthentication()

            mockMvc.post("/auth/me/password/change") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "oldPassword" to "OldPassword123!",
                        "newPassword" to "NewPassword123!",
                    ),
                )
            }.andExpect {
                status { isUnauthorized() }
            }
        }
    }
})
