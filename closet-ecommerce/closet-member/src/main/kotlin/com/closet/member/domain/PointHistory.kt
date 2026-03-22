package com.closet.member.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 포인트 변동 이력 엔티티
 */
@Entity
@Table(name = "point_history")
class PointHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val type: PointType,

    @Column(nullable = false)
    val amount: Int,

    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Int,

    @Column(nullable = false, length = 200)
    val reason: String,

    @Column(name = "reference_id", length = 100)
    val referenceId: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    val createdAt: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun earn(memberId: Long, amount: Int, balanceAfter: Int, reason: String, referenceId: String? = null): PointHistory {
            return PointHistory(
                memberId = memberId,
                type = PointType.EARN,
                amount = amount,
                balanceAfter = balanceAfter,
                reason = reason,
                referenceId = referenceId,
            )
        }

        fun use(memberId: Long, amount: Int, balanceAfter: Int, reason: String, referenceId: String? = null): PointHistory {
            return PointHistory(
                memberId = memberId,
                type = PointType.USE,
                amount = amount,
                balanceAfter = balanceAfter,
                reason = reason,
                referenceId = referenceId,
            )
        }
    }
}
