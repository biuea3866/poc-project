package com.closet.member.presentation.point

import com.closet.common.response.ApiResponse
import com.closet.member.application.point.PointPolicyService
import com.closet.member.presentation.dto.PointPolicyResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/members/point-policies")
class PointPolicyController(
    private val pointPolicyService: PointPolicyService,
) {
    @GetMapping
    fun getActivePolicies(): ApiResponse<List<PointPolicyResponse>> {
        val response = pointPolicyService.getActivePolicies()
        return ApiResponse.ok(response)
    }
}
