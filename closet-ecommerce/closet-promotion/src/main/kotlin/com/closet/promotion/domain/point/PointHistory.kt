package com.closet.promotion.domain.point

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.ZonedDateTime

@Entity
@Table(name = "point_history")
@EntityListeners(AuditingEntityListener::class)
class PointHistory(
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "amount", nullable = false)
    val amount: Int,
    @Column(name = "balance_after", nullable = false)
    val balanceAfter: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val transactionType: PointTransactionType,
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 30, columnDefinition = "VARCHAR(30)")
    var referenceType: PointReferenceType? = null,
    @Column(name = "reference_id")
    var referenceId: Long? = null,
    @Column(name = "expired_at", columnDefinition = "DATETIME(6)")
    var expiredAt: ZonedDateTime? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: ZonedDateTime

    fun withReference(
        referenceType: PointReferenceType,
        referenceId: Long,
    ): PointHistory {
        this.referenceType = referenceType
        this.referenceId = referenceId
        return this
    }

    fun withExpiration(expiredAt: ZonedDateTime): PointHistory {
        this.expiredAt = expiredAt
        return this
    }
}
