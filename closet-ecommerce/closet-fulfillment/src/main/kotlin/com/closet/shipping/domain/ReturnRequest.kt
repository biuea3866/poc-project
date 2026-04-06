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
 * 반품 요청 엔티티.
 *
 * 상태 머신: REQUESTED -> PICKUP_SCHEDULED -> PICKUP_COMPLETED -> INSPECTING -> APPROVED/REJECTED
 * APPROVED -> COMPLETED
 *
 * APPROVED 시: payment-service 부분 환불 호출 + inventory RESTORE 이벤트 발행
 */
@Entity
@Table(name = "return_request")
@EntityListeners(AuditingEntityListener::class)
class ReturnRequest(
    @Column(name = "order_id", nullable = false)
    val orderId: Long,
    @Column(name = "order_item_id", nullable = false)
    val orderItemId: Long,
    @Column(name = "member_id", nullable = false)
    val memberId: Long,
    @Column(name = "seller_id", nullable = false)
    val sellerId: Long,
    @Column(name = "product_option_id", nullable = false)
    val productOptionId: Long,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    val reason: ReturnReason,
    @Column(name = "reason_detail", length = 500)
    val reasonDetail: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "VARCHAR(30)")
    var status: ReturnStatus = ReturnStatus.REQUESTED,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "shipping_fee", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var shippingFee: Money,
    @Column(name = "shipping_fee_payer", nullable = false, length = 20)
    var shippingFeePayer: String,
    @Embedded
    @AttributeOverride(name = "amount", column = Column(name = "refund_amount", nullable = false, columnDefinition = "DECIMAL(15,2)"))
    var refundAmount: Money,
    @Column(name = "pickup_tracking_number", length = 30)
    var pickupTrackingNumber: String? = null,
    @Column(name = "inspected_at", columnDefinition = "DATETIME(6)")
    var inspectedAt: ZonedDateTime? = null,
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
        status.validateTransitionTo(ReturnStatus.PICKUP_SCHEDULED)
        status = ReturnStatus.PICKUP_SCHEDULED
        this.pickupTrackingNumber = pickupTrackingNumber
    }

    fun completePickup() {
        status.validateTransitionTo(ReturnStatus.PICKUP_COMPLETED)
        status = ReturnStatus.PICKUP_COMPLETED
    }

    fun startInspection() {
        status.validateTransitionTo(ReturnStatus.INSPECTING)
        status = ReturnStatus.INSPECTING
        inspectedAt = ZonedDateTime.now()
    }

    fun approve() {
        status.validateTransitionTo(ReturnStatus.APPROVED)
        status = ReturnStatus.APPROVED
    }

    fun reject(
        @Suppress("unused") rejectReason: String?,
    ) {
        status.validateTransitionTo(ReturnStatus.REJECTED)
        status = ReturnStatus.REJECTED
    }

    fun complete() {
        status.validateTransitionTo(ReturnStatus.COMPLETED)
        status = ReturnStatus.COMPLETED
        completedAt = ZonedDateTime.now()
    }

    companion object {
        fun create(
            orderId: Long,
            orderItemId: Long,
            memberId: Long,
            sellerId: Long,
            productOptionId: Long,
            quantity: Int,
            reason: ReturnReason,
            reasonDetail: String?,
            shippingFee: Money,
            shippingFeePayer: String,
            refundAmount: Money,
        ): ReturnRequest {
            return ReturnRequest(
                orderId = orderId,
                orderItemId = orderItemId,
                memberId = memberId,
                sellerId = sellerId,
                productOptionId = productOptionId,
                quantity = quantity,
                reason = reason,
                reasonDetail = reasonDetail,
                shippingFee = shippingFee,
                shippingFeePayer = shippingFeePayer,
                refundAmount = refundAmount,
            )
        }
    }
}
