package com.closet.settlement.presentation.settlement

import com.closet.common.response.ApiResponse
import com.closet.settlement.application.SettlementService
import com.closet.settlement.domain.settlement.SettlementStatus
import com.closet.settlement.presentation.dto.CalculateSettlementRequest
import com.closet.settlement.presentation.dto.SettlementResponse
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/settlements")
class SettlementController(
    private val settlementService: SettlementService,
) {

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.CREATED)
    fun calculate(
        @RequestBody @Valid request: CalculateSettlementRequest,
    ): ApiResponse<SettlementResponse> {
        val response = settlementService.calculate(request)
        return ApiResponse.created(response)
    }

    @GetMapping
    fun getSettlements(
        @RequestParam sellerId: Long,
        @RequestParam(required = false) status: SettlementStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ApiResponse<Page<SettlementResponse>> {
        val response = settlementService.findBySeller(sellerId, status, pageable)
        return ApiResponse.ok(response)
    }

    @GetMapping("/{id}")
    fun getSettlement(@PathVariable id: Long): ApiResponse<SettlementResponse> {
        val response = settlementService.findById(id)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/confirm")
    fun confirm(@PathVariable id: Long): ApiResponse<SettlementResponse> {
        val response = settlementService.confirm(id)
        return ApiResponse.ok(response)
    }

    @PatchMapping("/{id}/pay")
    fun pay(@PathVariable id: Long): ApiResponse<SettlementResponse> {
        val response = settlementService.pay(id)
        return ApiResponse.ok(response)
    }
}
