package com.biuea.wiki.presentation.user

import com.biuea.wiki.application.DeleteUserInput
import com.biuea.wiki.application.LoginUserInput
import com.biuea.wiki.application.SignUpUserInput
import com.biuea.wiki.application.UserAuthFacade
import com.biuea.wiki.infrastructure.security.AuthenticatedUser
import com.biuea.wiki.infrastructure.security.JwtTokenBlacklist
import com.biuea.wiki.infrastructure.security.JwtTokenProvider
import com.biuea.wiki.presentation.user.request.LoginRequest
import com.biuea.wiki.presentation.user.request.SignUpRequest
import com.biuea.wiki.presentation.user.response.LoginResponse
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
@RequestMapping("/api/users")
class UserApiController(
    private val userAuthFacade: UserAuthFacade,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtTokenBlacklist: JwtTokenBlacklist,
) {

    @PostMapping("/signup")
    fun signUp(@RequestBody @Valid request: SignUpRequest): ResponseEntity<UserResponse> {
        val output = userAuthFacade.signUp(
            SignUpUserInput(
                email = request.email,
                password = request.password,
                name = request.name,
            )
        )

        return ResponseEntity.ok(
            UserResponse(
                id = output.id,
                email = output.email,
                name = output.name,
            )
        )
    }

    @PostMapping("/login")
    fun login(@RequestBody @Valid request: LoginRequest): ResponseEntity<LoginResponse> {
        val output = userAuthFacade.login(
            LoginUserInput(
                email = request.email,
                password = request.password,
            )
        )

        val authenticatedUser = AuthenticatedUser(
            id = output.id,
            email = output.email,
            name = output.name,
        )
        val accessToken = jwtTokenProvider.createAccessToken(authenticatedUser)

        return ResponseEntity.ok(
            LoginResponse(
                accessToken = accessToken,
                tokenType = "Bearer",
                user = UserResponse(
                    id = output.id,
                    email = output.email,
                    name = output.name,
                ),
            )
        )
    }

    @PostMapping("/logout")
    fun logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorizationHeader: String?,
    ): ResponseEntity<Void> {
        val token = jwtTokenProvider.resolveToken(authorizationHeader)
        if (token != null) {
            val expiresAt = jwtTokenProvider.getExpirationTimeMillis(token)
            if (expiresAt != null) {
                jwtTokenBlacklist.blacklist(token, expiresAt)
            }
        }

        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/me")
    fun deleteMe(authentication: Authentication): ResponseEntity<Void> {
        val principal = authentication.principal as AuthenticatedUser
        userAuthFacade.delete(DeleteUserInput(userId = principal.id))

        return ResponseEntity.noContent().build()
    }
}
