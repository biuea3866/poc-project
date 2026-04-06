package com.closet.shipping.presentation

import com.closet.common.response.ApiResponse
import com.closet.shipping.application.cs.ApproveClaimCommand
import com.closet.shipping.application.cs.ClaimResponse
import com.closet.shipping.application.cs.ClaimService
import com.closet.shipping.application.cs.CreateClaimCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/claims")
class ClaimController(
    private val claimService: ClaimService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createClaim(
        @RequestBody @Valid command: CreateClaimCommand,
    ): ApiResponse<ClaimResponse> {
        val response = claimService.createClaim(command)
        return ApiResponse.created(response)
    }

    @GetMapping("/{id}")
    fun getClaim(
        @PathVariable id: Long,
    ): ApiResponse<ClaimResponse> {
        val response = claimService.getClaim(id)
        return ApiResponse.ok(response)
    }

    @GetMapping("/my")
    fun getMyClaims(
        @RequestParam memberId: Long,
    ): ApiResponse<List<ClaimResponse>> {
        val response = claimService.getClaimsByMember(memberId)
        return ApiResponse.ok(response)
    }

    @PutMapping("/{id}/approve")
    fun approveClaim(
        @PathVariable id: Long,
        @RequestBody @Valid command: ApproveClaimCommand,
    ): ApiResponse<ClaimResponse> {
        val response = claimService.approveClaim(id, command.refundAmount)
        return ApiResponse.ok(response)
    }

    @PutMapping("/{id}/reject")
    fun rejectClaim(
        @PathVariable id: Long,
    ): ApiResponse<ClaimResponse> {
        val response = claimService.rejectClaim(id)
        return ApiResponse.ok(response)
    }

    @PutMapping("/{id}/complete")
    fun completeClaim(
        @PathVariable id: Long,
    ): ApiResponse<ClaimResponse> {
        val response = claimService.completeClaim(id)
        return ApiResponse.ok(response)
    }
}
