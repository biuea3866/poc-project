package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.application.auth.ConfirmPasswordResetCommand
import com.hrplatform.auth.application.auth.ConfirmPasswordResetUseCase
import com.hrplatform.auth.application.auth.LoginCommand
import com.hrplatform.auth.application.auth.LoginUseCase
import com.hrplatform.auth.application.auth.LogoutCommand
import com.hrplatform.auth.application.auth.LogoutUseCase
import com.hrplatform.auth.application.auth.RefreshCommand
import com.hrplatform.auth.application.auth.RefreshUseCase
import com.hrplatform.auth.application.auth.RequestPasswordResetCommand
import com.hrplatform.auth.application.auth.RequestPasswordResetUseCase
import com.hrplatform.auth.application.twofactor.EnrollTwoFactorCommand
import com.hrplatform.auth.application.twofactor.EnrollTwoFactorUseCase
import com.hrplatform.auth.application.twofactor.VerifyTwoFactorCommand
import com.hrplatform.auth.application.twofactor.VerifyTwoFactorUseCase
import com.hrplatform.auth.presentation.auth.AuthUserAccountId
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthApiController(
    private val loginUseCase: LoginUseCase,
    private val refreshUseCase: RefreshUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val requestPasswordResetUseCase: RequestPasswordResetUseCase,
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase,
    private val enrollTwoFactorUseCase: EnrollTwoFactorUseCase,
    private val verifyTwoFactorUseCase: VerifyTwoFactorUseCase,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<LoginResponse> {
        val result = loginUseCase.execute(
            LoginCommand(
                email = request.email,
                rawPassword = request.password,
                deviceInfo = request.deviceInfo,
                ipAddress = request.ipAddress,
                userAgent = request.userAgent,
            ),
        )
        return ResponseEntity.ok(LoginResponse.of(result))
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): TokenPairResponse {
        val pair = refreshUseCase.execute(RefreshCommand(rawRefreshToken = request.refreshToken))
        return TokenPairResponse.of(pair)
    }

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: LogoutRequest): ResponseEntity<Void> {
        logoutUseCase.execute(LogoutCommand(rawRefreshToken = request.refreshToken))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password-reset/request")
    fun requestReset(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<Void> {
        val rawToken = requestPasswordResetUseCase.execute(RequestPasswordResetCommand(email = request.email))
        log.info("[STUB] 비밀번호 재설정 토큰 발급 (notification-service 미구현): email={}, token={}...", request.email, rawToken.take(8))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/password-reset/confirm")
    fun confirmReset(@Valid @RequestBody request: PasswordResetConfirmRequest): ResponseEntity<Void> {
        confirmPasswordResetUseCase.execute(
            ConfirmPasswordResetCommand(token = request.token, newPassword = request.newPassword),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/2fa/enroll")
    fun enroll2fa(@AuthUserAccountId userAccountId: Long): TwoFactorEnrollResponse {
        val result = enrollTwoFactorUseCase.execute(EnrollTwoFactorCommand(userAccountId = userAccountId, actorEmploymentId = null))
        return TwoFactorEnrollResponse.of(result)
    }

    @PostMapping("/2fa/verify")
    fun verify2fa(
        @AuthUserAccountId userAccountId: Long,
        @Valid @RequestBody request: TwoFactorVerifyRequest,
    ): TwoFactorVerifyResponse {
        val verified = verifyTwoFactorUseCase.execute(VerifyTwoFactorCommand(userAccountId = userAccountId, otp = request.otp))
        return TwoFactorVerifyResponse(verified = verified)
    }
}
