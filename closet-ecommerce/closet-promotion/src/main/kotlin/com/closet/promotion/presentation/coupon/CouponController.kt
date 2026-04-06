package com.closet.promotion.presentation.coupon

import com.closet.common.response.ApiResponse
import com.closet.promotion.application.CouponService
import com.closet.promotion.presentation.dto.CouponResponse
import com.closet.promotion.presentation.dto.CouponValidationResponse
import com.closet.promotion.presentation.dto.CreateCouponRequest
import com.closet.promotion.presentation.dto.IssueCouponRequest
import com.closet.promotion.presentation.dto.MemberCouponResponse
import com.closet.promotion.presentation.dto.UseCouponRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/promotions/coupons")
class CouponController(
    private val couponService: CouponService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCoupon(
        @RequestBody @Valid request: CreateCouponRequest,
    ): ApiResponse<CouponResponse> {
        val response = couponService.createCoupon(request)
        return ApiResponse.created(response)
    }

    @PostMapping("/{id}/issue")
    fun issueCoupon(
        @PathVariable id: Long,
        @RequestBody @Valid request: IssueCouponRequest,
    ): ApiResponse<MemberCouponResponse> {
        val response = couponService.issueCoupon(id, request.memberId)
        return ApiResponse.ok(response)
    }

    @PostMapping("/{id}/use")
    fun useCoupon(
        @PathVariable id: Long,
        @RequestBody @Valid request: UseCouponRequest,
    ): ApiResponse<MemberCouponResponse> {
        val response = couponService.useCoupon(id, request.orderId, request.memberId)
        return ApiResponse.ok(response)
    }

    @GetMapping("/my")
    fun getMyCoupons(
        @RequestParam memberId: Long,
    ): ApiResponse<List<MemberCouponResponse>> {
        val response = couponService.getMyCoupons(memberId)
        return ApiResponse.ok(response)
    }

    @GetMapping("/{id}/validate")
    fun validateCoupon(
        @PathVariable id: Long,
        @RequestParam orderAmount: BigDecimal,
    ): ApiResponse<CouponValidationResponse> {
        val response = couponService.validateCoupon(id, orderAmount)
        return ApiResponse.ok(response)
    }
}
