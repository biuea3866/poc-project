package com.closet.shipping.domain.cs.claim

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
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "claim_request")
@EntityListeners(AuditingEntityListener::class)
class ClaimRequest(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val claimType: ClaimType,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason_category", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val reasonCategory: ClaimReasonCategory,
    @Column(name = "reason_detail", length = 500)
    val reasonDetail: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ClaimStatus = ClaimStatus.REQUESTED,
    @Column(name = "refund_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    var refundAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "approved_at", columnDefinition = "DATETIME(6)")
    var approvedAt: ZonedDateTime? = null,
    @Column(name = "completed_at", columnDefinition = "DATETIME(6)")
    var completedAt: ZonedDateTime? = null,
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

    fun approve(refundAmount: BigDecimal) {
        status.validateTransitionTo(ClaimStatus.APPROVED)
        status = ClaimStatus.APPROVED
        this.refundAmount = refundAmount
        this.approvedAt = ZonedDateTime.now()
    }

    fun reject() {
        status.validateTransitionTo(ClaimStatus.REJECTED)
        status = ClaimStatus.REJECTED
    }

    fun complete() {
        status.validateTransitionTo(ClaimStatus.COMPLETED)
        status = ClaimStatus.COMPLETED
        this.completedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            orderId: Long,
            orderItemId: Long,
            memberId: Long,
            claimType: ClaimType,
            reasonCategory: ClaimReasonCategory,
            reasonDetail: String? = null,
        ): ClaimRequest {
            return ClaimRequest(
                orderId = orderId,
                orderItemId = orderItemId,
                memberId = memberId,
                claimType = claimType,
                reasonCategory = reasonCategory,
                reasonDetail = reasonDetail,
            )
        }
    }
}
