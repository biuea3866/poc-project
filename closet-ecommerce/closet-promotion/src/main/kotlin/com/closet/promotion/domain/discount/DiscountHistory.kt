package com.closet.promotion.domain.discount

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
import java.math.BigDecimal
import java.time.ZonedDateTime

@Entity
@Table(name = "discount_history")
@EntityListeners(AuditingEntityListener::class)
class DiscountHistory(
    @Column(name = "discount_policy_id", nullable = false)
    val discountPolicyId: Long,
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "original_amount", nullable = false, columnDefinition = "DECIMAL(12,2)")
    val originalAmount: BigDecimal,
    @Column(name = "discount_amount", nullable = false, columnDefinition = "DECIMAL(12,2)")
    val discountAmount: BigDecimal,
    @Column(name = "applied_at", nullable = false, columnDefinition = "DATETIME(6)")
    val appliedAt: ZonedDateTime = ZonedDateTime.now(),
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

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    var deletedAt: ZonedDateTime? = null
}
