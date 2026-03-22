package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.point.GradeType
import com.closet.promotion.domain.point.PointEventType
import com.closet.promotion.domain.point.PointPolicy
import java.math.BigDecimal

data class PointPolicyResponse(
    val id: Long,
    val eventType: PointEventType,
    val gradeType: GradeType,
    val pointAmount: Int?,
    val pointRate: BigDecimal?,
    val description: String,
    val isActive: Boolean,
) {
    companion object {
        fun from(policy: PointPolicy): PointPolicyResponse = PointPolicyResponse(
            id = policy.id,
            eventType = policy.eventType,
            gradeType = policy.gradeType,
            pointAmount = policy.pointAmount,
            pointRate = policy.pointRate,
            description = policy.description,
            isActive = policy.isActive,
        )
    }
}

data class CalculatePointResponse(
    val totalPoint: Int,
    val details: List<PointDetail>,
)

data class PointDetail(
    val policyId: Long,
    val eventType: PointEventType,
    val description: String,
    val earnedPoint: Int,
)
