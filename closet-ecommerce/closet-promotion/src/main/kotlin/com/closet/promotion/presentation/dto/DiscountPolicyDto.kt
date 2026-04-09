package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.discount.ConditionType
import com.closet.promotion.domain.discount.DiscountHistory
import com.closet.promotion.domain.discount.DiscountPolicy
import com.closet.promotion.domain.discount.DiscountType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateDiscountPolicyRequest(
    @field:NotBlank val name: String,
    @field:NotNull val discountType: DiscountType,
    @field:NotNull @field:Positive val discountValue: BigDecimal,
    val maxDiscountAmount: BigDecimal? = null,
    @field:NotNull val conditionType: ConditionType,
    val conditionValue: String = "",
    val priority: Int = 1,
    val isStackable: Boolean = false,
    @field:NotNull val startedAt: ZonedDateTime,
    @field:NotNull val endedAt: ZonedDateTime,
)

data class ApplyDiscountRequest(
    @field:NotNull val orderId: Long,
    @field:NotNull val memberId: Long,
    @field:NotNull @field:Positive val originalAmount: BigDecimal,
    val categoryId: Long? = null,
    val brandId: Long? = null,
)

data class DiscountPolicyResponse(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: BigDecimal,
    val maxDiscountAmount: BigDecimal?,
    val conditionType: ConditionType,
    val conditionValue: String,
    val priority: Int,
    val isStackable: Boolean,
    val isActive: Boolean,
    val startedAt: ZonedDateTime,
    val endedAt: ZonedDateTime,
) {
    companion object {
        fun from(policy: DiscountPolicy): DiscountPolicyResponse =
            DiscountPolicyResponse(
                id = policy.id,
                name = policy.name,
                discountType = policy.discountType,
                discountValue = policy.discountValue,
                maxDiscountAmount = policy.maxDiscountAmount,
                conditionType = policy.conditionType,
                conditionValue = policy.conditionValue,
                priority = policy.priority,
                isStackable = policy.isStackable,
                isActive = policy.isActive,
                startedAt = policy.startedAt,
                endedAt = policy.endedAt,
            )
    }
}

data class DiscountResult(
    val policyId: Long,
    val policyName: String,
    val discountAmount: BigDecimal,
    val finalAmount: BigDecimal,
)

data class StackedDiscountResult(
    val appliedPolicies: List<DiscountResult>,
    val totalDiscountAmount: BigDecimal,
    val finalAmount: BigDecimal,
)

data class DiscountHistoryResponse(
    val id: Long,
    val discountPolicyId: Long,
    val orderId: Long,
    val memberId: Long,
    val originalAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val appliedAt: ZonedDateTime,
) {
    companion object {
        fun from(history: DiscountHistory): DiscountHistoryResponse =
            DiscountHistoryResponse(
                id = history.id,
                discountPolicyId = history.discountPolicyId,
                orderId = history.orderId,
                memberId = history.memberId,
                originalAmount = history.originalAmount,
                discountAmount = history.discountAmount,
                appliedAt = history.appliedAt,
            )
    }
}
