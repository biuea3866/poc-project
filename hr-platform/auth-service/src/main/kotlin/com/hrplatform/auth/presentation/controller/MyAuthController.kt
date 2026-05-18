package com.hrplatform.auth.presentation.controller

import com.hrplatform.auth.application.auth.ChangePasswordCommand
import com.hrplatform.auth.application.auth.ChangePasswordUseCase
import com.hrplatform.auth.application.auth.GetMeCommand
import com.hrplatform.auth.application.auth.GetMeUseCase
import com.hrplatform.auth.application.twofactor.DisableTwoFactorCommand
import com.hrplatform.auth.application.twofactor.DisableTwoFactorUseCase
import com.hrplatform.auth.presentation.auth.AuthUserAccountId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/me")
class MyAuthController(
    private val getMeUseCase: GetMeUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val disableTwoFactorUseCase: DisableTwoFactorUseCase,
) {

    @GetMapping
    fun me(@AuthUserAccountId userAccountId: Long): MeResponse {
        val result = getMeUseCase.execute(GetMeCommand(userAccountId = userAccountId))
        return MeResponse.of(result)
    }

    @PostMapping("/password/change")
    fun changePassword(
        @AuthUserAccountId userAccountId: Long,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Void> {
        changePasswordUseCase.execute(
            ChangePasswordCommand(
                userAccountId = userAccountId,
                oldRawPassword = request.oldPassword,
                newRawPassword = request.newPassword,
                actorEmploymentId = null,
            ),
        )
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/2fa/disable")
    fun disable2fa(@AuthUserAccountId userAccountId: Long): ResponseEntity<Void> {
        disableTwoFactorUseCase.execute(DisableTwoFactorCommand(userAccountId = userAccountId, actorEmploymentId = null))
        return ResponseEntity.noContent().build()
    }
}
