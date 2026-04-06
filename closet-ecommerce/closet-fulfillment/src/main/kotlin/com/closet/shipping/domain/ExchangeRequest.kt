package com.closet.shipping.domain

import com.closet.common.vo.Money
import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Embedded
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
import java.time.ZonedDateTime

/**
 * 교환 요청 엔티티 (CP-28).
 *
 * 상태 머신: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> RESHIPPING -> COMPLETED
 *
 * PD-14: 동일 상품 = 동일 가격 옵션만 교환 허용.
 * 교환 시 새 옵션 재고 예약 + 기존 옵션 수거 완료 시 복구.
 */
@Entity
@Table(name = "exchange_request")
@EntityListeners(AuditingEntityListener::class)
class ExchangeRequest(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,
    /** 기존(반환할) 옵션 ID */
    @Column(name = "original_product_option_id", nullable = false)
    val originalProductOptionId: Long,
    /** 새로(교환받을) 옵션 ID */
    @Column(name = "new_product_option_id", nullable = false)
    val newProductOptionId: Long,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val reason: ReturnReason,
    @Column(name = "reason_detail", length = 500)
    val reasonDetail: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ExchangeStatus = ExchangeStatus.REQUESTED,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "shipping_fee", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var shippingFee: Money,
    @Column(name = "shipping_fee_payer", nullable = false, length = 20)
    var shippingFeePayer: String,
    @Column(name = "pickup_tracking_number", length = 30)
    var pickupTrackingNumber: String? = null,
    @Column(name = "new_tracking_number", length = 30)
    var newTrackingNumber: String? = null,
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

    fun schedulePickup(pickupTrackingNumber: String?) {
        status.validateTransitionTo(ExchangeStatus.PICKUP_SCHEDULED)
        status = ExchangeStatus.PICKUP_SCHEDULED
        this.pickupTrackingNumber = pickupTrackingNumber
    }

    fun completePickup() {
        status.validateTransitionTo(ExchangeStatus.PICKUP_COMPLETED)
        status = ExchangeStatus.PICKUP_COMPLETED
    }

    fun startReshipping(newTrackingNumber: String?) {
        status.validateTransitionTo(ExchangeStatus.RESHIPPING)
        status = ExchangeStatus.RESHIPPING
        this.newTrackingNumber = newTrackingNumber
    }

    fun complete() {
        status.validateTransitionTo(ExchangeStatus.COMPLETED)
        status = ExchangeStatus.COMPLETED
        completedAt = ZonedDateTime.now()
    }

    fun reject() {
        status.validateTransitionTo(ExchangeStatus.REJECTED)
        status = ExchangeStatus.REJECTED
    }

    companion object {
        fun create(
            orderId: Long,
            orderItemId: Long,
            memberId: Long,
            sellerId: Long,
            originalProductOptionId: Long,
            newProductOptionId: Long,
            quantity: Int,
            reason: ReturnReason,
            reasonDetail: String?,
            shippingFee: Money,
            shippingFeePayer: String,
        ): ExchangeRequest {
            return ExchangeRequest(
                orderId = orderId,
                orderItemId = orderItemId,
                memberId = memberId,
                sellerId = sellerId,
                originalProductOptionId = originalProductOptionId,
                newProductOptionId = newProductOptionId,
                quantity = quantity,
                reason = reason,
                reasonDetail = reasonDetail,
                shippingFee = shippingFee,
                shippingFeePayer = shippingFeePayer,
            )
        }
    }
}
