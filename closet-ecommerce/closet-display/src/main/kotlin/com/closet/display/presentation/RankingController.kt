package com.closet.display.presentation

import com.closet.common.response.ApiResponse
import com.closet.display.application.dto.RankingResponse
import com.closet.display.application.service.RankingService
import com.closet.display.domain.enums.PeriodType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/display/rankings")
class RankingController(
    private val rankingService: RankingService
) {

    @GetMapping
    fun getRanking(
        @RequestParam categoryId: Long,
        @RequestParam(defaultValue = "DAILY") period: PeriodType,
        @RequestParam(defaultValue = "10") limit: Int
    ): ApiResponse<List<RankingResponse>> {
        return ApiResponse.ok(rankingService.getRanking(categoryId, period, limit))
    }
}
