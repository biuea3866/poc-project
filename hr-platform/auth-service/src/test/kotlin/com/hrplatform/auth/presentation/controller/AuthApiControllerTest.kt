package com.hrplatform.auth.presentation.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.hrplatform.auth.application.auth.ConfirmPasswordResetUseCase
import com.hrplatform.auth.application.auth.LoginUseCase
import com.hrplatform.auth.application.auth.LogoutUseCase
import com.hrplatform.auth.application.auth.RefreshUseCase
import com.hrplatform.auth.application.auth.RequestPasswordResetUseCase
import com.hrplatform.auth.application.twofactor.EnrollTwoFactorUseCase
import com.hrplatform.auth.application.twofactor.VerifyTwoFactorUseCase
import com.hrplatform.auth.domain.auth.service.LoginResult
import com.hrplatform.auth.domain.auth.service.TokenPair
import com.hrplatform.core.exception.UnauthorizedException
import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.ZoneOffset
import java.time.ZonedDateTime

class AuthApiControllerTest : BehaviorSpec({

    val loginUseCase = mockk<LoginUseCase>()
    val refreshUseCase = mockk<RefreshUseCase>()
    val logoutUseCase = mockk<LogoutUseCase>()
    val requestPasswordResetUseCase = mockk<RequestPasswordResetUseCase>()
    val confirmPasswordResetUseCase = mockk<ConfirmPasswordResetUseCase>()
    val enrollTwoFactorUseCase = mockk<EnrollTwoFactorUseCase>()
    val verifyTwoFactorUseCase = mockk<VerifyTwoFactorUseCase>()

    val controller = AuthApiController(
        loginUseCase = loginUseCase,
        refreshUseCase = refreshUseCase,
        logoutUseCase = logoutUseCase,
        requestPasswordResetUseCase = requestPasswordResetUseCase,
        confirmPasswordResetUseCase = confirmPasswordResetUseCase,
        enrollTwoFactorUseCase = enrollTwoFactorUseCase,
        verifyTwoFactorUseCase = verifyTwoFactorUseCase,
    )

    val objectMapper = ObjectMapper().findAndRegisterModules()
    val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    val now = ZonedDateTime.now(ZoneOffset.UTC)

    given("POST /auth/login - 올바른 자격증명") {
        then("200 + LoginResponse 반환") {
            every { loginUseCase.execute(any()) } returns LoginResult(
                userAccountId = 1L,
                accessToken = "access-token",
                refreshToken = "refresh-token",
                expiresAt = now.plusDays(14),
                requiresTwoFactor = false,
            )

            mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "email" to "user@example.com",
                        "password" to "Password1234!",
                    ),
                )
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { value("access-token") }
                jsonPath("$.requiresTwoFactor") { value(false) }
            }
        }
    }

    given("POST /auth/login - 잘못된 password") {
        then("401 반환") {
            every { loginUseCase.execute(any()) } throws UnauthorizedException(
                errorCode = "UNAUTHORIZED",
                message = "이메일 또는 비밀번호가 올바르지 않습니다",
            )

            mockMvc.post("/auth/login") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(
                    mapOf(
                        "email" to "user@example.com",
                        "password" to "wrongPassword1!",
                    ),
                )
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("UNAUTHORIZED") }
            }
        }
    }

    given("POST /auth/refresh - 유효한 refresh token") {
        then("200 + TokenPairResponse 반환") {
            every { refreshUseCase.execute(any()) } returns TokenPair(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                refreshTokenHash = "hash",
                refreshTokenExpiresAt = now.plusDays(14),
                jti = "jti-123",
            )

            mockMvc.post("/auth/refresh") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("refreshToken" to "old-refresh-token"))
            }.andExpect {
                status { isOk() }
                jsonPath("$.accessToken") { value("new-access-token") }
            }
        }
    }

    given("POST /auth/logout - 유효한 refresh token") {
        then("204 반환") {
            every { logoutUseCase.execute(any()) } returns Unit

            mockMvc.post("/auth/logout") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("refreshToken" to "refresh-token"))
            }.andExpect {
                status { isNoContent() }
            }

            verify { logoutUseCase.execute(any()) }
        }
    }

    given("POST /auth/password-reset/request") {
        then("204 반환 (토큰 log stub만 수행)") {
            every { requestPasswordResetUseCase.execute(any()) } returns "raw-token-stub"

            mockMvc.post("/auth/password-reset/request") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(mapOf("email" to "user@example.com"))
            }.andExpect {
                status { isNoContent() }
            }
        }
    }
})
