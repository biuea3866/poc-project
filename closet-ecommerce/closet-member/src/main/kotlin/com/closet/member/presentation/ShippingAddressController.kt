package com.closet.member.presentation

import com.closet.common.response.ApiResponse
import com.closet.member.application.ShippingAddressService
import com.closet.member.config.JwtAuthenticationFilter
import com.closet.member.presentation.dto.ShippingAddressRequest
import com.closet.member.presentation.dto.ShippingAddressResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members/me/addresses")
class ShippingAddressController(
    private val shippingAddressService: ShippingAddressService,
) {
    /** 배송지 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        request: HttpServletRequest,
        @Valid @RequestBody body: ShippingAddressRequest,
    ): ApiResponse<ShippingAddressResponse> {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        return ApiResponse.created(shippingAddressService.create(memberId, body))
    }

    /** 배송지 목록 조회 */
    @GetMapping
    fun findAll(request: HttpServletRequest): ApiResponse<List<ShippingAddressResponse>> {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        return ApiResponse.ok(shippingAddressService.findAll(memberId))
    }

    /** 배송지 수정 */
    @PutMapping("/{id}")
    fun update(
        request: HttpServletRequest,
        @PathVariable id: Long,
        @Valid @RequestBody body: ShippingAddressRequest,
    ): ApiResponse<ShippingAddressResponse> {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        return ApiResponse.ok(shippingAddressService.update(memberId, id, body))
    }

    /** 배송지 삭제 */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(request: HttpServletRequest, @PathVariable id: Long) {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        shippingAddressService.delete(memberId, id)
    }

    /** 기본 배송지 설정 */
    @PatchMapping("/{id}/default")
    fun setDefault(
        request: HttpServletRequest,
        @PathVariable id: Long,
    ): ApiResponse<ShippingAddressResponse> {
        val memberId = request.getAttribute(JwtAuthenticationFilter.MEMBER_ID_ATTRIBUTE) as Long
        return ApiResponse.ok(shippingAddressService.setDefault(memberId, id))
    }
}
