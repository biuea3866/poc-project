package com.closet.promotion.application

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountHistory
import com.closet.promotion.domain.discount.DiscountPolicy
import com.closet.promotion.presentation.dto.ApplyDiscountRequest
import com.closet.promotion.presentation.dto.CreateDiscountPolicyRequest
import com.closet.promotion.presentation.dto.DiscountPolicyResponse
import com.closet.promotion.presentation.dto.DiscountResult
import com.closet.promotion.presentation.dto.StackedDiscountResult
import com.closet.promotion.repository.DiscountHistoryRepository
import com.closet.promotion.repository.DiscountPolicyRepository
import mu.KotlinLogging
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class DiscountPolicyService(
    private val discountPolicyRepository: DiscountPolicyRepository,
    private val discountHistoryRepository: DiscountHistoryRepository,
) {
    @Transactional
    fun createPolicy(request: CreateDiscountPolicyRequest): DiscountPolicyResponse {
        val policy =
            DiscountPolicy.create(
                name = request.name,
                discountType = request.discountType,
                discountValue = request.discountValue,
                maxDiscountAmount = request.maxDiscountAmount,
                conditionType = request.conditionType,
                conditionValue = request.conditionValue,
                priority = request.priority,
                isStackable = request.isStackable,
                startedAt = request.startedAt,
                endedAt = request.endedAt,
            )

        val saved = discountPolicyRepository.save(policy)
        logger.info { "할인 정책 생성: id=${saved.id}, name=${saved.name}" }
        return DiscountPolicyResponse.from(saved)
    }

    fun findApplicablePolicies(
        categoryId: Long?,
        brandId: Long?,
        orderAmount: BigDecimal?,
    ): List<DiscountPolicyResponse> {
        val policies = discountPolicyRepository.findActiveByConditions(categoryId, brandId, orderAmount)
        return policies
            .filter { it.conditionType != ConditionType.AMOUNT_RANGE || it.matchesCondition(orderAmount = orderAmount) }
            .map { DiscountPolicyResponse.from(it) }
    }

    @Transactional
    fun applyBestDiscount(request: ApplyDiscountRequest): DiscountResult {
        val policies =
            discountPolicyRepository.findActiveByConditions(
                request.categoryId,
                request.brandId,
                request.originalAmount,
            ).filter { it.conditionType != ConditionType.AMOUNT_RANGE || it.matchesCondition(orderAmount = request.originalAmount) }

        if (policies.isEmpty()) {
            return DiscountResult(
                policyId = 0L,
                policyName = "",
                discountAmount = BigDecimal.ZERO,
                finalAmount = request.originalAmount,
            )
        }

        // 우선순위가 가장 높은(priority 값이 낮은) 정책 적용
        val bestPolicy = policies.first()
        val discountAmount = bestPolicy.calculateDiscount(request.originalAmount)

        discountHistoryRepository.save(
            DiscountHistory(
                discountPolicyId = bestPolicy.id,
                orderId = request.orderId,
                memberId = request.memberId,
                originalAmount = request.originalAmount,
                discountAmount = discountAmount,
                appliedAt = ZonedDateTime.now(),
            ),
        )

        logger.info {
            "할인 적용: policyId=${bestPolicy.id}, orderId=${request.orderId}, discount=$discountAmount"
        }

        return DiscountResult(
            policyId = bestPolicy.id,
            policyName = bestPolicy.name,
            discountAmount = discountAmount,
            finalAmount = request.originalAmount.subtract(discountAmount),
        )
    }

    @Transactional
    fun applyStackedDiscounts(request: ApplyDiscountRequest): StackedDiscountResult {
        val policies =
            discountPolicyRepository.findActiveByConditions(
                request.categoryId,
                request.brandId,
                request.originalAmount,
            ).filter {
                it.isStackable && (
                    it.conditionType != ConditionType.AMOUNT_RANGE ||
                        it.matchesCondition(
                            orderAmount = request.originalAmount,
                        )
                )
            }

        if (policies.isEmpty()) {
            return StackedDiscountResult(
                appliedPolicies = emptyList(),
                totalDiscountAmount = BigDecimal.ZERO,
                finalAmount = request.originalAmount,
            )
        }

        var remainingAmount = request.originalAmount
        var totalDiscount = BigDecimal.ZERO
        val results = mutableListOf<DiscountResult>()

        for (policy in policies) {
            val discount = policy.calculateDiscount(remainingAmount)
            if (discount > BigDecimal.ZERO) {
                discountHistoryRepository.save(
                    DiscountHistory(
                        discountPolicyId = policy.id,
                        orderId = request.orderId,
                        memberId = request.memberId,
                        originalAmount = request.originalAmount,
                        discountAmount = discount,
                        appliedAt = ZonedDateTime.now(),
                    ),
                )

                totalDiscount = totalDiscount.add(discount)
                remainingAmount = remainingAmount.subtract(discount)

                results.add(
                    DiscountResult(
                        policyId = policy.id,
                        policyName = policy.name,
                        discountAmount = discount,
                        finalAmount = remainingAmount,
                    ),
                )
            }
        }

        logger.info {
            "중복 할인 적용: orderId=${request.orderId}, policies=${results.size}, totalDiscount=$totalDiscount"
        }

        return StackedDiscountResult(
            appliedPolicies = results,
            totalDiscountAmount = totalDiscount,
            finalAmount = remainingAmount,
        )
    }

    @Transactional
    fun deactivatePolicy(policyId: Long): DiscountPolicyResponse {
        val policy =
            discountPolicyRepository.findByIdOrNull(policyId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "할인 정책을 찾을 수 없습니다. id=$policyId")

        policy.deactivate()
        val saved = discountPolicyRepository.save(policy)

        logger.info { "할인 정책 비활성화: id=$policyId" }
        return DiscountPolicyResponse.from(saved)
    }

    fun getPolicy(policyId: Long): DiscountPolicyResponse {
        val policy =
            discountPolicyRepository.findByIdOrNull(policyId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "할인 정책을 찾을 수 없습니다. id=$policyId")
        return DiscountPolicyResponse.from(policy)
    }
}
