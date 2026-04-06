package com.closet.promotion.domain.point

import com.closet.common.exception.BusinessException
import com.closet.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "point_balance")
@EntityListeners(AuditingEntityListener::class)
class PointBalance(
    @Column(name = "member_id", nullable = false, unique = true)
    val memberId: Long,
    @Column(name = "total_points", nullable = false)
    var totalPoints: Int = 0,
    @Column(name = "available_points", nullable = false)
    var availablePoints: Int = 0,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: ZonedDateTime

    fun earn(amount: Int): PointHistory {
        require(amount > 0) { "적립 금액은 0보다 커야 합니다" }
        totalPoints += amount
        availablePoints += amount

        return PointHistory(
            memberId = memberId,
            amount = amount,
            balanceAfter = availablePoints,
            transactionType = PointTransactionType.EARN,
        )
    }

    fun use(amount: Int): PointHistory {
        require(amount > 0) { "사용 금액은 0보다 커야 합니다" }
        if (availablePoints < amount) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "적립금이 부족합니다. 현재 잔액: $availablePoints, 요청 금액: $amount",
            )
        }
        availablePoints -= amount

        return PointHistory(
            memberId = memberId,
            amount = -amount,
            balanceAfter = availablePoints,
            transactionType = PointTransactionType.USE,
        )
    }

    fun cancelEarn(amount: Int): PointHistory {
        require(amount > 0) { "취소 금액은 0보다 커야 합니다" }
        if (availablePoints < amount) {
            throw BusinessException(
                ErrorCode.INVALID_INPUT,
                "취소할 적립금이 부족합니다. 현재 잔액: $availablePoints, 취소 요청 금액: $amount",
            )
        }
        totalPoints -= amount
        availablePoints -= amount

        return PointHistory(
            memberId = memberId,
            amount = -amount,
            balanceAfter = availablePoints,
            transactionType = PointTransactionType.CANCEL_EARN,
        )
    }

    fun cancelUse(amount: Int): PointHistory {
        require(amount > 0) { "취소 금액은 0보다 커야 합니다" }
        availablePoints += amount

        return PointHistory(
            memberId = memberId,
            amount = amount,
            balanceAfter = availablePoints,
            transactionType = PointTransactionType.CANCEL_USE,
        )
    }

    companion object {
        fun create(memberId: Long): PointBalance {
            return PointBalance(memberId = memberId)
        }
    }
}
