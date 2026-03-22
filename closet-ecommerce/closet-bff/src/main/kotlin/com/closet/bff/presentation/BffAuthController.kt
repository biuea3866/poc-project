package com.closet.bff.presentation

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.RegisterRequest
import com.closet.common.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/bff/auth")
class BffAuthController(
    private val memberClient: MemberServiceClient,
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) =
        ApiResponse.created(memberClient.register(request).block()!!)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest) =
        ApiResponse.ok(memberClient.login(request).block()!!)
}
