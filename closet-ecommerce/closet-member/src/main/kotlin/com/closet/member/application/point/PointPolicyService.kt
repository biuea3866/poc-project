package com.closet.member.application.point

import com.closet.member.domain.MemberGrade
import com.closet.member.domain.point.PointEventType
import com.closet.member.domain.repository.PointPolicyRepository
import com.closet.member.presentation.dto.CalculatePointResponse
import com.closet.member.presentation.dto.PointDetail
import com.closet.member.presentation.dto.PointPolicyResponse
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PointPolicyService(
    private val pointPolicyRepository: PointPolicyRepository,
) {
    fun getActivePolicies(): List<PointPolicyResponse> {
        return pointPolicyRepository.findByIsActiveTrue()
            .map { PointPolicyResponse.from(it) }
    }

    fun calculateEarnPoint(
        orderAmount: BigDecimal,
        grade: MemberGrade,
        eventType: PointEventType = PointEventType.PURCHASE,
    ): CalculatePointResponse {
        val policies = pointPolicyRepository.findActiveByEventType(eventType)

        val details =
            policies.mapNotNull { policy ->
                val point = policy.calculatePoint(orderAmount, grade)
                if (point > 0) {
                    PointDetail(
                        policyId = policy.id,
                        eventType = policy.eventType,
                        description = policy.description,
                        earnedPoint = point,
                    )
                } else {
                    null
                }
            }

        val totalPoint = details.sumOf { it.earnedPoint }

        logger.info { "적립금 계산 완료: orderAmount=$orderAmount, grade=$grade, totalPoint=$totalPoint" }
        return CalculatePointResponse(
            totalPoint = totalPoint,
            details = details,
        )
    }
}
