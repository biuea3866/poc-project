package com.closet.promotion.presentation.dto

import com.closet.promotion.domain.point.PointBalance
import com.closet.promotion.domain.point.PointHistory
import com.closet.promotion.domain.point.PointReferenceType
import com.closet.promotion.domain.point.PointTransactionType
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.ZonedDateTime

data class PointBalanceResponse(
    val id: Long,
    val memberId: Long,
    val totalPoints: Int,
    val availablePoints: Int,
) {
    companion object {
        fun from(balance: PointBalance): PointBalanceResponse =
            PointBalanceResponse(
                id = balance.id,
                memberId = balance.memberId,
                totalPoints = balance.totalPoints,
                availablePoints = balance.availablePoints,
            )
    }
}

data class PointHistoryResponse(
    val id: Long,
    val memberId: Long,
    val amount: Int,
    val balanceAfter: Int,
    val transactionType: PointTransactionType,
    val referenceType: PointReferenceType?,
    val referenceId: Long?,
    val expiredAt: ZonedDateTime?,
    val createdAt: ZonedDateTime?,
) {
    companion object {
        fun from(history: PointHistory): PointHistoryResponse =
            PointHistoryResponse(
                id = history.id,
                memberId = history.memberId,
                amount = history.amount,
                balanceAfter = history.balanceAfter,
                transactionType = history.transactionType,
                referenceType = history.referenceType,
                referenceId = history.referenceId,
                expiredAt = history.expiredAt,
                createdAt = history.createdAt,
            )
    }
}

data class EarnPointRequest(
    @field:NotNull val memberId: Long,
    @field:Positive val amount: Int,
    val referenceType: PointReferenceType? = null,
    val referenceId: Long? = null,
)

data class UsePointRequest(
    @field:NotNull val memberId: Long,
    @field:Positive val amount: Int,
    val referenceType: PointReferenceType? = null,
    val referenceId: Long? = null,
)

data class CancelPointRequest(
    @field:NotNull val memberId: Long,
    @field:Positive val amount: Int,
    @field:NotNull val transactionType: PointTransactionType,
    val referenceType: PointReferenceType? = null,
    val referenceId: Long? = null,
)
