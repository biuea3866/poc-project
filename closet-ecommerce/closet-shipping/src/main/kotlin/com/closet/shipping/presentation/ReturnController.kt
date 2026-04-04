package com.closet.shipping.presentation

import com.closet.common.auth.MemberRole
import com.closet.common.auth.RoleRequired
import com.closet.common.response.ApiResponse
import com.closet.shipping.application.CreateReturnRequest
import com.closet.shipping.application.RejectReturnRequest
import com.closet.shipping.application.ReturnRequestResponse
import com.closet.shipping.application.ReturnService
import com.closet.shipping.application.SchedulePickupRequest
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

@RestController
@RequestMapping("/api/v1/returns")
class ReturnController(
    private val returnService: ReturnService,
) {

    /**
     * 반품 신청 (BUYER).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RoleRequired(MemberRole.BUYER)
    fun createReturnRequest(
        @RequestHeader("X-Member-Id") memberId: Long,
        @RequestHeader("X-Seller-Id", required = false, defaultValue = "0") sellerId: Long,
        @RequestBody @Valid request: CreateReturnRequest,
    ): ApiResponse<ReturnRequestResponse> {
        val response = returnService.createReturnRequest(memberId, sellerId, request)
        return ApiResponse.created(response)
    }

    /**
     * 반품 상세 조회.
     */
    @GetMapping("/{id}")
    fun getReturnRequest(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnService.findById(id)
        return ApiResponse.ok(response)
    }

    /**
     * 주문별 반품 조회.
     */
    @GetMapping
    fun getReturnRequestsByOrderId(@RequestParam orderId: Long): ApiResponse<List<ReturnRequestResponse>> {
        val response = returnService.findByOrderId(orderId)
        return ApiResponse.ok(response)
    }

    /**
     * 수거 예약 (SELLER).
     */
    @PatchMapping("/{id}/pickup-schedule")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun schedulePickup(
        @PathVariable id: Long,
        @RequestBody request: SchedulePickupRequest,
    ): ApiResponse<ReturnRequestResponse> {
        val response = returnService.schedulePickup(id, request)
        return ApiResponse.ok(response)
    }

    /**
     * 수거 완료 (SELLER).
     */
    @PatchMapping("/{id}/pickup-complete")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun completePickup(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnService.completePickup(id)
        return ApiResponse.ok(response)
    }

    /**
     * 검수 시작 (SELLER).
     */
    @PatchMapping("/{id}/inspect")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun startInspection(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnService.startInspection(id)
        return ApiResponse.ok(response)
    }

    /**
     * 반품 승인 (SELLER).
     */
    @PatchMapping("/{id}/approve")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun approve(@PathVariable id: Long): ApiResponse<ReturnRequestResponse> {
        val response = returnService.approve(id)
        return ApiResponse.ok(response)
    }

    /**
     * 반품 거절 (SELLER).
     */
    @PatchMapping("/{id}/reject")
    @RoleRequired(MemberRole.SELLER, MemberRole.ADMIN)
    fun reject(
        @PathVariable id: Long,
        @RequestBody request: RejectReturnRequest,
    ): ApiResponse<ReturnRequestResponse> {
        val response = returnService.reject(id, request)
        return ApiResponse.ok(response)
    }
}
