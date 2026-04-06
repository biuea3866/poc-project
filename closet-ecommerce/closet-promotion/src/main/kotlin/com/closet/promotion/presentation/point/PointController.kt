package com.closet.promotion.presentation.point

import com.closet.common.response.ApiResponse
import com.closet.promotion.application.PointService
import com.closet.promotion.presentation.dto.CancelPointRequest
import com.closet.promotion.presentation.dto.EarnPointRequest
import com.closet.promotion.presentation.dto.PointBalanceResponse
import com.closet.promotion.presentation.dto.PointHistoryResponse
import com.closet.promotion.presentation.dto.UsePointRequest
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/promotions/points")
class PointController(
    private val pointService: PointService,
) {
    @GetMapping("/balance")
    fun getBalance(
        @RequestParam memberId: Long,
    ): ApiResponse<PointBalanceResponse> {
        val response = pointService.getBalance(memberId)
        return ApiResponse.ok(response)
    }

    @GetMapping("/history")
    fun getHistory(
        @RequestParam memberId: Long,
    ): ApiResponse<List<PointHistoryResponse>> {
        val response = pointService.getHistory(memberId)
        return ApiResponse.ok(response)
    }

    @PostMapping("/earn")
    fun earn(
        @RequestBody @Valid request: EarnPointRequest,
    ): ApiResponse<PointHistoryResponse> {
        val response = pointService.earn(request)
        return ApiResponse.ok(response)
    }

    @PostMapping("/use")
    fun use(
        @RequestBody @Valid request: UsePointRequest,
    ): ApiResponse<PointHistoryResponse> {
        val response = pointService.use(request)
        return ApiResponse.ok(response)
    }

    @PostMapping("/cancel")
    fun cancel(
        @RequestBody @Valid request: CancelPointRequest,
    ): ApiResponse<PointHistoryResponse> {
        val response = pointService.cancel(request)
        return ApiResponse.ok(response)
    }
}
