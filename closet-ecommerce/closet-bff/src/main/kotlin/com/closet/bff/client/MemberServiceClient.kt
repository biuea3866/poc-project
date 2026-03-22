package com.closet.bff.client

import com.closet.bff.dto.AddAddressRequest
import com.closet.bff.dto.LoginRequest
import com.closet.bff.dto.LoginResponse
import com.closet.bff.dto.MemberResponse
import com.closet.bff.dto.RegisterRequest
import com.closet.bff.dto.ShippingAddressResponse
import com.closet.bff.dto.UpdateAddressRequest
import com.closet.common.response.ApiResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "member-service", url = "\${service.member.url}")
interface MemberServiceClient {

    @GetMapping("/members/{id}")
    fun getMember(@PathVariable id: Long): ApiResponse<MemberResponse>

    @GetMapping("/members/{memberId}/addresses")
    fun getAddresses(@PathVariable memberId: Long): ApiResponse<List<ShippingAddressResponse>>

    @PostMapping("/members/register")
    fun register(@RequestBody request: RegisterRequest): ApiResponse<MemberResponse>

    @PostMapping("/members/login")
    fun login(@RequestBody request: LoginRequest): ApiResponse<LoginResponse>

    @PostMapping("/members/{memberId}/addresses")
    fun addAddress(@PathVariable memberId: Long, @RequestBody request: AddAddressRequest): ApiResponse<ShippingAddressResponse>

    @PutMapping("/members/{memberId}/addresses/{addressId}")
    fun updateAddress(@PathVariable memberId: Long, @PathVariable addressId: Long, @RequestBody request: UpdateAddressRequest): ApiResponse<ShippingAddressResponse>

    @DeleteMapping("/members/{memberId}/addresses/{addressId}")
    fun deleteAddress(@PathVariable memberId: Long, @PathVariable addressId: Long)

    @PatchMapping("/members/{memberId}/addresses/{addressId}/default")
    fun setDefaultAddress(@PathVariable memberId: Long, @PathVariable addressId: Long): ApiResponse<ShippingAddressResponse>

    @PostMapping("/members/auth/refresh")
    fun refresh(@RequestBody request: Any): ApiResponse<LoginResponse>
}
