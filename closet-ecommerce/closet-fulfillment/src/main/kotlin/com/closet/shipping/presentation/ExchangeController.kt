package com.closet.shipping.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.shipping.application.CreateExchangeRequest
import com.closet.shipping.application.ExchangeRequestResponse
import com.closet.shipping.application.ExchangeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 교환 API 컨트롤러 (CP-28).
 *
 * POST   /api/v1/exchanges                    - 교환 신청 (BUYER)
 * GET    /api/v1/exchanges/{id}               - 교환 상세
 * GET    /api/v1/exchanges?orderId=           - 주문별 교환 목록
 * PATCH  /api/v1/exchanges/{id}/pickup-schedule - 수거 예약 (SELLER)
 * PATCH  /api/v1/exchanges/{id}/pickup-complete - 수거 완료 (SELLER)
 * PATCH  /api/v1/exchanges/{id}/reship          - 재배송 시작 (SELLER)
 * PATCH  /api/v1/exchanges/{id}/complete        - 교환 완료 (SELLER)
 * PATCH  /api/v1/exchanges/{id}/reject          - 교환 거절 (SELLER)
 */
@RestController
@RequestMapping("/api/v1/exchanges")
class ExchangeController(
    private val exchangeService: ExchangeService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.BUYER)
    fun createExchangeRequest(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestHeader("X-Seller-Id", required = false, defaultValue = "0") sellerId: Long,
        @RequestBody @Valid request: CreateExchangeRequest,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.createExchangeRequest(memberId, sellerId, request)
        return ApiResponse.created(response)
    }

    @GetMapping("/{id}")
    fun getExchangeRequest(
        @PathVariable id: Long,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.findById(id)
        return ApiResponse.ok(response)
    }

    @GetMapping
    fun getExchangesByOrderId(
        @RequestParam orderId: Long,
    ): ApiResponse<List<ExchangeRequestResponse>> {
        val response = exchangeService.findByOrderId(orderId)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/pickup-schedule")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun schedulePickup(
        @PathVariable id: Long,
        @RequestBody(required = false) request: PickupScheduleRequest?,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.schedulePickup(id, request?.pickupTrackingNumber)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/pickup-complete")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun completePickup(
        @PathVariable id: Long,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.completePickup(id)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/reship")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun startReshipping(
        @PathVariable id: Long,
        @RequestBody(required = false) request: ReshipRequest?,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.startReshipping(id, request?.newTrackingNumber)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/complete")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun complete(
        @PathVariable id: Long,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.complete(id)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/reject")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun reject(
        @PathVariable id: Long,
    ): ApiResponse<ExchangeRequestResponse> {
        val response = exchangeService.reject(id)
        return ApiResponse.ok(response)
    }

    data class PickupScheduleRequest(val pickupTrackingNumber: String? = null)

    data class ReshipRequest(val newTrackingNumber: String? = null)
}
