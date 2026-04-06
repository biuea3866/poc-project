package com.closet.shipping.application.cs

import com.closet.shipping.domain.cs.claim.ClaimReasonCategory
import com.closet.shipping.domain.cs.claim.ClaimRequest
import com.closet.shipping.domain.cs.claim.ClaimStatus
import com.closet.shipping.domain.cs.claim.ClaimType
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.ZonedDateTime

data class CreateClaimCommand(
    @field:NotNull val orderId: Long,
    @field:NotNull val orderItemId: Long,
    @field:NotNull val memberId: Long,
    @field:NotNull val claimType: ClaimType,
    @field:NotNull val reasonCategory: ClaimReasonCategory,
    val reasonDetail: String? = null,
)

data class ApproveClaimCommand(
    @field:NotNull val refundAmount: BigDecimal,
)

data class ClaimResponse(
    val id: Long,
    val orderId: Long,
    val orderItemId: Long,
    val memberId: Long,
    val claimType: ClaimType,
    val reasonCategory: ClaimReasonCategory,
    val reasonDetail: String?,
    val status: ClaimStatus,
    val refundAmount: BigDecimal,
    val approvedAt: ZonedDateTime?,
    val completedAt: ZonedDateTime?,
) {
    companion object {
        fun from(claim: ClaimRequest): ClaimResponse =
            ClaimResponse(
                id = claim.id,
                orderId = claim.orderId,
                orderItemId = claim.orderItemId,
                memberId = claim.memberId,
                claimType = claim.claimType,
                reasonCategory = claim.reasonCategory,
                reasonDetail = claim.reasonDetail,
                status = claim.status,
                refundAmount = claim.refundAmount,
                approvedAt = claim.approvedAt,
                completedAt = claim.completedAt,
            )
    }
}
