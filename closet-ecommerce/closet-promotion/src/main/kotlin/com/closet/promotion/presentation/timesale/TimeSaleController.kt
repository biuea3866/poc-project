package com.closet.promotion.presentation.timesale

import com.closet.common.response.ApiResponse
import com.closet.promotion.application.TimeSaleService
import com.closet.promotion.presentation.dto.CreateTimeSaleRequest
import com.closet.promotion.presentation.dto.TimeSaleResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/promotions/time-sales")
class TimeSaleController(
    private val timeSaleService: TimeSaleService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTimeSale(
        @RequestBody @Valid request: CreateTimeSaleRequest,
    ): ApiResponse<TimeSaleResponse> {
        val response = timeSaleService.createTimeSale(request)
        return ApiResponse.created(response)
    }

    @GetMapping("/active")
    fun getActiveTimeSales(): ApiResponse<List<TimeSaleResponse>> {
        val response = timeSaleService.getActiveTimeSales()
        return ApiResponse.ok(response)
    }

    @PostMapping("/{id}/purchase")
    fun purchaseTimeSale(
        @PathVariable id: Long,
    ): ApiResponse<TimeSaleResponse> {
        val response = timeSaleService.purchaseTimeSale(id)
        return ApiResponse.ok(response)
    }
}
