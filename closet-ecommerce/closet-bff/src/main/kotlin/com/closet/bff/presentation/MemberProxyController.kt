package com.closet.bff.presentation

import com.closet.bff.client.MemberServiceClient
import com.closet.bff.dto.AddAddressRequest
import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.RegisterRequest
import com.closet.bff.dto.UpdateAddressRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members")
class MemberProxyController(
    private val memberClient: MemberServiceClient,
) {

    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest) =
        memberClient.register(request)

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest) =
        memberClient.login(request)

    @GetMapping("/me")
    fun getMe(@RequestHeader("X-Member-Id") memberId: Long) =
        memberClient.getMember(memberId)

    @PostMapping("/me/addresses")
    fun addAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestBody request: AddAddressRequest,
    ) = memberClient.addAddress(memberId, request)

    @GetMapping("/me/addresses")
    fun getAddresses(@RequestHeader("X-Member-Id") memberId: Long) =
        memberClient.getAddresses(memberId)

    @PutMapping("/me/addresses/{id}")
    fun updateAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
        @RequestBody request: UpdateAddressRequest,
    ) = memberClient.updateAddress(memberId, id, request)

    @DeleteMapping("/me/addresses/{id}")
    fun deleteAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ): ResponseEntity<Void> {
        memberClient.deleteAddress(memberId, id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/me/addresses/{id}/default")
    fun setDefaultAddress(
        @RequestHeader("X-Member-Id") memberId: Long,
        @PathVariable id: Long,
    ) = memberClient.setDefaultAddress(memberId, id)

    @PostMapping("/auth/refresh")
    fun refresh(@RequestBody request: Any) =
        memberClient.refresh(request)
}
