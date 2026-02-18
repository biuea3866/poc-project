package com.biuea.wiki.presentation.user

import com.biuea.wiki.application.DeleteUserInput
import com.biuea.wiki.application.LoginUserInput
import com.biuea.wiki.application.LogoutUserInput
import com.biuea.wiki.application.RefreshUserInput
import com.biuea.wiki.application.SignUpUserInput
import com.biuea.wiki.application.UserAuthFacade
import com.biuea.wiki.domain.auth.AuthenticatedUser
import com.biuea.wiki.presentation.user.request.LoginRequest
import com.biuea.wiki.presentation.user.request.LogoutRequest
import com.biuea.wiki.presentation.user.request.RefreshRequest
import com.biuea.wiki.presentation.user.request.SignUpRequest
import com.biuea.wiki.presentation.user.response.LoginResponse
import com.biuea.wiki.presentation.user.response.RefreshResponse
import com.biuea.wiki.presentation.user.response.UserResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class UserApiController(
    private val userAuthFacade: UserAuthFacade,
) {
    @PostMapping("/signup")
    fun signUp(@RequestBody @Valid request: SignUpRequest): ResponseEntity<UserResponse> {
        val output = userAuthFacade.signUp(SignUpUserInput(request.email, request.password, request.name))
        return ResponseEntity.ok(UserResponse(output.id, output.email, output.name))
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<LoginResponse> {
        val output = userAuthFacade.login(LoginUserInput(request.email, request.password))
        return ResponseEntity.ok(
            LoginResponse(
                accessToken = output.accessToken,
                refreshToken = output.refreshToken,
                tokenType = output.tokenType,
                user = UserResponse(output.user.id, output.user.email, output.user.name),
            )
        )
    }

    @PostMapping("/refresh")
    fun refresh(@RequestBody @Valid request: RefreshRequest): ResponseEntity<RefreshResponse> {
        val output = userAuthFacade.refresh(RefreshUserInput(request.refreshToken))
        return ResponseEntity.ok(RefreshResponse(output.accessToken, output.tokenType))
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorizationHeader: String?,
        @RequestBody(required = false) request: LogoutRequest?,
    ): ResponseEntity<Void> {
        userAuthFacade.logout(LogoutUserInput(authorizationHeader, request?.refreshToken))
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/me")
    fun deleteMe(authentication: Authentication): ResponseEntity<Void> {
        val principal = authentication.principal as AuthenticatedUser
        userAuthFacade.delete(DeleteUserInput(principal.id))
        return ResponseEntity.noContent().build()
    }
}
