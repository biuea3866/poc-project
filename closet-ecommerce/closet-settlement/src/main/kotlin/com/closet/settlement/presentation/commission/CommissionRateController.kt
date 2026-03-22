package com.closet.settlement.presentation.commission

import com.closet.common.response.ApiResponse
import com.closet.settlement.application.CommissionRateService
import com.closet.settlement.presentation.dto.CommissionRateResponse
import com.closet.settlement.presentation.dto.UpdateCommissionRateRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/commission-rates")
class CommissionRateController(
    private val commissionRateService: CommissionRateService,
) {

    @GetMapping
    fun getCommissionRates(): ApiResponse<List<CommissionRateResponse>> {
        val response = commissionRateService.getAllRates()
        return ApiResponse.ok(response)
    }

    @PutMapping("/{categoryId}")
    fun setCommissionRate(
        @PathVariable categoryId: Long,
        @RequestBody @Valid request: UpdateCommissionRateRequest,
    ): ApiResponse<CommissionRateResponse> {
        val response = commissionRateService.setRate(categoryId, request.rate, request.effectiveFrom)
        return ApiResponse.ok(response)
    }
}
