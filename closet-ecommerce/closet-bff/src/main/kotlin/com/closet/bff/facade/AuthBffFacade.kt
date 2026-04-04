package com.closet.bff.facade

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.LoginResponse
import com.closet.bff.dto.MemberResponse
import com.closet.bff.dto.RegisterRequest
import org.springframework.stereotype.Service

@Service
class AuthBffFacade(
    private val memberClient: MemberServiceClient,
) {
    fun register(request: RegisterRequest): MemberResponse {
        return memberClient.register(request).data!!
    }

    fun login(request: LoginRequest): LoginResponse {
        return memberClient.login(request).data!!
    }
}
