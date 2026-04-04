package com.closet.shipping.domain

import com.closet.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 배송비 정책 (PD-15).
 *
 * type + reason 조합으로 배송비 매트릭스를 관리한다.
 * - RETURN + DEFECTIVE -> SELLER / 0원
 * - RETURN + WRONG_ITEM -> SELLER / 0원
 * - RETURN + SIZE_MISMATCH -> BUYER / 3,000원
 * - RETURN + CHANGE_OF_MIND -> BUYER / 3,000원
 * - EXCHANGE + DEFECTIVE -> SELLER / 0원
 * - EXCHANGE + WRONG_ITEM -> SELLER / 0원
 * - EXCHANGE + SIZE_MISMATCH -> BUYER / 6,000원
 * - EXCHANGE + CHANGE_OF_MIND -> BUYER / 6,000원
 */
@Entity
@Table(name = "shipping_fee_policy")
@EntityListeners(AuditingEntityListener::class)
class ShippingFeePolicy(
    @Column(name = "type", nullable = false, length = 20)
    val type: String,

    @Column(name = "reason", nullable = false, length = 30)
    val reason: String,

    @Column(name = "payer", nullable = false, length = 20)
    val payer: String,

    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "fee", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    val fee: Money,

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1)")
    var isActive: Boolean = true,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    lateinit var createdAt: LocalDateTime

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    lateinit var updatedAt: LocalDateTime
}
