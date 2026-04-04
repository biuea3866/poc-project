package com.closet.bff.presentation

import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.RegisterRequest
import com.closet.bff.facade.AuthBffFacade
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff/auth")
class BffAuthController(
    private val authFacade: AuthBffFacade,
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) =
        ApiResponse.created(authFacade.register(request))

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest) =
        ApiResponse.ok(authFacade.login(request))
}
