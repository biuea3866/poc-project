package com.closet.promotion.presentation.timesale

import com.closet.common.response.ApiResponse
import com.closet.promotion.application.TimeSaleFacade
import com.closet.promotion.presentation.dto.PurchaseTimeSaleRequest
import com.closet.promotion.presentation.dto.TimeSaleOrderResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/promotions/time-sales")
class TimeSaleOrderController(
    private val timeSaleFacade: TimeSaleFacade,
) {
    @PostMapping("/{id}/purchase-order")
    fun purchaseWithOrder(
        @PathVariable id: Long,
        @RequestBody @Valid request: PurchaseTimeSaleRequest,
    ): ApiResponse<TimeSaleOrderResponse> {
        val response = timeSaleFacade.purchaseWithOrder(id, request)
        return ApiResponse.ok(response)
    }

    @GetMapping("/{id}/orders")
    fun getOrdersByTimeSale(
        @PathVariable id: Long,
    ): ApiResponse<List<TimeSaleOrderResponse>> {
        val response = timeSaleFacade.getOrdersByTimeSale(id)
        return ApiResponse.ok(response)
    }

    @GetMapping("/orders/my")
    fun getMyOrders(
        @RequestParam memberId: Long,
    ): ApiResponse<List<TimeSaleOrderResponse>> {
        val response = timeSaleFacade.getOrdersByMember(memberId)
        return ApiResponse.ok(response)
    }
}
