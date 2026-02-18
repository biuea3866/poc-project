package com.biuea.wiki.presentation.user

import com.biuea.wiki.application.DeleteUserInput
import com.biuea.wiki.application.LoginUserInput
import com.biuea.wiki.application.SignUpUserInput
import com.biuea.wiki.application.UserAuthFacade
import com.biuea.wiki.infrastructure.security.AuthenticatedUser
import com.biuea.wiki.presentation.user.request.LoginRequest
import com.biuea.wiki.presentation.user.request.SignUpRequest
import com.biuea.wiki.presentation.user.response.UserResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserApiController(
    private val userAuthFacade: UserAuthFacade,
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
    fun login(
        @RequestBody @Valid request: LoginRequest,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<UserResponse> {
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

        val authentication = UsernamePasswordAuthenticationToken(
            authenticatedUser,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        )

        val securityContext = SecurityContextHolder.createEmptyContext()
        securityContext.authentication = authentication
        SecurityContextHolder.setContext(securityContext)
        httpServletRequest.getSession(true)

        return ResponseEntity.ok(
            UserResponse(
                id = output.id,
                email = output.email,
                name = output.name,
            )
        )
    }

    @PostMapping("/logout")
    fun logout(
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        SecurityContextLogoutHandler().logout(request, response, authentication)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/me")
    fun deleteMe(
        authentication: Authentication,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val principal = authentication.principal as AuthenticatedUser

        userAuthFacade.delete(DeleteUserInput(userId = principal.id))
        SecurityContextLogoutHandler().logout(request, response, authentication)

        return ResponseEntity.noContent().build()
    }
}
